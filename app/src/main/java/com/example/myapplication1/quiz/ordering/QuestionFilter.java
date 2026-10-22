package com.example.myapplication1.quiz.ordering;

import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;

/**
 * Pipeline step 1: decides if a question is ALLOWED to be shown right now.
 * Example: "not if it was shown in the last 10 minutes".
 */
public interface QuestionFilter {

    /** True if the question may be shown at time `now` (milliseconds). */
    boolean allows(Question question, QuestionProgress progress, long now);
}
