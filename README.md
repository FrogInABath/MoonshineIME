MoonshineIME (CAT S22 Flip Edition)

A modified fork of woheller69/whisperIME tailored specifically for the CAT S22 Flip running Android Go.

This fork replaces the original Whisper TensorFlow Lite inference backend with Sherpa-ONNX Moonshine Tiny (INT8) and overhauls the interface into a compact, fantasy-themed stone rune toolbar designed for the phone's 2.8" display.

Modifications

1. Inference Engine: Whisper TFLite → Moonshine Tiny (INT8)

Backend: Replaced the Whisper TFLite runtime with sherpa-onnx running the Moonshine Tiny (INT8 quantized) model architecture.

Architecture: Targeted and compiled with native libraries for 32-bit ARM (armeabi-v7a) to run locally on the Qualcomm Snapdragon 215 chipset.

Performance: Near-instant offline transcription with low memory overhead, zero network dependencies, and no thermal throttling.

2. Rune Punctuation Bar & RPG UI

Screen-Optimized Toolbar: Replaced the oversized stock IME view with a slim 52dp single-row bar, keeping over 85% of the 2.8" screen visible during dictation.

Ancient Stone Runes: Common punctuation targets (,, ., ?, !, ', ⌨) rendered as weathered, moss-covered stone runes with chiseled, glowing glyphs set against an aged canvas deck.

Spiraling Branch Separator: A gnarled oak branch wrapped in leafy briar vines frames the top of the bar, cleanly separating it from the active text field.

Sacred Geometry Coin: Touch-to-record action button styled as an engraved silver coin with sacred geometry linework and a horizontal-axis 3D spin animation.

Smart Spacing: Punctuation keys automatically detect and strip trailing whitespace from model output before committing punctuation, followed by standard spacing.

Attribution & Licenses

Upstream Android Application: woheller69/whisperIME, based on vilassn/whisper_android by Vilas Ninawe (MIT License)

ASR Architecture: Useful Sensors - Moonshine (MIT License)

On-Device Inference: k2-fsa/sherpa-onnx (Apache-2.0)

Runtime: Microsoft ONNX Runtime (MIT License)

Voice Activity Detection: gkonovalov/android-vad (MIT License)

Chinese Conversion: houbb/opencc4j (Apache-2.0)

Setup & Deployment

1. Model Files

The app expects the Moonshine ONNX model files in the application's external files directory:
/sdcard/Android/data/org.woheller69.whisper/files/

Required files: Download the pre-converted INT8 model files from Hugging Face:

Moonshine Tiny (INT8): csukuangfj/sherpa-onnx-moonshine-tiny-en-int8

preprocess.onnx

encode.int8.onnx

uncached_decode.int8.onnx

cached_decode.int8.onnx

tokens.txt

Push them to the device via ADB:

adb shell mkdir -p /sdcard/Android/data/org.woheller69.whisper/files/
adb push preprocess.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push encode.int8.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push uncached_decode.int8.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push cached_decode.int8.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push tokens.txt /sdcard/Android/data/org.woheller69.whisper/files/
