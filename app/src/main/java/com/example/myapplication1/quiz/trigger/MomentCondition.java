package com.example.myapplication1.quiz.trigger;

import android.content.Context;

/**
 * A VETO: decides if a question may be sent right now, whatever the detectors say.
 * Examples (later): "not during a calendar event", "not at night", "not twice in 5 minutes".
 * All conditions must allow it.
 */
public interface MomentCondition {

    /** True if sending a question at time `now` (milliseconds) is allowed. */
    boolean allowsNow(Context context, long now);
}
