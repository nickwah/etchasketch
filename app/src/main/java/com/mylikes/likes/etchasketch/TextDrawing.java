package com.mylikes.likes.etchasketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

/**
 * Created by nick on 11/29/14.
 */
public class TextDrawing extends MoveableDrawing {
    public static final String[] FONTS = {"Overlock-Regular.ttf", "AmaticSC-Bold.ttf", "Bangers.ttf", "ClickerScript-Regular.ttf", "ClickerScript-Regular.ttf"};
    public static final String TAG = "TextDrawing";
    private static HashMap<String, Typeface> fontCache;
    static  {
        fontCache = new HashMap<String, Typeface>(6);
    }
    private String text, font = "Overlock-Regular.ttf";
    private float fontSize = 80.0f;
    private final Paint paint = new Paint();

    public TextDrawing(Context context, String text, int x, int y) {
        super(context, x, y);
        this.text = text;
        fontSize = context.getResources().getDimensionPixelSize(R.dimen.default_font_size);
        paint.setColor(color);
        paint.setTextSize(fontSize);
        paint.setTypeface(getFont());
        measure(text);
        Log.d(TAG, "x=" + x + " y=" + y + " w=" + width + " h=" + height);
    }

    private void measure(String text) {
        Rect bounds = new Rect();
        paint.setTextSize(fontSize);
        paint.getTextBounds(text, 0, text.length(), bounds);
        width = bounds.width();
        height = bounds.height();
    }

    public void setText(String text) {
        this.text = text;
        measure(text);
    }
    public String getText() {
        return text;
    }

    public void setFont(String font) {
        this.font = font;
    }

    private Typeface getFont() {
        if (fontCache.containsKey(font)) {
            return fontCache.get(font);
        }
        Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/" + font);
        fontCache.put(font, face);
        return face;
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        paint.setColor(color);
    }

    @Override
    public void resizeCorner(String corner, int x, int y) {
        float oldWidth = width;
        Log.d(TAG, "corner: " + corner + " x=" + x + " y=" + y + " width=" + width);
        if (corner.endsWith("r")) {
            width = Math.max(10, width + x);
        } else {
            if (width - x < 10) return;
            width -= x;
            this.x += x;
        }
        paint.setTextSize(10);
        fontSize *= (width / oldWidth);
        int newWidth = width;
        Log.d(TAG, "New width: " + width + " font: " + fontSize);
        paint.setTextSize(fontSize);
        if (corner.startsWith("t")) {
            int oldHeight = height;
            measure(text);
            this.y += oldHeight - height;
        } else {
            measure(text);
        }
        width = newWidth;
    }

    public void renderInto(Canvas canvas, boolean showBorders) {
        canvas.drawText(text, x - threshold / 2, y + height - threshold / 2, paint);
        if (showBorders) renderBorders(canvas);
    }

    @Override
    public void renderBorders(Canvas canvas) {
        super.renderBorders(canvas);
        float left = x - threshold, right = x + width + threshold;
        float top = y - threshold, bottom = y + height + threshold;
        canvas.drawBitmap(editIcon, left - editIcon.getWidth() / 2, top - editIcon.getHeight() / 2, fillColor);
    }
}
