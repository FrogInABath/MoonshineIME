package com.whispertflite;

import static com.whispertflite.MainActivity.ENGLISH_ONLY_VOCAB_FILE;
import static com.whispertflite.MainActivity.MULTI_LINGUAL_TOP_WORLD_SLOW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.whispertflite.asr.Recorder;
import com.whispertflite.asr.Whisper;
import com.whispertflite.asr.WhisperResult;
import com.whispertflite.utils.HapticFeedback;

import java.io.File;

public class WhisperInputMethodService extends InputMethodService {
    private static final String TAG = "WhisperInputMethodService";
    private ImageButton btnRecord;
    private ImageButton btnKeyboard;
    private ImageButton btnTranslate;
    private ImageButton btnModeAuto;
    private ImageButton btnEnter;
    private ImageButton btnDel;
    private TextView tvStatus;
    private Recorder mRecorder = null;
    private Whisper mWhisper = null;
    private File sdcardDataFolder = null;
    private ProgressBar processingBar = null;
    private SharedPreferences sp = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context mContext;
    private CountDownTimer countDownTimer;
    private static boolean translate = false;
    private boolean modeAuto = false;
    private RelativeLayout layoutButtons;

    // Push-to-Talk tracking
    private long recordStartTime = 0;
    private boolean isDiscarded = false;

    @Override
    public void onCreate() {
        mContext = this;
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        deinitModel();
        if (mRecorder != null && mRecorder.isInProgress()) {
            mRecorder.stop();
        }
        super.onDestroy();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        if (attribute.inputType == EditorInfo.TYPE_NULL) {
            deinitModel();
            if (mRecorder != null && mRecorder.isInProgress()) {
                mRecorder.stop();
            }
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        if (mWhisper == null) {
            initModel();
        }
    }

    // --- Validation: Ensure an actual editable input box is active ---

    private boolean isInputTargetValid() {
        if (!isInputViewShown()) return false;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei == null) return false;
        // Verify target is an editable field (not TYPE_NULL)
        return (ei.inputType & EditorInfo.TYPE_MASK_CLASS) != EditorInfo.TYPE_NULL;
    }

    // --- Physical Hardware Key Interception (CAT S22 Orange Action/PTT Button) ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // CAT S22 Orange PTT Key = Keycode 269 (KeyEvent.KEYCODE_PTT)
        if (keyCode == KeyEvent.KEYCODE_PTT || keyCode == 269) {
            if (isInputTargetValid()) {
                // event.getRepeatCount() == 0 ensures recording triggers on initial press, not held repeat pulses
                if (event.getRepeatCount() == 0) {
                    if (checkRecordPermission()) {
                        if (mWhisper != null && !mWhisper.isInProgress() && mRecorder != null && !mRecorder.isInProgress()) {
                            isDiscarded = false;
                            recordStartTime = System.currentTimeMillis();
                            HapticFeedback.vibrate(this);
                            startRecordingAction();
                        }
                    }
                }
                return true; // Consume event
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PTT || keyCode == 269) {
            if (mRecorder != null && mRecorder.isInProgress()) {
                long holdDuration = System.currentTimeMillis() - recordStartTime;
                // If held for less than 300ms, treat as accidental tap and discard
                if (holdDuration < 300) {
                    isDiscarded = true;
                    stopRecordingAction();
                } else {
                    isDiscarded = false;
                    stopRecordingAction();
                }
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    // --- Recording & UI Helpers ---

    private void startRecordingAction() {
        if (mWhisper != null && mWhisper.isInProgress()) {
            handler.post(() -> {
                if (tvStatus != null) {
                    tvStatus.setText(getString(R.string.please_wait));
                    tvStatus.setVisibility(View.VISIBLE);
                }
            });
            return;
        }

        handler.post(() -> {
            if (btnRecord != null) {
                btnRecord.setBackgroundResource(R.drawable.rounded_button_background_pressed);
            }
            if (processingBar != null) {
                processingBar.setProgress(100);
            }
            if (tvStatus != null) {
                tvStatus.setText("");
                tvStatus.setVisibility(View.GONE);
            }
        });

        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long l) {
                handler.post(() -> {
                    if (processingBar != null) processingBar.setProgress((int) (l / 300));
                });
            }

            @Override
            public void onFinish() {}
        };
        countDownTimer.start();

        if (modeAuto && mRecorder != null) {
            mRecorder.initVad();
        }
        if (mRecorder != null) {
            mRecorder.start();
        }
    }

    private void stopRecordingAction() {
        handler.post(() -> {
            if (btnRecord != null) {
                btnRecord.setBackgroundResource(R.drawable.rounded_button_background);
            }
        });
        if (mRecorder != null && mRecorder.isInProgress()) {
            mRecorder.stop();
        }
    }

    // --- View Construction ---

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateInputView() {
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        View view = getLayoutInflater().inflate(R.layout.voice_service, null);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });

