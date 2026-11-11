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
            detector.start(this.context, new MomentListener() {
                @Override
                public boolean onMomentDetected(String reason) {
                    return onDetectorSignal(reason);
                }

                @Override
                public void onWalkingStarted() {
                    listener.onWalkingStarted();   // not a "moment": no condition can veto it
                }
            });
        }
    }

    /** Passes "the question was answered" on to the detectors (spacing counts from the answer). */
    public void onQuestionAnswered() {
        if (!running) return;
        for (MomentDetector detector : detectors) {
            detector.onQuestionAnswered();
        }
    }

    /** Passes "bad moment, try later" on to the detectors. */
    public void postponeCycle() {
        for (MomentDetector detector : detectors) {
            detector.postponeCycle();
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        for (MomentDetector detector : detectors) {
            detector.stop();
        }
    }

    /**
     * Called by any detector. Passes the moment on only if every condition agrees.
     * @return true if a question was really sent
     */
    private boolean onDetectorSignal(String reason) {
        long now = System.currentTimeMillis();
        for (MomentCondition condition : conditions) {
            if (!condition.allowsNow(context, now)) return false;   // vetoed
        }
        return listener.onMomentDetected(reason);
    }
}
