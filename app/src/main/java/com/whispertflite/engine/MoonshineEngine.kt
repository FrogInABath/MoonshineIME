package com.whispertflite.engine

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import com.whispertflite.asr.RecordBuffer
import com.whispertflite.asr.Whisper
import com.whispertflite.asr.WhisperResult
import java.io.File
import java.io.IOException

class MoonshineEngine(private val context: Context) : WhisperEngine {

    private var recognizer: OfflineRecognizer? = null
    private var initialized = false

    companion object {
        private const val TAG = "MoonshineEngine"
    }

    override fun isInitialized(): Boolean = initialized && recognizer != null

    private fun resolveFile(dir: File, vararg candidates: String): File? {
        for (name in candidates) {
            val file = File(dir, name)
            if (file.exists()) return file
        }
        return null
    }

    @Throws(IOException::class)
    override fun initialize(modelPath: String?, vocabPath: String?, multilingual: Boolean) {
        if (isInitialized()) return

        val modelDir = context.getExternalFilesDir(null) ?: context.filesDir

        val preprocessor = resolveFile(modelDir, "preprocess.onnx")
        val encoder = resolveFile(modelDir, "encode.int8.onnx", "encode.onnx", "encoder_model.int8.onnx")
        val uncachedDecoder = resolveFile(modelDir, "uncached_decode.int8.onnx", "uncached_decode.onnx", "decoder_model_merged.int8.onnx")
        val cachedDecoder = resolveFile(modelDir, "cached_decode.int8.onnx", "cached_decode.onnx") ?: uncachedDecoder
        val tokens = resolveFile(modelDir, "tokens.txt")

        if (preprocessor == null || encoder == null || uncachedDecoder == null || tokens == null) {
            Log.e(TAG, "Moonshine ONNX assets missing from ${modelDir.absolutePath}. Found: preprocessor=$preprocessor, encoder=$encoder, decoder=$uncachedDecoder, tokens=$tokens")
            initialized = false
            return
        }

        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    moonshine = OfflineMoonshineModelConfig(
                        preprocessor = preprocessor.absolutePath,
                        encoder = encoder.absolutePath,
                        uncachedDecoder = uncachedDecoder.absolutePath,
                        cachedDecoder = cachedDecoder.absolutePath
                    ),
                    tokens = tokens.absolutePath,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            )

            recognizer = OfflineRecognizer(config = config)
            initialized = true
            Log.i(TAG, "Moonshine Engine initialized successfully from ${modelDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Sherpa-ONNX Moonshine recognizer", e)
            initialized = false
        }
    }

    override fun deinitialize() {
        recognizer?.release()
        recognizer = null
        initialized = false
    }

    override fun processRecordBuffer(mAction: Whisper.Action, mLangToken: Int): WhisperResult {
        if (!isInitialized()) {
            try {
                initialize(null, null, false)
            } catch (e: IOException) {
                return WhisperResult("", "", mAction)
            }
        }

        val rec = recognizer ?: return WhisperResult("", "", mAction)
        val samples = RecordBuffer.getSamples() ?: return WhisperResult("", "", mAction)

        return try {
            val stream = rec.createStream()
            stream.acceptWaveform(samples, 16000)
            rec.decode(stream)
            val textResult = rec.getResult(stream).text.trim()
            stream.release()

            WhisperResult(textResult, "en", mAction)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error during Moonshine transcription", e)
            WhisperResult("", "", mAction)
        }
    }
}