        btnRecord = view.findViewById(R.id.btnRecord);
        btnKeyboard = view.findViewById(R.id.btnKeyboard);
        btnTranslate = view.findViewById(R.id.btnTranslate);
        btnModeAuto = view.findViewById(R.id.btnModeAuto);
        btnEnter = view.findViewById(R.id.btnEnter);
        btnDel = view.findViewById(R.id.btnDel);
        processingBar = view.findViewById(R.id.processing_bar);
        tvStatus = view.findViewById(R.id.tv_status);
        sdcardDataFolder = this.getExternalFilesDir(null);

        btnTranslate.setImageResource(translate ? R.drawable.ic_english_on_36dp : R.drawable.ic_english_off_36dp);
        modeAuto = sp.getBoolean("imeModeAuto", false);
        btnModeAuto.setImageResource(modeAuto ? R.drawable.ic_auto_on_36dp : R.drawable.ic_auto_off_36dp);
        layoutButtons = view.findViewById(R.id.layout_buttons);
        checkRecordPermission();

        mRecorder = new Recorder(this);
        mRecorder.setListener(new Recorder.RecorderListener() {
            @Override
            public void onUpdateReceived(String message) {
                if (message.equals(Recorder.MSG_RECORDING)) {
                    handler.post(() -> {
                        if (btnRecord != null) {
                            btnRecord.setBackgroundResource(R.drawable.rounded_button_background_pressed);
                        }
                    });
                } else if (message.equals(Recorder.MSG_RECORDING_DONE)) {
                    HapticFeedback.vibrate(mContext);
                    handler.post(() -> {
                        if (btnRecord != null) {
                            btnRecord.setBackgroundResource(R.drawable.rounded_button_background);
                        }
                    });
                    if (!isDiscarded) {
                        startTranscription();
                    } else {
                        handler.post(() -> {
                            if (processingBar != null) processingBar.setProgress(0);
                        });
                    }
                } else if (message.equals(Recorder.MSG_RECORDING_ERROR)) {
                    HapticFeedback.vibrate(mContext);
                    if (countDownTimer != null) countDownTimer.cancel();
                    handler.post(() -> {
                        if (btnRecord != null) {
                            btnRecord.setBackgroundResource(R.drawable.rounded_button_background);
                        }
                        if (tvStatus != null) {
                            tvStatus.setText(getString(R.string.error_no_input));
                            tvStatus.setVisibility(View.VISIBLE);
                        }
                        if (processingBar != null) {
                            processingBar.setProgress(0);
                        }
                    });
                }
            }
        });

