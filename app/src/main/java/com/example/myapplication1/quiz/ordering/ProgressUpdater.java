package com.example.myapplication1.quiz.ordering;

import com.example.myapplication1.data.ProgressStore;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;

/**
 * Updates the progress when a question is shown or answered.
 * The only class in ordering that WRITES progress.
 */
public class ProgressUpdater {

    private final ProgressStore progressStore;

    public ProgressUpdater(ProgressStore progressStore) {
        this.progressStore = progressStore;
    }

    /**
     * Call when the question is sent (e.g. as a notification).
     * From this moment it is "at the end of the queue", even if the user never answers.
     */
    public void recordShown(long questionId, long now) {
        QuestionProgress progress = progressStore.get(questionId);
        progress.setLastShownAt(now);
        progressStore.save(progress);
    }

    /** Call when the user answers. */
    public void recordAnswer(Question question, boolean correct, long now) {
        QuestionProgress progress = progressStore.get(question.getId());
        progress.setLastAnsweredAt(now);
        if (correct) {
            progress.setTimesCorrect(progress.getTimesCorrect() + 1);
        } else {
            progress.setTimesWrong(progress.getTimesWrong() + 1);
        }
        // Later (Leitner): correct -> progress.setBox(progress.getBox() + 1)
        //                  wrong   -> progress.setBox(QuestionProgress.MIN_BOX)
        progressStore.save(progress);
    }
}
