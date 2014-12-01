package com.mylikes.likes.etchasketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

/**
 * Created by nick on 11/29/14.
 */
public abstract class MoveableDrawing {
    public static final String TAG = "Drawing";
    protected Context context;
    protected int x, y, width, height;
    protected int color = Color.WHITE;
    protected final Paint borderColor = new Paint(Color.WHITE);
    protected final Paint fillColor = new Paint(Color.WHITE);
    public static final float RADIUS = 6.0f;
    public float threshold = 8.0f;
    protected static Bitmap closeIcon;
    protected static Bitmap resizeIcon;
    protected static Bitmap editIcon;

    public MoveableDrawing(Context context, int x, int y) {
        this.context = context;
        this.x = x;
        this.y = y;
        fillColor.setStyle(Paint.Style.FILL);
        borderColor.setStyle(Paint.Style.STROKE);
        fillColor.setColor(Color.WHITE);
        // TODO: set radius, threshold, and stroke width based on density
        borderColor.setStrokeWidth(2.0f);
        borderColor.setColor(Color.LTGRAY);
        if (closeIcon == null) {
            closeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.drawing_remove);
            resizeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.drawing_resize);
            editIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.drawing_edit);
        }
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public void moveBy(int x, int y) {
        this.x += x;
        this.y += y;
    }

    public void resizeCorner(String corner, int x, int y) {
        // Um, you should probably override this
        width = Math.max(10, width + x);
    }

    public boolean contains(int x, int y) {
        Log.d(TAG, "x=" + x + " y=" + y);
        return (this.x < x && this.x + width > x) && (this.y < y && this.y + height > y);
    }

    public String closestCorner(int x, int y) {
        float left = this.x - threshold * 4, right = this.x + width + threshold * 4;
        float top = this.y - threshold * 4, bottom = this.y + height + threshold * 4;
        if (x < this.x + threshold && x > left) {
            Log.d(TAG, "left");
            if (y < this.y + threshold && y > top) {
                return "tl";
            } else if (y > this.y + height - threshold && y < bottom) {
                return "bl";
            }
        } else if (x > this.x - threshold + width && x < right) {
            Log.d(TAG, "right");
            if (y < this.y + threshold && y > top) {
                return "tr";
            } else if (y > this.y + height - threshold && y < bottom) {
                return "br";
            }
        }
        Log.d(TAG, "nope");
        return null;
    }

    public abstract void renderInto(Canvas canvas, boolean showBorders);

    public void renderBorders(Canvas canvas) {
        float left = x - threshold, right = x + width + threshold;
        float top = y - threshold, bottom = y + height + threshold;
        canvas.drawLine(left, top, right, top, borderColor);
        canvas.drawLine(right, top, right, bottom, borderColor);
        canvas.drawLine(left, bottom, right, bottom, borderColor);
        canvas.drawLine(left, top, left, bottom, borderColor);
        canvas.drawCircle(left, top, RADIUS, fillColor);
        //canvas.drawCircle(right, top, RADIUS, fillColor);
        //canvas.drawCircle(right, bottom, RADIUS, fillColor);
        canvas.drawCircle(left, bottom, RADIUS, fillColor);
        canvas.drawBitmap(closeIcon, right - closeIcon.getWidth() / 2, top - closeIcon.getHeight() / 2, fillColor);
        canvas.drawBitmap(resizeIcon, right - resizeIcon.getWidth() / 2, bottom - resizeIcon.getHeight() / 2, fillColor);
    }
}
