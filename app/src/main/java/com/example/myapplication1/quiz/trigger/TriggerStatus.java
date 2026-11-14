package com.example.myapplication1.quiz.trigger;

/**
 * A read-only snapshot of what BreakDetector currently sees, for the "De ce acum?" screen.
 * BreakDetector writes it; screens only read it. Not used by the trigger logic itself.
 */
public final class TriggerStatus {

    private TriggerStatus() {
    }

    public enum Motion {
        /** Detector not running (no permission, no step sensor, service stopped). */
        UNKNOWN,
        /** Walked recently (or just started): waiting for a minute without steps. */
        WALKING,
        /** Still, but not yet for a full minute (just after the app started). */
        STILL_WAITING,
        /** A break is on: questions may be sent. */
        BREAK,
        /** Monitoring is suspended on purpose (busy calendar or quiet hours); it resumes by itself. */
        PAUSED
    }

    public static volatile Motion motion = Motion.UNKNOWN;

    /** SystemClock.elapsedRealtime() when the current break started (0 if none). */
    public static volatile long breakStartedAt = 0;

    /** Which question of the current set was sent last (1..QUESTIONS_PER_SET), 0 = none. */
    public static volatile int questionsInSet = 0;
}
