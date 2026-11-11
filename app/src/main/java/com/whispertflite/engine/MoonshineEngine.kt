
package com.whispertflite.engine

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File

class MoonshineEngine private constructor(private val context: Context) {

    private var recognizer: OfflineRecognizer? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "MoonshineEngine"
        @Volatile private var instance: MoonshineEngine? = null

        fun getInstance(context: Context): MoonshineEngine =
            instance ?: synchronized(this) {
                instance ?: MoonshineEngine(context.applicationContext).also { instance = it }
            }
    }

    @Synchronized
    fun initialize(): Boolean {
        if (isInitialized && recognizer != null) return true

        val modelDir = context.getExternalFilesDir(null) ?: context.filesDir
        val encoderFile = File(modelDir, "encoder_model.int8.onnx")
        val decoderFile = File(modelDir, "decoder_model_merged.int8.onnx")
        val tokensFile = File(modelDir, "tokens.txt")

        if (!encoderFile.exists() || !decoderFile.exists() || !tokensFile.exists()) {
            Log.e(TAG, "Moonshine v2 model files missing from ${modelDir.absolutePath}")
            return false
        }

        try {
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
            isInitialized = true
            Log.i(TAG, "Moonshine v2 Engine initialized successfully.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OfflineRecognizer", e)
            return false
        }
    }

    @Synchronized
    fun transcribe(samples: FloatArray): String {
        if (!isInitialized || recognizer == null) {
            if (!initialize()) return ""
        }

        return try {
            val rec = recognizer ?: return ""
            val stream = rec.createStream()
            stream.acceptWaveform(samples, 16000)
            rec.decode(stream)
            val result = rec.getResult(stream)?.text ?: ""
            stream.release()
            result.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Inference error during transcription", e)
            ""
        }
    }
}
