package com.example.myapplication1.quiz.ordering;

import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;

/**
 * Pipeline step 2: gives a question a score. Higher score = shown sooner.
 * The scores of all rules are added together.
 */
public interface ScoringRule {

    /** Score of the question at time `now` (milliseconds). */
    double score(Question question, QuestionProgress progress, long now);
}
