package com.mylikes.likes.etchasketch;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by nick on 12/4/14.
 */
public class RoundMenu extends ViewGroup {

    public static final String TAG = "RoundMenu";
    protected float minAlpha = 0.75f;
    protected int touchChildRadius = 50;
    protected int minRadius = 50;
    protected float radsPerChild;
    protected float density;
    protected int currentRadius, toolRadius = 100;
    protected Bitmap image;
    protected final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected double startAngle = 0;
    protected Animator currentAnimation;
    protected boolean expanded;
    protected Point origin;
    public static int CORNER_TOPLEFT = 0;
    public static int CORNER_TOPRIGHT = 1;
    public static int CORNER_BOTTOMLEFT = 2;
    public static int CORNER_BOTTOMRIGHT = 3;
    public int corner = CORNER_BOTTOMLEFT;
    protected float animatedFraction;
    protected int maxRadius = 120;

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
        corner = a.getInt(R.styleable.RoundMenu_corner, corner);
        init();
    }

    protected void init() {
        setWillNotDraw(false);
        density = getResources().getDisplayMetrics().density;
        touchChildRadius = 80;

        setOnTouchListener(new OnTouchListener() {
            @SuppressLint("NewApi")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                RoundSubMenu expandedChild = null;
                boolean result = false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (Math.pow(event.getX() - origin.x, 2) + Math.pow(event.getY() - origin.y, 2) > Math.pow(currentRadius *density, 2)) {
                        return false;
                    }
                    if (currentAnimation != null) {
                        currentAnimation.cancel();
                    }
                    expanded = true;
                    ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
                    animation.setDuration(200);
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            animatedFraction = animation.getAnimatedFraction();
                            currentRadius = (int) (minRadius + maxRadius * animation.getAnimatedFraction());
                            setAlpha(minAlpha + animation.getAnimatedFraction() / 2.0f);
                            invalidate();
                        }
                    });
                    animation.addListener(animationEndListener());
                    animation.start();
                    currentAnimation = animation;
                    result = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP && expanded) {
                    expanded = false;
                    if (currentAnimation != null) {
                        // TODO: if they tap quickly, just let the animation finish, and we'll close when they touch outside
                        currentAnimation.cancel();
                    }
                    ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
                    animation.setDuration(200);
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            animatedFraction = animation.getAnimatedFraction();
                            currentRadius = (int) (minRadius + maxRadius * (1.0f - animation.getAnimatedFraction()));
                            setAlpha(1.0f - animation.getAnimatedFraction() / 2.0f);
                            invalidate();
                        }
                    });
                    animation.addListener(animationEndListener());
                    animation.start();
                    currentAnimation = animation;
                    double x = event.getX() - origin.x, y = event.getY() - origin.y;
                    if (x*x + y*y < (minRadius * density) * (minRadius * density)) {
                        // do nothing; touching origin circle
                    } else {
                        expandedChild = touchChild(x, y);
                        if (expandedChild != null) {
                            expandedChild.callOnClick();
                            expandedChild = null;
                        }
                        //Log.d(TAG, "Angle: " + Math.round(Math.toDegrees(angle)) + " startAngle: " + Math.round(Math.toDegrees(startAngle)) + " per child: " + Math.round(Math.toDegrees(radsPerChild)) + " child:" + child);
                    }
                    result = true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && expanded && currentAnimation == null) {
                    double x = event.getX() - origin.x, y = event.getY() - origin.y;
                    if (x*x + y*y < (touchChildRadius * density) * ((touchChildRadius) * density)) {
                        // do nothing; touching origin circle
                    } else {
                        expandedChild = touchChild(x, y);

                        //Log.d(TAG, "Angle: " + Math.round(Math.toDegrees(angle)) + " startAngle: " + Math.round(Math.toDegrees(startAngle)) + " per child: " + Math.round(Math.toDegrees(radsPerChild)) + " child:" + child);
                    }
                    result = true;
                }
                for (int i = 0; i < getChildCount(); i++) {
                    View view = getChildAt(i);
                    if (view instanceof RoundSubMenu) {
                        RoundSubMenu subMenu = (RoundSubMenu)view;
                        if (subMenu.isExpanded() && subMenu != expandedChild) {
                            subMenu.collapse();
                        }
                    }
                }

                return result;
            }
        });

        currentRadius = minRadius;
        setAlpha(minAlpha);
        // TODO: this depends on corner
        startAngle = Math.PI * 3 / 2;
    }

    public void setImage(Bitmap image) {
        this.image = image;
        invalidate();
    }

    protected RoundSubMenu touchChild(double x, double y) {
        RoundSubMenu expandedChild = null;
        // TODO: adjust this math for each corner
        double angle = Math.atan2(y, x) - startAngle;
        while (angle < 0) angle += Math.PI * 2;
        int child = (int) ((angle) / radsPerChild);
        if (child >= 0 && child < getChildCount()) {
            View view = getChildAt(child);
            if (view instanceof RoundSubMenu) {
                expandedChild = (RoundSubMenu)view;
                if (!expandedChild.isExpanded()) {
                    expandedChild.setAngle(radsPerChild * child + radsPerChild / 2);
                    expandedChild.expand();
                } else {
                    expandedChild.touchChild(x, y);
                }
            }
        }
        return expandedChild;
    }

    protected Animator.AnimatorListener animationEndListener() {
        return new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimation = null;
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimation = null;
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
        if (corner == CORNER_BOTTOMLEFT) {
            origin = new Point(0, getHeight());
        } else if (corner == CORNER_BOTTOMRIGHT) {
            origin = new Point(getWidth(), getHeight());
        }
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
                int centerX = (int) (Math.cos(angle) * toolRadius * density + origin.x);
                int centerY = (int) (Math.sin(angle) * toolRadius * density + origin.y);
                Log.d(TAG, "position with angle " + angle + " at " + centerX + ", " + centerY);
                child.layout(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2);
            }
        }
    }

    @SuppressLint("WrongCall")
    @Override
    public void draw(Canvas canvas) {
        onDraw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO: adjust based on corner
        if (currentAnimation != null || expanded) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child instanceof RoundSubMenu) {
                    canvas.save();
                    double angle = radsPerChild * i + radsPerChild / 2.0f + startAngle;
                    int centerX = (int) (Math.cos(angle) * (currentRadius - 20) * density + origin.x);
                    int centerY = (int) (Math.sin(angle) * (currentRadius - 20) * density + origin.y);
                    canvas.translate(centerX - child.getWidth() / 2, centerY - child.getHeight() / 2);
                    ((RoundSubMenu)child).drawChildren(canvas);
                    canvas.restore();
                }
            }
        }
        canvas.drawCircle(origin.x, origin.y, currentRadius * density, background);
        if (currentAnimation != null || expanded) {
            for (int i = 0; i < getChildCount(); i++) {
                canvas.save();
                View child = getChildAt(i);
                double angle = radsPerChild * i + radsPerChild / 2.0f + startAngle;
                int centerX = (int) (Math.cos(angle) * (currentRadius - 20) * density + origin.x);
                int centerY = (int) (Math.sin(angle) * (currentRadius - 20) * density + origin.y);
                canvas.translate(centerX - child.getWidth() / 2, centerY - child.getHeight() / 2);
                child.draw(canvas);
                canvas.restore();
            }
        }
        canvas.drawCircle(origin.x, origin.y, minRadius * density, background);
        if (!expanded) {
            canvas.drawBitmap(image, origin.x + 5 * density, origin.y - image.getHeight() - 5 * density, background);
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         */
        public int gravity = Gravity.TOP | Gravity.START;



        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            // Pull the layout param values from the layout XML during
            // inflation.  This is not needed if you don't care about
            // changing the layout behavior in XML.
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.RoundMenuLP);
            gravity = a.getInt(R.styleable.RoundMenuLP_android_layout_gravity, gravity);
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
