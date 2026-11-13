package com.whispertflite;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.inputmethodservice.InputMethodService;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ImageButton;
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

    // CAT S22 Orange Action key constants
    private static final int KEYCODE_PTT = 269;
    private static final int SCANCODE_CAT_ACTION = 35;

    private ImageButton btnRecord;
    private TextView btnKeyboard;
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

    // --- CAT S22 Flip Hardware Keypad Overrides: Force Soft Keyboard on Tap ---

    @Override
    public boolean onEvaluateInputViewShown() {
        super.onEvaluateInputViewShown();
        // Overrides Android Go default which suppresses soft IME when a physical keypad is present
        return true;
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        return true;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        if (mWhisper == null) {
            initModel();
        }
        // Force the IME window to become immediately visible upon field focus
        showWindow(true);
        requestShowSelf(0);
    }

    private boolean isInputTargetValid() {
        return getCurrentInputConnection() != null;
    }

    private boolean isActionKey(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_PTT || keyCode == 269) return true;
        if (event != null && (event.getScanCode() == SCANCODE_CAT_ACTION || event.getScanCode() == 0x23)) {
            return true;
        }
        return false;
    }

    // --- Physical Hardware Key Interception (Orange PTT Side Key) ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: keyCode=" + keyCode + ", scanCode=" + (event != null ? event.getScanCode() : 0));

        if (isActionKey(keyCode, event)) {
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
        Log.d(TAG, "onKeyUp: keyCode=" + keyCode + ", scanCode=" + (event != null ? event.getScanCode() : 0));

        if (isActionKey(keyCode, event)) {
            if (mRecorder != null && mRecorder.isInProgress()) {
                long holdDuration = System.currentTimeMillis() - recordStartTime;
                isDiscarded = (holdDuration < 150);
                stopRecordingAction();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    // --- 3D Horizontal-Axis (rotationX) Spin Animation ---

    private void animateHorizontalSpin(View v) {
        if (v == null) return;
        v.setCameraDistance(6000 * getResources().getDisplayMetrics().density);
        ObjectAnimator spin = ObjectAnimator.ofFloat(v, "rotationX", 0f, 360f);
        spin.setDuration(350);
        spin.setInterpolator(new AccelerateDecelerateInterpolator());
        spin.start();
    }

    // --- Recording Actions & Visual Feedback ---

    private void startRecordingAction() {
        if (mWhisper != null && mWhisper.isInProgress()) return;

        handler.post(() -> {
            if (btnRecord != null) {
                // Electric Cyan Cybersigil Glow
                btnRecord.setColorFilter(Color.parseColor("#00E5FF"), PorterDuff.Mode.SRC_ATOP);
                animateHorizontalSpin(btnRecord);
            }
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
            if (btnRecord != null) {
                btnRecord.clearColorFilter();
            }
        });
        if (mRecorder != null && mRecorder.isInProgress()) {
            mRecorder.stop();
        }
    }

    // --- Smart Punctuation Helper ---

    private void commitPunctuation(View view, String punct) {
        animateHorizontalSpin(view);
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (before != null && before.length() > 0 && before.charAt(0) == ' ') {
            if (!punct.equals(" ")) {
                ic.deleteSurroundingText(1, 0);
            }
        }

        if (punct.equals("'")) {
            ic.commitText("'", 1);
        } else {
            ic.commitText(punct + " ", 1);
        }
    }

    // --- Layout Construction & Bindings ---

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
        btnPunctComma = view.findViewById(R.id.btnPunctComma);
        btnPunctPeriod = view.findViewById(R.id.btnPunctPeriod);
        btnPunctQuestion = view.findViewById(R.id.btnPunctQuestion);
        btnPunctExclamation = view.findViewById(R.id.btnPunctExclamation);
        btnPunctApostrophe = view.findViewById(R.id.btnPunctApostrophe);

        if (btnPunctComma != null) btnPunctComma.setOnClickListener(v -> commitPunctuation(v, ","));
        if (btnPunctPeriod != null) btnPunctPeriod.setOnClickListener(v -> commitPunctuation(v, "."));
        if (btnPunctQuestion != null) btnPunctQuestion.setOnClickListener(v -> commitPunctuation(v, "?"));
        if (btnPunctExclamation != null) btnPunctExclamation.setOnClickListener(v -> commitPunctuation(v, "!"));
        if (btnPunctApostrophe != null) btnPunctApostrophe.setOnClickListener(v -> commitPunctuation(v, "'"));

        if (btnKeyboard != null) {
            btnKeyboard.setOnClickListener(v -> {
                animateHorizontalSpin(v);
                if (mWhisper != null) stopTranscription();
                switchToPreviousInputMethod();
            });
        }

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

        mRecorder = new Recorder(this);
        mRecorder.setListener(new Recorder.RecorderListener() {
            @Override
            public void onUpdateReceived(String message) {
                if (message.equals(Recorder.MSG_RECORDING_DONE)) {
                    HapticFeedback.vibrate(mContext);
                    handler.post(() -> {
                        if (btnRecord != null) btnRecord.clearColorFilter();
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
                        if (btnRecord != null) btnRecord.clearColorFilter();
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
