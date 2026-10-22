package com.example.myapplication1.quiz.trigger;

/** Gets told "now is a good moment for a question", and why. */
public interface MomentListener {

    /**
     * @param reason short human-readable explanation, e.g. "You stopped walking".
     *               Can later be shown by the "Why now?" button.
     */
    void onMomentDetected(String reason);
}
