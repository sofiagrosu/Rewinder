package com.example.myapplication1.quiz.ordering.rules;

import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;
import com.example.myapplication1.quiz.ordering.ScoringRule;

/**
 * Default rule: the longer a question has been waiting, the higher its score.
 * A question that was just shown gets the lowest score, so it "goes to the end of the queue",
 * whatever the answer was. Questions never shown come first.
 */
public class LeastRecentlyShownRule implements ScoringRule {

    @Override
    public double score(Question question, QuestionProgress progress, long now) {
        long waitingMillis = now - progress.getLastShownAt();   // never shown: lastShownAt = 0
        return waitingMillis / 60000.0;                          // minutes waited
    }
}
