package com.mylikes.likes.etchasketch;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.util.Log;

import com.mylikes.likes.etchasketch.R;

/**
 * Created by nick on 11/22/14.
 */
public class SpotSizeBar extends SeekBar {
    private int minSize = 5;

    protected Paint mPaint;
    protected ColorStateList mFgColor, mBgColor;
    protected float density = 2;
    private static int smoothness = 10;
    private OnSizeChangedListener sizeChangedListener;

    public interface OnSizeChangedListener {
        public void sizeChanged(float size);
    }

    public SpotSizeBar(Context context) {
        super(context);
        init();
    }

    public SpotSizeBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SpotSizeBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        sizeChangedListener = listener;
    }

    protected void init() {
        setMin(1);
        setMax(50);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        density = metrics.density;

        setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (sizeChangedListener != null) sizeChangedListener.sizeChanged(getSize() * density);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public int getMin() {
        return minSize / smoothness;
    }

    @Override
    public int getMax() {
        return super.getMax() / smoothness;
    }

    public void setMin(int minSize) {
        this.minSize = minSize * smoothness;
    }
    public void setMax(int maxSize) {
        super.setMax(maxSize * smoothness);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFgColor = getResources().getColorStateList(R.color.pentool_fg);
        mBgColor = getResources().getColorStateList(R.color.pentool_bg);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {

        mPaint.setColor(getResources().getColor(R.color.btn_fg));
        final boolean vertical = true;

        float scale = getWidth() / (float)getMax() / 2.5f / density;
        float width = 15;
        float border_width = 5;
        float bar_width = 25;
        float bar_height = 13;

        float r1 = getMin() * scale * density;
        float r2 = getMax() * scale * density;

        final float center = getWidth() / 2;

        if (r1 > center) r1 = center;
        if (r2 > center) r2 = center;

        final float start = (vertical ? getPaddingTop() : getPaddingLeft()) + r1 + 1 * density;
        final float end = getHeight() - r2 - 5 * density;

        
        mPaint.setColor(0xff33a2d7);
        c.drawRect(center-width + border_width,start+border_width,center+width-border_width,end-border_width,mPaint);
        //c.drawCircle(vertical ? center : end, vertical ? end : center, r2, mPaint);

        float progress = getProgress() / (float)smoothness;
        int size = getSize();
        mPaint.setColor(isSelected() ? 0xffffffff : 0xffcccccc);
        //c.drawCircle(center,
                //progress / (float)(getMax() - getMin()) * (getHeight() - center - 4 * density),
                /*(float) (Math.max(size, 5)) * scale * 1.1f * density*///10, mPaint);
        float off = progress / (float)(getMax() - getMin()) * (getHeight() - center - 4 * density);
        float ycenter = start + off;
        RectF r = new RectF(center - bar_width, ycenter - bar_height, center + bar_width, ycenter + bar_height);
        c.drawRoundRect(r, 3 * density, 3 * density, mPaint);
    }

    public int getSize() {
        float f = (getProgress() / (float)smoothness) / (float)(getMax() - getMin());
        return getMin() + (int)(getMax() * Math.pow(f, 1.6));
    }

    public void setSize(int size) {
        setProgress((size - getMin()) * smoothness);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                setProgress((int) (super.getMax() * event.getY() / getHeight()));
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}
