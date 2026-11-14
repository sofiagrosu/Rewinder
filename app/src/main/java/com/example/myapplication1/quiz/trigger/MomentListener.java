package com.example.myapplication1.quiz.trigger;

/** Gets told "now is a good moment for a question", and why. */
public interface MomentListener {

    /**
     * @param reason short human-readable explanation, e.g. "You stopped walking".
     *               Can later be shown by the "Why now?" button.
     * @return true if a question was really sent; false if the moment was not used
     *         (blocked time, a question still waiting for an answer, nothing to ask).
     *         BreakDetector counts only the questions really sent.
     */
    boolean onMomentDetected(String reason);

    /**
     * The user started walking: the cycle of questions is interrupted, so a question that is
     * still on screen is no longer wanted.
     */
    default void onWalkingStarted() {
    }
}
