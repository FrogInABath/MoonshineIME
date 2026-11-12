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

    @Throws(IOException::class)
    override fun initialize(modelPath: String?, vocabPath: String?, multilingual: Boolean) {
        if (isInitialized()) return

        val modelDir = context.getExternalFilesDir(null) ?: context.filesDir
        val preprocessorFile = File(modelDir, "preprocess.onnx")
        val encoderFile = File(modelDir, "encode.onnx")
        val uncachedDecoderFile = File(modelDir, "uncached_decode.onnx")
        val cachedDecoderFile = File(modelDir, "cached_decode.onnx")
        val tokensFile = File(modelDir, "tokens.txt")

        // Check required Sherpa-ONNX Moonshine files
        if (!encoderFile.exists() || !uncachedDecoderFile.exists() || !tokensFile.exists()) {
            Log.e(TAG, "Moonshine model files missing from ${modelDir.absolutePath}")
            initialized = false
            return
        }

        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    moonshine = OfflineMoonshineModelConfig(
                        preprocessor = if (preprocessorFile.exists()) preprocessorFile.absolutePath else "",
                        encoder = encoderFile.absolutePath,
                        uncachedDecoder = uncachedDecoderFile.absolutePath,
                        cachedDecoder = if (cachedDecoderFile.exists()) cachedDecoderFile.absolutePath else uncachedDecoderFile.absolutePath
                    ),
                    tokens = tokensFile.absolutePath,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            )

            recognizer = OfflineRecognizer(config = config)
            initialized = true
            Log.i(TAG, "Moonshine Engine initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OfflineRecognizer", e)
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
            Log.e(TAG, "Inference error during transcription", e)
            WhisperResult("", "", mAction)
        }
    }
}
