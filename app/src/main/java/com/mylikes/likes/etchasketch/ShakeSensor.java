package com.mylikes.likes.etchasketch;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Date;

/**
 * Created by nick on 11/27/14.
 */
public class ShakeSensor {

    private static final String TAG = "ShakeSensor";
    private SensorManager mSensorManager;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity
    private float lastX, lastY, lastZ;
    private Date lastShake, lastHighAccel;

    private ShakeListener shakeListener;

    public interface ShakeListener {
        public void onShake();
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // perform low-cut filter
            if (mAccel > 12) {
                Date now = new Date();
                Log.d(TAG, "current accel: " + mAccel);
                if (lastHighAccel == null || now.getTime() - lastHighAccel.getTime() > 500) {
                    lastHighAccel = now;
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                } else if (now.getTime() - lastHighAccel.getTime() <= 500
                        && cosBetween(x, y, z, lastX, lastY, lastZ) < 0 // we require two fast jerks in opposite directions to count as a shake
                        && (lastShake == null || (now.getTime() - lastShake.getTime()) > 5000)) { // don't shake one right after the other
                    if (shakeListener != null) shakeListener.onShake();
                    lastShake = now;
                    lastHighAccel = null;
                }
            }
        }

        private float cosBetween(float x1, float y1, float z1, float x2, float y2, float z2) {
            // dot product divided by the product of the magnitudes is cos θ between them
            return (float)((x1*x2 + y1*y2 + z1*z2) / Math.sqrt(x1*x1 + y1*y1 + z1*z1) / Math.sqrt(x2*x2 + y2*y2 + z2*z2));
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public ShakeSensor(Activity activity) {
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    public void setShakeListener(ShakeListener listener) {
        shakeListener = listener;
    }

    // Call from your activity's onPause
    public void unregister() {
        mSensorManager.unregisterListener(mSensorListener);
    }

    // Call from your activity's onResume
    public void register() {
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }
}
