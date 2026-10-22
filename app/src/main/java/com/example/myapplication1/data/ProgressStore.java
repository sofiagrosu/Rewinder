package com.example.myapplication1.data;

import java.util.List;

/**
 * Where the user's progress is kept.
 * In memory for quick tests, SharedPreferences next, local database (Room) later.
 */
public interface ProgressStore {

    /**
     * Progress for one question. Never null: if the question was never seen,
     * returns QuestionProgress.newFor(questionId) (not saved until save() is called).
     */
    QuestionProgress get(long questionId);

    /** Saves (creates or replaces) the progress of one question. */
    void save(QuestionProgress progress);

    /** All saved progress entries (for statistics). */
    List<QuestionProgress> getAll();
}
