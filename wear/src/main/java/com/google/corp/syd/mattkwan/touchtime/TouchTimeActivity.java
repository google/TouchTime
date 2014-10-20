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

import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.DismissOverlayView;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class TouchTimeActivity extends Activity {
    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_time);

        final DismissOverlayView dov = (DismissOverlayView) findViewById(R.id.dismiss);
        dov.setIntroText(R.string.long_press_intro);
        dov.showIntroIfNecessary();

        GestureDetector gd = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent ev) {
                dov.show();
            }
        });

        TouchTimeView ttv = (TouchTimeView) findViewById(R.id.touch_time);
        ttv.setGestureDetector (gd);

        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVibrator.vibrate(new long[]{0, 50, 50, 50}, -1);
    }

    @Override
    protected void onStop() {
        mVibrator.vibrate(50);
        super.onStop();
    }
}
