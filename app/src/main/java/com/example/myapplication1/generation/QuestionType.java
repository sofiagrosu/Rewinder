package com.example.myapplication1.generation;

import com.example.myapplication1.data.Question;

/**
 * The three kinds of questions from the project plan.
 * They all fit the existing Question class (2 or 3 options), so the quiz,
 * the notifications and the statistics need NO change:
 *
 *   TRUE_FALSE   statement, options "True" / "False"                  (2 options)
 *   TWO_OPTIONS  sentence with a gap "____", choose between 2 words       (2 options)
 *   MISSING_WORD sentence with a gap "____", choose between 3 words       (3 options)
 */
public enum QuestionType {
    TRUE_FALSE,
    TWO_OPTIONS,
    MISSING_WORD;

    public static final String TRUE_TEXT = "True";
    public static final String FALSE_TEXT = "False";
    // questions generated before the switch to English (already saved in the database)
    private static final String OLD_TRUE_TEXT = "Adevărat";
    private static final String OLD_FALSE_TEXT = "Fals";

    /** Guesses the type of an already saved question (the database does not store it). */
    public static QuestionType of(Question q) {
        if (q.getOptionCount() == 3) return MISSING_WORD;
        if ((TRUE_TEXT.equals(q.getOption(1)) && FALSE_TEXT.equals(q.getOption(2)))
                || (OLD_TRUE_TEXT.equals(q.getOption(1)) && OLD_FALSE_TEXT.equals(q.getOption(2)))) return TRUE_FALSE;
        return TWO_OPTIONS;
    }
}
