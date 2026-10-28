package com.example.myapplication1.quiz.ordering.rules;

import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;
import com.example.myapplication1.quiz.ordering.ScoringRule;

public class WrongAnswerPriorityRule implements ScoringRule {

    @Override
    public double score(Question question, QuestionProgress progress, long now){
        if (progress.isNew()){
            return 1;
            // questions never asked have the highest priority
        }
        else {
            // calculate the time since the question was last answered
            long elapsedMillis = now - progress.getLastAnsweredAt();
            long waitingMinutes = elapsedMillis / 60_000;
            // in order to normalize the value of the calculated waiting Minutes,
            // we choose 60 minutes as the refference "middle" value T
            final int T = 60;
            double  timeScore = waitingMinutes/ (double)(waitingMinutes + T);
            // the rate with which the question was answered wrong is normalised
            // between 0 (was always answered correct) and 1 (always answered wrong)
            long wrong = progress.getTimesWrong(); long right = progress.getTimesCorrect();
            double wrongRate = (double) wrong/ (right+(wrong+1));

            // the time since the question was last answered weights more (70%) than
            //the number of times the answer was wrong(30%)
        return 0.7* timeScore + 0.3 * wrongRate;
        }
    }
}
