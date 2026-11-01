package com.example.myapplication1.data;

/**
 * The few summary numbers other features may show (e.g. the quiz's result notification),
 * without knowing who calculates them.
 *
 * Implemented by stats/StatsCalculator; connected at app start in RewinderApp
 * (StatsProvider.set). Get the numbers with StatsProvider.today().
 */
public interface DailyStats {

    /** Answers given today. */
    int questionsToday();

    /** Percent of correct answers today (0 if no answers). */
    int accuracyToday();

    /** Consecutive days with at least one answer, ending today (or yesterday if none yet today). */
    int streakDays();
}
