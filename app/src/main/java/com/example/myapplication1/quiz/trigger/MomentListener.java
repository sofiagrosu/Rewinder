package com.example.myapplication1.quiz.trigger;

/** Gets told "now is a good moment for a question", and why. */
public interface MomentListener {

    /**
     * @param reason short human-readable explanation, e.g. "You stopped walking".
     *               Can later be shown by the "Why now?" button.
     * @return true if a question was really sent; false if the moment was not used
     *         (blocked time, a question still waiting for an answer, no question available).
     *         Detectors that count questions (BreakDetector) use this; others may ignore it.
     */
    boolean onMomentDetected(String reason);
}
