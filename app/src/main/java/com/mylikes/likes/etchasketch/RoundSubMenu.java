package com.mylikes.likes.etchasketch;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by nick on 12/6/14.
 */
public class RoundSubMenu extends View {
    public static final String TAG = "RoundSubMenu";
    public static final int SPACER = 10;
    protected ArrayList<Bitmap> options = new ArrayList<Bitmap>();
    protected ArrayList<String> values = new ArrayList<String>();

    protected Bitmap image;
    protected float angleDgs = 0;
    protected int totalHeight = 0;
    protected float density;

    protected Animator currentAnimation;
    protected boolean expanded;
    protected Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);

    static int[] iconIds = {R.styleable.RoundSubMenu_icon1, R.styleable.RoundSubMenu_icon2, R.styleable.RoundSubMenu_icon3, R.styleable.RoundSubMenu_icon4, R.styleable.RoundSubMenu_icon5};
    static int[] valueIds = {R.styleable.RoundSubMenu_value1, R.styleable.RoundSubMenu_value2, R.styleable.RoundSubMenu_value3, R.styleable.RoundSubMenu_value4, R.styleable.RoundSubMenu_value5};
    private float openPct = 0;

    public RoundSubMenu(Context context) {
        super(context);
        init();
    }

    public RoundSubMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundSubMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RoundSubMenu,
                0, 0);

        totalHeight = (int) ((SPACER + 30) * density); // TODO: where's this 30 coming from??
        for (int i = 0; i < 5; i++) {
            String value = a.getString(valueIds[i]);
            int resourceId = a.getResourceId(iconIds[i], -1);
            if (resourceId != -1 && value != null) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), resourceId);
                addOption(value, bm);
            }
        }
        int icon = a.getResourceId(R.styleable.RoundSubMenu_menuIcon, -1);
        if (icon != -1) {
            image = BitmapFactory.decodeResource(getResources(), icon);
        }
        int backgroundColor = a.getColor(R.styleable.RoundSubMenu_slideColor, -1);
        if (backgroundColor != -1) {
            background.setColor(backgroundColor);
        }
        init();
    }

    public void addOption(String value, Bitmap bm) {
        options.add(bm);
        values.add(value);
        totalHeight += bm.getHeight() + SPACER * density;
        Log.d(TAG, "totalHegiht =" + totalHeight);
    }

    public void setAngle(double angle) {
        this.angleDgs = (float)Math.toDegrees(angle);
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
    }

    public void expand() {
        expanded = true;
        animateOpenness(openPct, 1, (int)Math.max(options.size() * 50 * (1.0f - openPct), 150));
    }
    public void animateOpenness(final float start, final float end, int duration) {
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        ValueAnimator animation = ValueAnimator.ofFloat(start, end);
        animation.setDuration(duration);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //Log.d(TAG, "animate");
                openPct = start + (end - start) * animation.getAnimatedFraction();
                ((RoundMenu)getParent()).invalidate();
            }
        });
        animation.addListener(animationEndListener());
        animation.start();
        currentAnimation = animation;
    }

    public void collapse() {
        expanded = false;
        animateOpenness(openPct, 0, (int)Math.max(options.size() * 50 * openPct, 150));
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int desiredWidth = 100;
        int desiredHeight = 100;
        if (image != null) {
            desiredWidth = image.getWidth();
            desiredHeight = image.getHeight();
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
        Log.d(TAG, "Set size to " + width + " x " + height);
    }

    @SuppressLint("WrongCall")
    @Override
    public void draw(Canvas canvas) {
        // Normally draw() clips the canvas to the rect of the child. We're going to draw way outside the lines here.
        onDraw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // TODO: use openPct

        canvas.drawBitmap(image, 0, 0, background);
    }

    public void drawChildren(Canvas canvas) {
        if (openPct > 0) {
            canvas.save();
            canvas.rotate(angleDgs, getWidth() / 2, getHeight() / 2);
            //Log.d(TAG, "rotate " + Math.round(angleDgs) + "; x=" + getX() + " y=" + getY() + " totalHeight=" + totalHeight);
            //canvas.drawRect(0, -totalHeight, getWidth(), getHeight(), background);
            float padding = 5 * density;
            RectF r = new RectF(-padding, openPct * (-totalHeight) - padding, getWidth() + 2 * padding, totalHeight + 2 * padding);
            canvas.drawRoundRect(r, 5 * density, 5 * density, background);
            float top = (1.0f - openPct) * totalHeight;
            for (int i = 0; i < options.size(); i++) {
                top -= options.get(i).getHeight() + SPACER * density;
                canvas.drawBitmap(options.get(i), 0, top, background);
            }
            Log.d(TAG, "total height: " + totalHeight + " top: " + top);
            canvas.restore();
        }
    }

    public void touchChild(double x, double y) {
        // Figure out which tool they're touching
        // x and y are relative to the origin, so x is positive and y is probably negative
    }

    protected Animator.AnimatorListener animationEndListener() {
        return new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimation = null;
                openPct = expanded ? 1.0f : 0.0f;
                ((RoundMenu)getParent()).invalidate();
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
}
