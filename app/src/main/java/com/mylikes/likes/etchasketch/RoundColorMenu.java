package com.mylikes.likes.etchasketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by nick on 12/6/14.
 */
public class RoundColorMenu extends RoundMenu {

    public static final String TAG = "RoundColorMenu";
    private Paint wedgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RoundColorMenu(Context context) {
        super(context);
    }

    public RoundColorMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundColorMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    static String[] colors = {
            "#A8C0A8", "#489078", "#784800", "#D86048", "#903060", "#CED9DF",
            "#FF9182", "#FFA75D", "#4BCAD1", "#95B138", "#18252D", "#767D5E",
            "#EBF55B", "#FFC94D", "#28A14C", "#315668", "#B44868", "#505A26",
            "#C0AC8B", "#C71427", "#9C284E", "#61FFEA", "#F55E47", "#BEF547",
            "#000000", "#333333", "#666666", "#999999", "#CCCCCC", "#FFFFFF",
    };

    private class Wedge {
        int color;
        float startAngle, sweepAngle;
        int radius;

        public Wedge(int color, float startAngle, float sweepAngle, int radius) {
            this.color = color;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.radius = radius;
        }

        public void draw(Canvas canvas) {

        }
    }

    private ArrayList<Wedge> wedges;

    @Override
    protected void init() {
        wedges = new ArrayList<Wedge>();
        minRadius = 50;
        maxRadius = 160;
        for (int i = 0; i < colors.length; i++) {
            int color = Color.parseColor(colors[i]);
            float startAngle = 0, sweepAngle = 0;
            int radius = 40;
            if (i < 4) {
                sweepAngle = (float) (Math.PI / 2 / 4);
                startAngle = (float)(i * sweepAngle);
                radius = 80;
            } else if (i < 10) {
                sweepAngle = (float) (Math.PI / 2 / 6);
                startAngle = (float)((i - 4) * sweepAngle);
                radius = 120;
            } else if (i < 18) {
                sweepAngle = (float) (Math.PI / 2 / 8);
                startAngle = (float)((i - 10) * sweepAngle);
                radius = 160;
            } else if (i < 30) {
                sweepAngle = (float) (Math.PI / 2 / 12);
                startAngle = (float)((i - 18) * sweepAngle);
                radius = 200;
            } else {
                continue;
            }
            wedges.add(0, new Wedge(color, startAngle, sweepAngle, radius));
        }
        corner = CORNER_BOTTOMRIGHT;
        super.init();
    }

    @Override
    protected RoundSubMenu touchChild(double x, double y) {
        // TODO: Highlight the color they're touching and change the current color
        float radius = (float)Math.sqrt(x*x + y*y) / density;
        float angle = (float) Math.atan2(y, x);
        if (angle < 0) angle += (float)(Math.PI * 2.0f);
        angle -= Math.PI;
        // TODO: this could be done without a loop
        Wedge chosenWedge = null;
        for (Wedge wedge: wedges) {
            if (wedge.startAngle < angle && wedge.startAngle + wedge.sweepAngle > angle && wedge.radius > radius) {
                chosenWedge = wedge;
            }
        }
        //Log.d(TAG, "radius:" + radius + " angle: " + (int)Math.toDegrees(angle));
        if (chosenWedge != null) {
            if (background.getColor() != chosenWedge.color) {
                background.setColor(chosenWedge.color);
                ((MarkersActivity) getContext()).setPenColor(chosenWedge.color);
                invalidate();
            }
        }
        return null;
    }

    public int getColor() {
        return background.getColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO: adjust based on corner
        if (currentAnimation != null || expanded) {
            /*
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
            */
        }
        canvas.drawCircle(origin.x, origin.y, currentRadius * density, background);
        if (currentAnimation != null || expanded) {
            for (Wedge wedge : wedges) {
                wedgePaint.setColor(wedge.color);
                float tmpRadius = wedge.radius * density;
                if (currentAnimation != null) {
                    tmpRadius *= expanded ? animatedFraction : (1.0f - animatedFraction);
                }
                RectF r = new RectF(origin.x - tmpRadius, origin.y - tmpRadius,
                        origin.x + tmpRadius, origin.y + tmpRadius);
                canvas.drawArc(r, (float) Math.toDegrees(wedge.startAngle + Math.PI), (float) Math.toDegrees(wedge.sweepAngle), true, wedgePaint);
            }
        }
        canvas.drawCircle(origin.x, origin.y, minRadius * density, background);
    }
}