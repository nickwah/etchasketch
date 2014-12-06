package com.mylikes.likes.etchasketch;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by nick on 12/4/14.
 */
public class RoundMenu extends ViewGroup {

    private float radsPerChild;
    private float density;

    public RoundMenu(Context context) {
        super(context);
        init();
    }

    public RoundMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        density = getResources().getDisplayMetrics().density;

        setClickable(true);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // TODO: switch state and invalidate
                }

                return false;
            }
        });
    }

    /**
     * Any layout manager that doesn't scroll will want this.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        radsPerChild = (float)(Math.PI / 2.0 / count);

        // Measurement will ultimately be computing these values.
        int maxWidth = (int)(200 * density);
        int maxHeight = maxWidth;
        int childState = 0;

        // Iterate through all children, measuring them and computing our dimensions
        // from their size.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                // Measure the child.
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Report our final dimensions.
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path clipping = new Path();
        // TODO: make the minimum circle size a setting
        clipping.addCircle(0, getHeight(), 20 * density, Path.Direction.CW);
        canvas.clipPath(clipping);
        super.onDraw(canvas);
        Region all = new Region(0, 0, getWidth(), getHeight());
        canvas.clipRegion(all);
    }

    public static class LayoutParams extends MarginLayoutParams {
        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         */
        public int gravity = Gravity.TOP | Gravity.START;

        public static int CORNER_TOPLEFT = 0;
        public static int CORNER_TOPRIGHT = 1;
        public static int CORNER_BOTTOMLEFT = 2;
        public static int CORNER_BOTTOMRIGHT = 2;

        public int corner = CORNER_BOTTOMLEFT;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            // Pull the layout param values from the layout XML during
            // inflation.  This is not needed if you don't care about
            // changing the layout behavior in XML.
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.RoundMenuLP);
            gravity = a.getInt(R.styleable.RoundMenuLP_android_layout_gravity, gravity);
            corner = a.getInt(R.styleable.RoundMenuLP_corner, corner);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

}
