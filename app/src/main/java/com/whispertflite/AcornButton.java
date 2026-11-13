package org.woheller69.whisper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.widget.AppCompatTextView;

public class AcornButton extends AppCompatTextView {

    private boolean isSpinning = false;

    public AcornButton(Context context) {
        super(context);
        init();
    }

    public AcornButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AcornButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Use the centered layer-list background to keep the acorn perfectly round
        setBackgroundResource(R.drawable.bg_acorn_button);
        setGravity(Gravity.CENTER);
    }

    @Override
    public boolean performClick() {
        if (!isSpinning) {
            playSpinAnimation();
        }
        return super.performClick();
    }

    private void playSpinAnimation() {
        isSpinning = true;

        setPivotX(getWidth() / 2f);
        setPivotY(getHeight() / 2f);

        // 360-degree flat spin
        ObjectAnimator rotate = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f);
        rotate.setDuration(420);
        rotate.setInterpolator(new OvershootInterpolator(1.2f));

        // Tactile compression
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.86f, 1.05f, 1f);
        scaleX.setDuration(420);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.86f, 1.05f, 1f);
        scaleY.setDuration(420);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(rotate, scaleX, scaleY);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setRotation(0f);
                setScaleX(1f);
                setScaleY(1f);
                isSpinning = false;
            }
        });
        set.start();
    }
}
