package com.example.myapplication1.quiz.trigger;

import android.content.Context;

/**
 * A SIGNAL source: notices something that suggests a free moment
 * (time passed, the user stopped walking, a class just ended...).
 * Detectors don't know where they run (activity or service) or what happens next.
 */
public interface MomentDetector {

    /** Start watching; call listener.onMomentDetected(...) every time a moment is found. */
    void start(Context context, MomentListener listener);

    /** Stop watching and release everything (timers, sensors). Safe to call twice. */
    void stop();
}
