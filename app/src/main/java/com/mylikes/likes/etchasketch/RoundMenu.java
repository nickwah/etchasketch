package com.mylikes.likes.etchasketch;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

/**
 * Created by nick on 12/4/14.
 */
public class RoundMenu extends ViewGroup {

    public static final String TAG = "RoundMenu";
    private float radsPerChild;
    private float density;
    private int currentRadius, toolRadius = 50;
    private Bitmap image;
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double startAngle = Math.PI / 2;
    private Animator currentAnimation;

    public RoundMenu(Context context) {
        super(context);
        init();
    }

    public RoundMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RoundMenu,
                0, 0);

        int icon = a.getResourceId(R.styleable.RoundMenu_mainIcon, -1);
        int backgroundColor = a.getColor(R.styleable.RoundMenu_backgroundColor, -1);
        if (backgroundColor != -1) {
            background.setColor(backgroundColor);
        }
        if (icon != -1) {
            image = BitmapFactory.decodeResource(getResources(), icon);
        }
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
                    if (currentAnimation != null) {
                        currentAnimation.cancel();
                    }
                    ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
                    animation.setDuration(200);
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            currentRadius = (int) (40 + 40 * animation.getAnimatedFraction());
                            setAlpha(0.5f + animation.getAnimatedFraction() / 2.0f);
                            invalidate();
                        }
                    });
                    animation.addListener(animationEndListener());
                    animation.start();
                    currentAnimation = animation;
                    // TODO: switch state and invalidate
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (currentAnimation != null) {
                        // TODO: if they tap quickly, just let the animation finish, and we'll close when they touch outside
                        currentAnimation.cancel();
                    }
                    ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
                    animation.setDuration(200);
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            currentRadius = (int) (40 + 40 * (1.0f - animation.getAnimatedFraction()));
                            setAlpha(1.0f - animation.getAnimatedFraction() / 2.0f);
                            invalidate();
                        }
                    });
                    animation.addListener(animationEndListener());
                    animation.start();
                    currentAnimation = animation;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // TODO: find the child that they are on (if the touch is over a child)
                    // if the child is an instance of RoundMenuButton, call touchDown()
                }

                return false;
            }
        });

        currentRadius = 40;
        setAlpha(0.5f);
        startAngle = Math.PI * 3 / 2;
    }

    private Animator.AnimatorListener animationEndListener() {
        return new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimation = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };
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
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
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
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();
                double angle = radsPerChild * i + radsPerChild / 2.0f + startAngle;
                int centerX = (int) (Math.cos(angle) * toolRadius * density + 20 * density);
                int centerY = (int) (Math.sin(angle) * toolRadius * density + getHeight() - 20 * density);
                Log.d(TAG, "position with angle " + angle + " at " + centerX + ", " + centerY);
                child.layout(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO: adjust based on corner
        canvas.drawCircle(20 * density, getHeight() - 20 * density, currentRadius * density, background);
        canvas.drawBitmap(image, 20 * density - image.getWidth() / 2, getHeight() - 20 * density - image.getHeight() / 2, background);
        Path clipping = new Path();
        // TODO: make the minimum circle size a setting
        clipping.addCircle(20 * density, getHeight() - 20 * density, currentRadius * density, Path.Direction.CW);
        canvas.clipPath(clipping);
        for (int i = 0; i < getChildCount(); i++) {
            canvas.save();
            canvas.translate(getChildAt(i).getX(), getChildAt(i).getY());
            getChildAt(i).draw(canvas);
            canvas.restore();
        }
        Region all = new Region(0, 0, getWidth(), getHeight());
        canvas.clipRegion(all);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
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
            //corner = a.getInt(R.styleable.RoundMenuLP_corner, corner);
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
