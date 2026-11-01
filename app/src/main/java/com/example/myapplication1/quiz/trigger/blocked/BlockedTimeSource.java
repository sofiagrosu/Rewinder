package com.example.myapplication1.quiz.trigger.blocked;

import java.util.List;

/**
 * One source of times when NO question may be sent (quiet hours, calendar, ...).
 * Returns intervals rather than a yes/no, so the same data can also answer
 * "when is the next free moment?" (e.g. for the Home screen).
 * Every source is optional: when it is turned off it simply returns an empty list.
 */
public interface BlockedTimeSource {

    /** All blocked intervals that overlap [from, to) (epoch millis). */
    List<TimeRange> blockedBetween(long from, long to);
}
