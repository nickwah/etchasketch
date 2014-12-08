package com.mylikes.likes.etchasketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by nick on 12/7/14.
 */
public class RoundMenuItem extends RoundSubMenu {

    private Paint popupPaint;

    public RoundMenuItem(Context context) {
        super(context);
    }

    public RoundMenuItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundMenuItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();
        popupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        popupPaint.setColor(getResources().getColor(R.color.btn_pressed_bg));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (openPct != 0) {
            //Log.d(TAG, "ondraw item");
            double rads = Math.toRadians(angleDgs);
            double y = - 15 * density * openPct - 10 * density - getHeight();
            float x2 = (float) (-y * Math.sin(rads));
            float y2 = (float) (y * Math.cos(rads));
            float radius = 15 * density + 10 * density * openPct;
            popupPaint.setAlpha((int) (255 * openPct));
            canvas.drawCircle(x2 + getWidth() / 2, y2 + getHeight() / 2, radius, popupPaint);
            canvas.drawBitmap(image, x2, y2, popupPaint);
        }
    }

    public void drawChildren(Canvas canvas) {
        // No children; instead we draw the bubble if this is expanded in onDraw
    }
}