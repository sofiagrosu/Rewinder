package com.example.myapplication1.quiz.trigger;

/**
 * All the numbers that decide WHEN a "free moment" is detected, in one place.
 * Change them here (e.g. smaller values while testing), nowhere else.
 */
public final class TriggerConfig {

    private TriggerConfig() {
        // only constants
    }

    // ------------------------------------------------------------------
    // BreakDetector: "the user is not walking" → sets of questions
    // ------------------------------------------------------------------

    /** How many steps count as "really walking" (not just a few steps around the room). */
    public static final int MIN_STEPS_TO_COUNT_AS_WALKING = 20;

    /** Those steps must have happened within this time window. */
    public static final long WALKING_WINDOW_MILLIS = 2 * 60_000;      // 2 minutes

    /** No step for this long = a break starts (after walking, or right after the app starts). */
    public static final long STOPPED_FOR_MILLIS = 60_000;             // 1 minute
    // TIP for testing: 15_000 (15 seconds)

    /** During a break, questions come in sets of this many... */
    public static final int QUESTIONS_PER_SET = 3;

    /** ...the next one this long AFTER THE PREVIOUS ONE WAS ANSWERED... */
    public static final long QUESTION_SPACING_MILLIS = 60_000;        // 1 minute

    /** ...and the next set comes this long after the last question of the set was answered. */
    public static final long PAUSE_BETWEEN_SETS_MILLIS = 15 * 60_000; // 15 minutes
    // TIP for testing: 60_000 (1 minute)

    /**
     * When a moment could not be used (quiet hours, busy in the calendar, nothing to ask),
     * the detector tries again after this long.
     */
    public static final long RETRY_AFTER_NOT_SENT_MILLIS = 60_000;    // 1 minute

    /** How often the detector checks the clock. */
    public static final long CHECK_INTERVAL_MILLIS = 5_000;           // 5 seconds

    // ------------------------------------------------------------------
    // Blocked times: when NO question may be sent (BlockedTimesCondition)
    // ------------------------------------------------------------------

    /** Quiet hours are ON by default: we don't send questions while the user sleeps. */
    public static final boolean DEFAULT_QUIET_HOURS_ENABLED = true;
    public static final int DEFAULT_QUIET_START_HOUR = 22;   // 22:00
    public static final int DEFAULT_QUIET_END_HOUR = 8;      // 08:00 (next morning)

    /** Calendar is OFF by default: the user turns it on (and gives the permission) in Settings. */
    public static final boolean DEFAULT_CALENDAR_ENABLED = true;
}