        // Touch Listener (Fallback for onscreen button)
        btnRecord.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (checkRecordPermission()) {
                    isDiscarded = false;
                    recordStartTime = System.currentTimeMillis();
                    HapticFeedback.vibrate(this);
                    startRecordingAction();
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                stopRecordingAction();
            }
            return true;
        });

        btnDel.setOnTouchListener(new View.OnTouchListener() {
            private Runnable initialDeleteRunnable;
            private Runnable repeatDeleteRunnable;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) return false;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                    initialDeleteRunnable = () -> {
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        repeatDeleteRunnable = new Runnable() {
                            @Override
                            public void run() {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                                handler.postDelayed(this, 100);
                            }
                        };
                        handler.postDelayed(repeatDeleteRunnable, 100);
                    };
                    handler.postDelayed(initialDeleteRunnable, 500);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (initialDeleteRunnable != null) handler.removeCallbacks(initialDeleteRunnable);
                    if (repeatDeleteRunnable != null) handler.removeCallbacks(repeatDeleteRunnable);
                    initialDeleteRunnable = null;
                    repeatDeleteRunnable = null;
                }
                return true;
            }
        });

        btnKeyboard.setOnClickListener(v -> {
            if (mWhisper != null) stopTranscription();
            switchToPreviousInputMethod();
        });

        btnTranslate.setOnClickListener(v -> {
            translate = !translate;
            btnTranslate.setImageResource(translate ? R.drawable.ic_english_on_36dp : R.drawable.ic_english_off_36dp);
        });

        btnEnter.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            }
        });

        btnModeAuto.setOnClickListener(v -> {
            modeAuto = !modeAuto;
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("imeModeAuto", modeAuto);
            editor.apply();
            if (layoutButtons != null) layoutButtons.setVisibility(modeAuto ? View.GONE : View.VISIBLE);
            btnModeAuto.setImageResource(modeAuto ? R.drawable.ic_auto_on_36dp : R.drawable.ic_auto_off_36dp);
            switchToPreviousInputMethod();
        });

        return view;
    }

    // --- Model Initialization ---

    private void initModel() {
        mWhisper = new Whisper(this);
        // MoonshineEngine resolves ONNX assets internally
        mWhisper.loadModel(new File(""), new File(""), false);
        mWhisper.setListener(new Whisper.WhisperListener() {
            @Override
            public void onUpdateReceived(String message) {}

            @Override
            public void onResultReceived(WhisperResult whisperResult) {
                handler.post(() -> {
                    if (processingBar != null) processingBar.setIndeterminate(false);
                    if (tvStatus != null) {
                        tvStatus.setText("");
                        tvStatus.setVisibility(View.GONE);
                    }
                });

                String result = whisperResult.getResult();
                if (whisperResult.getLanguage() != null && whisperResult.getLanguage().equals("zh")) {
                    boolean simpleChinese = sp.getBoolean("simpleChinese", false);
                    result = simpleChinese ? ZhConverterUtil.toSimple(result) : ZhConverterUtil.toTraditional(result);
                }

                boolean commitSuccess = false;
                InputConnection ic = getCurrentInputConnection();
                if (ic != null && result != null && result.trim().length() > 0) {
                    commitSuccess = ic.commitText(result.trim() + " ", 1);
                }

                if (modeAuto && commitSuccess) {
                    handler.postDelayed(() -> switchToPreviousInputMethod(), 100);
                }
            }
        });
    }

    private void startTranscription() {
        if (countDownTimer != null) countDownTimer.cancel();
        handler.post(() -> {
            if (processingBar != null) {
                processingBar.setProgress(0);
                processingBar.setIndeterminate(true);
            }
        });

        if (mWhisper != null) {
            mWhisper.setAction(Whisper.ACTION_TRANSCRIBE);
            mWhisper.start();
        }
    }

    private void stopTranscription() {
        handler.post(() -> {
            if (processingBar != null) processingBar.setIndeterminate(false);
        });
        if (mWhisper != null) {
            mWhisper.stop();
        }
    }

    private boolean checkRecordPermission() {
        int permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (tvStatus != null) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(getString(R.string.need_record_audio_permission));
            }
        }
        return (permission == PackageManager.PERMISSION_GRANTED);
    }

    private void deinitModel() {
        if (mWhisper != null) {
            mWhisper.unloadModel();
            mWhisper = null;
        }
    }
}
