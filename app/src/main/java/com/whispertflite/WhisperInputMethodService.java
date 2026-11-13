package com.whispertflite;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.whispertflite.asr.Recorder;
import com.whispertflite.asr.Whisper;
import com.whispertflite.asr.WhisperResult;
import com.whispertflite.utils.HapticFeedback;

import java.io.File;

public class WhisperInputMethodService extends InputMethodService {
    private static final String TAG = "WhisperInputMethodService";
    private static final int KEYCODE_PTT = 269;

    private TextView btnRecord;
    private TextView btnKeyboard;
    private TextView btnEnter;
    private TextView btnDel;
    private TextView btnPunctComma;
    private TextView btnPunctPeriod;
    private TextView btnPunctQuestion;
    private TextView btnPunctExclamation;
    private TextView btnPunctApostrophe;
    private ProgressBar processingBar;
    private TextView tvStatus;

    private Recorder mRecorder = null;
    private Whisper mWhisper = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context mContext;
    private CountDownTimer countDownTimer;

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

    private boolean isInputTargetValid() {
        if (!isInputViewShown()) return false;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei == null) return false;
        return (ei.inputType & EditorInfo.TYPE_MASK_CLASS) != EditorInfo.TYPE_NULL;
    }

    // --- Physical Hardware Key Interception (Orange PTT Side Key: Keycode 269) ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PTT || keyCode == 269) {
            if (isInputTargetValid() && event.getRepeatCount() == 0) {
                if (checkRecordPermission()) {
                    if (mWhisper != null && !mWhisper.isInProgress() && mRecorder != null && !mRecorder.isInProgress()) {
                        isDiscarded = false;
                        recordStartTime = System.currentTimeMillis();
                        HapticFeedback.vibrate(this);
                        startRecordingAction();
                    }
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PTT || keyCode == 269) {
            if (mRecorder != null && mRecorder.isInProgress()) {
                long holdDuration = System.currentTimeMillis() - recordStartTime;
                isDiscarded = (holdDuration < 300); // Discard accidental taps < 300ms
                stopRecordingAction();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    // --- Recording Actions & Visual Feedback ---

    private void startRecordingAction() {
        if (mWhisper != null && mWhisper.isInProgress()) return;

        handler.post(() -> {
            if (btnRecord != null) btnRecord.setTextColor(0xFFFF3D00); // Highlight red
            if (processingBar != null) {
                processingBar.setIndeterminate(false);
                processingBar.setProgress(100);
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

        if (mRecorder != null) mRecorder.start();
    }

    private void stopRecordingAction() {
        handler.post(() -> {
            if (btnRecord != null) btnRecord.setTextColor(0xFF78909C); // Restore gray
        });
        if (mRecorder != null && mRecorder.isInProgress()) {
            mRecorder.stop();
        }
    }

    // --- Smart Punctuation Helper ---

    private void commitPunctuation(String punct) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Moonshine transcribes with a trailing space. Strip it before punctuation.
        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (before != null && before.length() > 0 && before.charAt(0) == ' ') {
            if (!punct.equals(" ")) {
                ic.deleteSurroundingText(1, 0);
            }
        }

        // Attach punctuation + auto-insert trailing space (except for apostrophes)
        if (punct.equals("'")) {
            ic.commitText("'", 1);
        } else {
            ic.commitText(punct + " ", 1);
        }
    }

    // --- Layout Inflation & Click Bindings ---

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateInputView() {
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

        processingBar = view.findViewById(R.id.processing_bar);
        tvStatus = view.findViewById(R.id.tv_status);
        btnRecord = view.findViewById(R.id.btnRecord);
        btnKeyboard = view.findViewById(R.id.btnKeyboard);
        btnEnter = view.findViewById(R.id.btnEnter);
        btnDel = view.findViewById(R.id.btnDel);
        btnPunctComma = view.findViewById(R.id.btnPunctComma);
        btnPunctPeriod = view.findViewById(R.id.btnPunctPeriod);
        btnPunctQuestion = view.findViewById(R.id.btnPunctQuestion);
        btnPunctExclamation = view.findViewById(R.id.btnPunctExclamation);
        btnPunctApostrophe = view.findViewById(R.id.btnPunctApostrophe);

        // Punctuation click handlers
        if (btnPunctComma != null) btnPunctComma.setOnClickListener(v -> commitPunctuation(","));
        if (btnPunctPeriod != null) btnPunctPeriod.setOnClickListener(v -> commitPunctuation("."));
        if (btnPunctQuestion != null) btnPunctQuestion.setOnClickListener(v -> commitPunctuation("?"));
        if (btnPunctExclamation != null) btnPunctExclamation.setOnClickListener(v -> commitPunctuation("!"));
        if (btnPunctApostrophe != null) btnPunctApostrophe.setOnClickListener(v -> commitPunctuation("'"));

        // Switch back to T9 / standard keyboard
        if (btnKeyboard != null) {
            btnKeyboard.setOnClickListener(v -> {
                if (mWhisper != null) stopTranscription();
                switchToPreviousInputMethod();
            });
        }

        // Enter key
        if (btnEnter != null) {
            btnEnter.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                }
            });
        }

        // Backspace with repeat-on-hold
        if (btnDel != null) {
            btnDel.setOnTouchListener(new View.OnTouchListener() {
                private Runnable initialDel;
                private Runnable repeatDel;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic == null) return false;

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        initialDel = () -> {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                            repeatDel = new Runnable() {
                                @Override
                                public void run() {
                                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                                    handler.postDelayed(this, 100);
                                }
                            };
                            handler.postDelayed(repeatDel, 100);
                        };
                        handler.postDelayed(initialDel, 400);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (initialDel != null) handler.removeCallbacks(initialDel);
                        if (repeatDel != null) handler.removeCallbacks(repeatDel);
                    }
                    return true;
                }
            });
        }

        // Soft tap mic fallback
        if (btnRecord != null) {
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
        }

        // Recorder setup
        mRecorder = new Recorder(this);
        mRecorder.setListener(new Recorder.RecorderListener() {
            @Override
            public void onUpdateReceived(String message) {
                if (message.equals(Recorder.MSG_RECORDING_DONE)) {
                    HapticFeedback.vibrate(mContext);
                    handler.post(() -> {
                        if (btnRecord != null) btnRecord.setTextColor(0xFF78909C);
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
                        if (btnRecord != null) btnRecord.setTextColor(0xFF78909C);
                        if (processingBar != null) processingBar.setProgress(0);
                    });
                }
            }
        });

        checkRecordPermission();
        return view;
    }

    private void initModel() {
        mWhisper = new Whisper(this);
        mWhisper.loadModel(new File(""), new File(""), false);
        mWhisper.setListener(new Whisper.WhisperListener() {
            @Override
            public void onUpdateReceived(String message) {}

            @Override
            public void onResultReceived(WhisperResult whisperResult) {
                handler.post(() -> {
                    if (processingBar != null) processingBar.setIndeterminate(false);
                });

                String result = whisperResult.getResult();
                InputConnection ic = getCurrentInputConnection();
                if (ic != null && result != null && result.trim().length() > 0) {
                    ic.commitText(result.trim() + " ", 1);
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
        if (mWhisper != null) mWhisper.stop();
    }

    private boolean checkRecordPermission() {
        int permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO);
        return (permission == PackageManager.PERMISSION_GRANTED);
    }

    private void deinitModel() {
        if (mWhisper != null) {
            mWhisper.unloadModel();
            mWhisper = null;
        }
    }
}
