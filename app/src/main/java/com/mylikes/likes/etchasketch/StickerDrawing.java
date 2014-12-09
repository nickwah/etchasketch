package com.mylikes.likes.etchasketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/**
 * Created by nick on 12/8/14.
 */
public class StickerDrawing extends MoveableDrawing {
    protected Bitmap bitmap;
    private final Paint paint = new Paint();

    public StickerDrawing(Context context, Bitmap bitmap, int x, int y) {
        super(context, x, y);
        this.bitmap = bitmap;
        paint.setColor(color);
        measure(bitmap);
        Log.d(TAG, "x=" + x + " y=" + y + " w=" + width + " h=" + height);
    }

    private void measure(Bitmap bitmap) {
        width = bitmap.getWidth();
        height = bitmap.getHeight();
    }

    public void resizeBy(float amount) {

    }

    public void renderInto(Canvas canvas, boolean showBorders) {
        float finalX = x, finalY = y;
        canvas.save();
        canvas.translate(finalX, finalY);
        if (rotation != 0) {
            canvas.rotate((float)Math.toDegrees(rotation), width / 2, height / 2);
        }

        // TODO: use a dest Rect so we can scale to width x height
        canvas.drawBitmap(bitmap, -threshold / 2, height - threshold / 2, paint);
        if (showBorders) renderBorders(canvas);
        canvas.restore();
    }

    @Override
    public void renderBorders(Canvas canvas) {
        super.renderBorders(canvas);
        float left = x - threshold, right = x + width + threshold;
        float top = y - threshold, bottom = y + height + threshold;
        //canvas.drawBitmap(editIcon, left - editIcon.getWidth() / 2, top - editIcon.getHeight() / 2, fillColor);
    }

}
