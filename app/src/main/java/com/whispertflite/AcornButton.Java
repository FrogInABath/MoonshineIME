package org.woheller69.whisper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.widget.AppCompatImageButton;

public class AcornButton extends AppCompatImageButton {

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
        setImageResource(R.drawable.ic_acorn);
        setBackgroundResource(android.R.color.transparent);
        setScaleType(ScaleType.CENTER_INSIDE);
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

        // Anchor rotation directly in the center
        setPivotX(getWidth() / 2f);
        setPivotY(getHeight() / 2f);

        // 360-degree spin
        ObjectAnimator rotate = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f);
        rotate.setDuration(450);
        rotate.setInterpolator(new OvershootInterpolator(1.2f));

        // Squash and stretch bounce
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.88f, 1.05f, 1f);
        scaleX.setDuration(450);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.88f, 1.05f, 1f);
        scaleY.setDuration(450);

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
