/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.ceco.pie.gravitybox;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import java.util.ArrayList;
import java.util.HashSet;

import androidx.annotation.Keep;

public class KeyButtonRipple extends Drawable {

    private static final float GLOW_MAX_SCALE_FACTOR = 1.35f;
    private static final int ANIMATION_DURATION_SCALE = 350;
    private static final int ANIMATION_DURATION_FADE = 450;

    private Paint mRipplePaint;
    private float mGlowAlpha = 0f;
    private float mGlowAlphaMax = 0.2f;
    private float mGlowScale = 1f;
    private boolean mPressed;
    private int mMaxWidth;

    private final Interpolator mInterpolator = new LogInterpolator();
    private final Interpolator mAlphaExitInterpolator = new PathInterpolator(0f, 0f, 0.8f, 1f);

    private final HashSet<Animator> mRunningAnimations = new HashSet<>();
    private final ArrayList<Animator> mTmpArray = new ArrayList<>();

    public KeyButtonRipple(Context ctx, View targetView) {
        Resources res = ctx.getResources();
        int resId = res.getIdentifier("key_button_ripple_max_width", "dimen",
                ModNavigationBar.PACKAGE_NAME);
        mMaxWidth =  ctx.getResources().getDimensionPixelSize(resId);
    }

    private Paint getRipplePaint() {
        if (mRipplePaint == null) {
            mRipplePaint = new Paint();
            mRipplePaint.setAntiAlias(true);
            mRipplePaint.setColor(0xffffff);
            mRipplePaint.setAlpha((int)(mGlowAlphaMax * 255f));
        }
        return mRipplePaint;
    }

    public void setGlowColor(int color) {
        int rgb = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
        getRipplePaint().setColor(rgb);
        mGlowAlphaMax = Color.alpha(color) / 255f;
    }

    private void drawSoftware(Canvas canvas) {
        if (mGlowAlpha > 0f) {
            final Paint p = getRipplePaint();
            p.setAlpha((int)(mGlowAlpha * 255f));

            final float w = getBounds().width();
            final float h = getBounds().height();
            final boolean horizontal = w > h;
            final float diameter = getRippleSize() * mGlowScale;
            final float radius = diameter * .5f;
            final float cx = w * .5f;
            final float cy = h * .5f;
            final float rx = horizontal ? radius : cx;
            final float ry = horizontal ? cy : radius;
            final float corner = horizontal ? cy : cx;

            canvas.drawRoundRect(cx - rx, cy - ry,
                    cx + rx, cy + ry,
                    corner, corner, p);
        }
    }


    @Override
    public void draw(Canvas canvas) {
        drawSoftware(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        // Not supported.
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // Not supported.
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private boolean isHorizontal() {
        return getBounds().width() > getBounds().height();
    }

    public float getGlowAlpha() {
        return mGlowAlpha;
    }

    @Keep
    public void setGlowAlpha(float x) {
        mGlowAlpha = x;
        invalidateSelf();
    }

    public float getGlowScale() {
        return mGlowScale;
    }

    @Keep
    public void setGlowScale(float x) {
        mGlowScale = x;
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean pressed = false;
        for (int i = 0; i < state.length; i++) {
            if (state[i] == android.R.attr.state_pressed) {
                pressed = true;
                break;
            }
        }
        if (pressed != mPressed) {
            setPressed(pressed);
            mPressed = pressed;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    public void setPressed(boolean pressed) {
        setPressedSoftware(pressed);
    }

    private void cancelAnimations() {
        mTmpArray.addAll(mRunningAnimations);
        int size = mTmpArray.size();
        for (int i = 0; i < size; i++) {
            Animator a = mTmpArray.get(i);
            a.cancel();
        }
        mTmpArray.clear();
        mRunningAnimations.clear();
    }

    private void setPressedSoftware(boolean pressed) {
        if (pressed) {
            enterSoftware();
        } else {
            exitSoftware();
        }
    }

    private void enterSoftware() {
        cancelAnimations();
        mGlowAlpha = mGlowAlphaMax;
        ObjectAnimator scaleAnimator = ObjectAnimator.ofFloat(this, "glowScale",
                0f, GLOW_MAX_SCALE_FACTOR);
        scaleAnimator.setInterpolator(mInterpolator);
        scaleAnimator.setDuration(ANIMATION_DURATION_SCALE);
        scaleAnimator.addListener(mAnimatorListener);
        scaleAnimator.start();
        mRunningAnimations.add(scaleAnimator);
    }

    private void exitSoftware() {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "glowAlpha", mGlowAlpha, 0f);
        alphaAnimator.setInterpolator(mAlphaExitInterpolator);
        alphaAnimator.setDuration(ANIMATION_DURATION_FADE);
        alphaAnimator.addListener(mAnimatorListener);
        alphaAnimator.start();
        mRunningAnimations.add(alphaAnimator);
    }

    private int getRippleSize() {
        int size = isHorizontal() ? getBounds().width() : getBounds().height();
        return Math.min(size, mMaxWidth);
    }

    private final AnimatorListenerAdapter mAnimatorListener =
            new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mRunningAnimations.remove(animation);
            if (mRunningAnimations.isEmpty() && !mPressed) {
                invalidateSelf();
            }
        }
    };

    /**
     * Interpolator with a smooth log deceleration
     */
    private static final class LogInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return 1 - (float) Math.pow(400, -input * 1.4);
        }
    }
}