package com.whispertflite.engine

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File

class MoonshineEngine(private val context: Context) : WhisperEngine {

    private var recognizer: OfflineRecognizer? = null
    private var initialized = false

    companion object {
        private const val TAG = "MoonshineEngine"
        @Volatile private var instance: MoonshineEngine? = null

        @JvmStatic
        fun getInstance(context: Context): MoonshineEngine =
            instance ?: synchronized(this) {
                instance ?: MoonshineEngine(context.applicationContext).also { instance = it }
            }
    }

    override fun isInitialized(): Boolean = initialized && recognizer != null

    override fun initialize(modelPath: String?, vocabPath: String?, multilingual: Boolean): Boolean {
        if (isInitialized()) return true

        val modelDir = context.getExternalFilesDir(null) ?: context.filesDir
        val encoderFile = File(modelDir, "encoder_model.int8.onnx")
        val decoderFile = File(modelDir, "decoder_model_merged.int8.onnx")
        val tokensFile = File(modelDir, "tokens.txt")

        if (!encoderFile.exists() || !decoderFile.exists() || !tokensFile.exists()) {
            Log.e(TAG, "Moonshine model files missing from ${modelDir.absolutePath}")
            return false
        }

        return try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    moonshine = OfflineMoonshineModelConfig(
                        preprocessor = "",
                        encoder = encoderFile.absolutePath,
                        uncachedDecoder = decoderFile.absolutePath,
                        cachedDecoder = decoderFile.absolutePath
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OfflineRecognizer", e)
            false
        }
    }

    override fun transcribeBuffer(samples: FloatArray): String {
        if (!isInitialized()) {
            if (!initialize(null, null, false)) return ""
        }

        return try {
            val rec = recognizer ?: return ""
            val stream = rec.createStream()
            stream.acceptWaveform(samples, 16000)
            rec.decode(stream)
            // Notice .text instead of ?.text to clear the Kotlin compiler warning
            val result = rec.getResult(stream).text
            stream.release()
            result.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Inference error during transcription", e)
            ""
        }
    }

    override fun transcribeFile(wavePath: String?): String {
        // Fallback for file transcription if called by WhisperIME
        return ""
    }
}
