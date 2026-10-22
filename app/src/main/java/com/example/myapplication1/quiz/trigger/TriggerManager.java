package com.example.myapplication1.quiz.trigger;

import android.content.Context;

import java.util.List;

/**
 * Runs all detectors. When ANY detector signals a moment, checks that ALL conditions
 * allow it, and only then tells the listener (later: the quiz coordinator).
 *
 *   detectors (any one)  ──reason──▶  conditions (all)  ──▶  listener
 */
public class TriggerManager {

    private final List<MomentDetector> detectors;
    private final List<MomentCondition> conditions;
    private final MomentListener listener;
    private Context context;
    private boolean running = false;

    public TriggerManager(List<MomentDetector> detectors,
                          List<MomentCondition> conditions,
                          MomentListener listener) {
        this.detectors = detectors;
        this.conditions = conditions;
        this.listener = listener;
    }

    public void start(Context context) {
        if (running) return;
        running = true;
        this.context = context.getApplicationContext();
        for (MomentDetector detector : detectors) {
            detector.start(this.context, this::onDetectorSignal);
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        for (MomentDetector detector : detectors) {
            detector.stop();
        }
    }

    /** Called by any detector. Passes the moment on only if every condition agrees. */
    private void onDetectorSignal(String reason) {
        long now = System.currentTimeMillis();
        for (MomentCondition condition : conditions) {
            if (!condition.allowsNow(context, now)) return;   // vetoed
        }
        listener.onMomentDetected(reason);
    }
}
