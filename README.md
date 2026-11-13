# MoonshineIME (CAT S22 Flip Edition)
# MoonshineIME (CAT S22 Flip Edition)

```text
               .---.             _..._              .---.             _..._
       ~🌿~   /     \   ~🌿~   .'     '.   ~🌿~   /     \   ~🌿~   .'     '.   ~🌿~
     __,,____(  / \  )____,,__(   / \   )____,,__(  / \  )____,,__(   / \   )____,,__
    (=========\/===\/==========\ /===\ /==========\/===\/==========\ /===\ /========)
     `---..___/     \___..---'`-.'-.-.'-`---..___/     \___..---'`-.'-.-.'-`---..---'
             `'---'`                            `'---'`
  ══════════════════════════════════════════════════════════════════════════════════════
    ░▒▓█►  A N C I E N T   S P E E C H - T O - R U N E   L E X I C O N  ◄█▓▒░
  ══════════════════════════════════════════════════════════════════════════════════════

      .---..---.    .------.    .------.   .---.  .---.   .-------.  .---.  .---.
     /   / \/   \  /   __   \  /   __   \ /   /  /   / \ /   ____/  /   /  /   /
    |   / \  /   ||   /  \   ||   /  \   ||   \ /   /  /|   (___   |   /  /   / 
    |  /   \/|   ||  | () |  ||  | () |  || |\ ' /|  | | \___   \  |   '--'   | 
    | / /|   |   ||  |    |  ||  |    |  || | \_/ |  | | ____)   | |   .---.  | 
    |/_/ |_|/|___| \  \__/  /  \  \__/  / |_|     |_/| ||       /  |   |   |  | 
    '--'     '---'  '------'    '------'  '-'     '--'-''------'   '---'   '---'
                 ___ _ _ _     _ _ _ _    ___ _ _ _ _   
                |_ _| \ | |   | | \ | |  | __| | \ | |  
                 | ||  \| |   | |  \| |  | _|| |  \| |  
                |___|_|\__|   |_|_|\__|  |___|_|\__| |  
               '---''-----'   '---''---' '---''------'  

                     .---.     .---..---.    .-------.
                    /   /     /   / \/   \  /   ____/
                   |   |     |   / \  /   ||   |___  
                   |   |     |  /   \/|   ||    ___| 
                   |   |     | / /|   |   ||   |____ 
                   |   |     |/_/ |_|/|___||        /
                   '---'     '--'     '---''-------' 

  ══════════════════════════════════════════════════════════════════════════════════════
     [ Snapdragon 215 • 32-Bit ARMv7-A • Offline Sherpa-ONNX Moonshine Tiny INT8 ]
  ══════════════════════════════════════════════════════════════════════════════════════

     .-'-.         .-'-.         .-'-.         .-'-.         .-'-.         .-'-.
    ( HEX )       ( PEB )       ( DSC )       ( DIA )       ( OBS )       ( SHD )
     /===\         .---.         .---.         / \          .-----.       .-----.
   /| === |\      / .-. \       / .-. \       / / \ \      /   |   \     /   |   \
  | |[⌨]| |     | ( , ) |     | ( . ) |     < < ? > >    | | [!] | |    \ \[']/ /
   \| === |/      \ '-' /       \ '-' /       \ \ / /      \___|___/      \__|__/
     \===/         '---'         '---'         \ /          '-----'        '---'
     RUBY          AMBER        SUNSTONE       JADE          AZURE        AMETHYST
    (Flame)       (Earth)        (Sol)       (Forest)       (Ocean)       (Aether)

  ──────────────────────────────────────────────────────────────────────────────────────
                                                  SACRED GEOMETRY COIN (PTT ACTION)
                                                        . - ~ ~ ~ - .
                                                    .-'   _ / | \ _   '-.
                                                   /   .-' /  |  \ '-.   \
                                                  /  /   / /| | |\ \  \   \
                                                 |  |   | |( ◎ )| | |  |   |
                                                 |  |   | \ \/ \/ / |  |   |
                                                  \  \   \ \| | |/ /  /   /
                                                   \   '-. \_ | _/ .-'   /
                                                    '-.   ~ ~ | ~ ~   .-'
                                                        ' - ~ ~ ~ - '
  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
  ░▒▓ TAN CANVAS VELLUM DECK ▓▒░       - [ C R O S S - S T I T C H   T R I M ] -
A modified fork of [woheller69/whisperIME](https://github.com/woheller69/whisperIME) tailored specifically for the CAT S22 Flip running Android Go.

This fork replaces the original Whisper TensorFlow Lite inference backend with Sherpa-ONNX Moonshine Tiny (INT8) and overhauls the interface into a compact, fantasy-themed stone rune toolbar designed for the phone's 2.8" display.

---

## Modifications

### 1. Inference Engine: Whisper TFLite → Moonshine Tiny (INT8)
* **Backend:** Replaced the Whisper TFLite runtime with `sherpa-onnx` running the **Moonshine Tiny (INT8 quantized)** model architecture.
* **Architecture:** Targeted and compiled with native libraries for 32-bit ARM (`armeabi-v7a`) to run locally on the Qualcomm Snapdragon 215 chipset.
* **Performance:** Near-instant offline transcription with low memory overhead, zero network dependencies, and no thermal throttling.

### 2. Rune Punctuation Bar & RPG UI
* **Screen-Optimized Toolbar:** Replaced the oversized stock IME view with a slim 52dp single-row bar, keeping over 85% of the 2.8" screen visible during dictation.
* **Ancient Stone Runes:** Common punctuation targets (`,`, `.`, `?`, `!`, `'`, `⌨`) rendered as weathered, moss-covered stone runes with chiseled, glowing glyphs set against an aged canvas deck.
* **Spiraling Branch Separator:** A gnarled oak branch wrapped in leafy briar vines frames the top of the bar, cleanly separating it from the active text field.
* **Sacred Geometry Coin:** Touch-to-record action button styled as an engraved silver coin with sacred geometry linework and a horizontal-axis 3D spin animation.
* **Smart Spacing:** Punctuation keys automatically detect and strip trailing whitespace from model output before committing punctuation, followed by standard spacing.

---

## Attribution & Licenses

* **Upstream Android Application:** [woheller69/whisperIME](https://github.com/woheller69/whisperIME), based on [vilassn/whisper_android](https://github.com/vilassn/whisper_android) by Vilas Ninawe (MIT License)
* **ASR Architecture:** [Useful Sensors - Moonshine](https://github.com/usefulsensors/moonshine) (MIT License)
* **On-Device Inference:** [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) (Apache-2.0)
* **Runtime:** [Microsoft ONNX Runtime](https://github.com/microsoft/onnxruntime) (MIT License)
* **Voice Activity Detection:** [gkonovalov/android-vad](https://github.com/gkonovalov/android-vad) (MIT License)
* **Chinese Conversion:** [houbb/opencc4j](https://github.com/houbb/opencc4j) (Apache-2.0)

---

## Setup & Deployment

### 1. Model Files
The app expects the Moonshine ONNX model files in the application's external files directory:  
`/sdcard/Android/data/org.woheller69.whisper/files/`

Download the pre-converted INT8 model files from [csukuangfj/sherpa-onnx-moonshine-tiny-en-int8](https://huggingface.co/csukuangfj/sherpa-onnx-moonshine-tiny-en-int8):
* `preprocess.onnx`
* `encode.int8.onnx`
* `uncached_decode.int8.onnx`
* `cached_decode.int8.onnx`
* `tokens.txt`

### 2. Push via ADB
```cmd
adb shell mkdir -p /sdcard/Android/data/org.woheller69.whisper/files/
adb push preprocess.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push encode.int8.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push uncached_decode.int8.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push cached_decode.int8.onnx /sdcard/Android/data/org.woheller69.whisper/files/
adb push tokens.txt /sdcard/Android/data/org.woheller69.whisper/files/
