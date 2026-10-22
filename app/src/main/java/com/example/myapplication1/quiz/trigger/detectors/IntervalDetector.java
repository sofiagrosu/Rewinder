package com.example.myapplication1.quiz.trigger.detectors;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.myapplication1.quiz.trigger.MomentDetector;
import com.example.myapplication1.quiz.trigger.MomentListener;

/**
 * TEST detector: signals a "moment" every N milliseconds.
 * Uses a Handler, so it only works while the app process is alive (fine for testing).
 * To be removed when the real detectors (motion, calendar) exist.
 */
public class IntervalDetector implements MomentDetector {

    private final long intervalMillis;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MomentListener listener;

    // runs every intervalMillis: signal, then schedule itself again
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            listener.onMomentDetected("Test: every " + (intervalMillis / 1000) + " seconds");
            handler.postDelayed(this, intervalMillis);
        }
    };

    public IntervalDetector(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void start(Context context, MomentListener listener) {
        this.listener = listener;
        handler.removeCallbacks(tick);              // never run two timers at once
        handler.postDelayed(tick, intervalMillis);  // first signal after one interval
    }

    @Override
    public void stop() {
        handler.removeCallbacks(tick);
    }
}
