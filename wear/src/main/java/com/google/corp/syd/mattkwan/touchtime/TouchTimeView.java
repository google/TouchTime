/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.corp.syd.mattkwan.touchtime;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/**
 * A view that implements a clock face. It causes the device to vibrate when
 * a touch event intersects with one of the hands.
 * <p/>
 * Created by mattkwan on 7/9/14.
 */
public class TouchTimeView extends View {
    private Vibrator mVibrator;
    private GestureDetector mGestureDetector = null;
    private float mCurrentX = Float.NaN;
    private float mCurrentY = Float.NaN;

    private static final int VIBRATE_PATTERN_NONE = 0;
    private static final int VIBRATE_PATTERN_HOUR = 1;
    private static final int VIBRATE_PATTERN_MINUTE = 2;
    private int mVibratePattern = VIBRATE_PATTERN_NONE;

    public TouchTimeView(Context context) {
        this(context, null);
    }

    public TouchTimeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchTimeView(Context context, AttributeSet attrs,
                         int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mGestureDetector != null && mGestureDetector.onTouchEvent(motionEvent))
                    return true;
                else
                    return processMotionEvent(motionEvent);
            }
        });

        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Set the gesture detector that handles long presses.
     *
     * @param gd The gesture detector that handles long press events.
     */
    public void setGestureDetector(final GestureDetector gd) {
        mGestureDetector = gd;
    }

    /**
     * Get the current time, in minutes since midnight or midday.
     *
     * @return The current time, in minutes since midnight or midday.
     */
    private int
    getCurrentMinutes() {
        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(System.currentTimeMillis());

        int ret = cal.get(Calendar.HOUR_OF_DAY) * 60
                + cal.get(Calendar.MINUTE);

        if (ret >= 720)
            ret -= 720;

        return ret;
    }

    /**
     * Process a touch motion event.
     *
     * @param event The event to process.
     * @return True if the event was consumed.
     */
    private boolean
    processMotionEvent(MotionEvent event) {
        final int action = event.getAction();

        if (action == MotionEvent.ACTION_UP) {
            mCurrentX = Float.NaN;
            mCurrentY = Float.NaN;

            if (mVibratePattern != VIBRATE_PATTERN_NONE) {
                mVibrator.cancel();
                mVibratePattern = VIBRATE_PATTERN_NONE;
            }
            return true;
        }

        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE)
            return false;

        final int minutes = getCurrentMinutes();
        final double hoursAngle = minutes * 2.0 * Math.PI / 720.0;
        final double minutesAngle = (minutes % 60) * 2.0 * Math.PI / 60.0;
        boolean hoursPressed = eventPressesAngle(event, hoursAngle);
        boolean minutesPressed = eventPressesAngle(event, minutesAngle);
        int vibratePattern = VIBRATE_PATTERN_NONE;

        if (hoursPressed)
            vibratePattern |= VIBRATE_PATTERN_HOUR;
        if (minutesPressed)
            vibratePattern |= VIBRATE_PATTERN_MINUTE;

        if (vibratePattern == VIBRATE_PATTERN_NONE
                && action == MotionEvent.ACTION_MOVE) {
            boolean hoursCrossed = eventCrossesAngle(event, hoursAngle);
            boolean minutesCrossed = eventCrossesAngle(event, minutesAngle);

            if (hoursCrossed && minutesCrossed)
                mVibrator.vibrate(new long[]{0, 40, 40, 40, 40, 40}, -1);
            else if (hoursCrossed)
                mVibrator.vibrate(50);
            else if (minutesCrossed)
                mVibrator.vibrate(new long[]{0, 40, 40, 40}, -1);
            if (hoursCrossed || minutesCrossed)
                mVibrator.vibrate(50);
        }

        if (vibratePattern != mVibratePattern) {
            mVibrator.cancel();
            if (vibratePattern == VIBRATE_PATTERN_HOUR) {
                mVibrator.vibrate(new long[]{0, 100}, 0);
            } else if (vibratePattern == VIBRATE_PATTERN_MINUTE) {
                mVibrator.vibrate(new long[]{40, 40}, 0);
            } else if (vibratePattern ==
                    (VIBRATE_PATTERN_HOUR | VIBRATE_PATTERN_MINUTE)) {
                mVibrator.vibrate(new long[]{50, 50, 50, 200}, 0);
            }
            mVibratePattern = vibratePattern;
        }

        mCurrentX = event.getX();
        mCurrentY = event.getY();

        return true;
    }

    /**
     * Does the ending position of the recent motion event touch the
     * line radiating at the specified angle?
     *
     * @param event The motion event.
     * @param angle The angle (clockwise from 12 o'clock) in radians.
     * @return True if the latest motion crosses the line.
     */
    private boolean
    eventPressesAngle(MotionEvent event, double angle) {
        final double cx = 0.5 * getWidth();
        final double cy = 0.5 * getHeight();
        final double touchAngle = Math.atan2(event.getX() - cx, cy - event.getY());

        if (Double.isNaN(touchAngle))
            return false;

        double angDiff = Math.toDegrees(Math.abs(touchAngle - angle));
        if (angDiff > 360.0)
            angDiff -= 360.0;
        if (angDiff > 180.0)
            angDiff = 360.0 - angDiff;

        return angDiff < 6.0;
    }

    /**
     * Does the most recent motion event cross the line radiating at the
     * specified angle?
     *
     * @param event The motion event.
     * @param angle The angle (clockwise from 12 o'clock) in radians.
     * @return True if the latest motion crosses the line.
     */
    private boolean
    eventCrossesAngle(MotionEvent event, double angle) {
        final float lx = 0.5f * getWidth() + (float) Math.sin(angle);
        final float ly = 0.5f * getHeight() - (float) Math.cos(angle);
        float px = mCurrentX;
        float py = mCurrentY;

        for (int i = 0; i < event.getHistorySize(); i++) {
            int dir1 = Float.isNaN(px) ? 0 : directionOfVector(px, py, lx, ly);
            final float nx = event.getHistoricalX(i);
            final float ny = event.getHistoricalY(i);
            int dir2 = directionOfVector(lx, ly, nx, ny);

            if (dir1 == dir2 && dir1 != 0
                    && dir1 == directionOfVector(px, py, nx, ny)) {
                return true;
            }
            px = nx;
            py = ny;
        }

        return false;
    }

    /**
     * Return the rotational direction the vector points in, relative to the
     * centre of the view.
     *
     * @param x1 Vector start X.
     * @param y1 Vector start Y.
     * @param x2 Vector end X.
     * @param y2 Vector end Y.
     * @return 1 if it points clockwise, -1 if anticlockwise, 0 if ambiguous.
     */
    private int
    directionOfVector(float x1, float y1, float x2, float y2) {
        final float cx = 0.5f * getWidth();
        final float cy = 0.5f * getHeight();
        final float crossZ = (x1 - cx) * (y2 - cy) - (x2 - cx) * (y1 - cy);

        if (crossZ < 0.0f)
            return -1;
        else if (crossZ > 0.0f)
            return 1;
        else
            return 0;
    }
}