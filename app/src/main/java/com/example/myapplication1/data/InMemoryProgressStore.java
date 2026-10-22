package com.example.myapplication1.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps progress only in memory: everything is lost when the app process stops.
 * Good for quick tests; NOT enough for real notifications (use a persistent store).
 */
public class InMemoryProgressStore implements ProgressStore {

    private final Map<Long, QuestionProgress> progressById = new HashMap<>();

    @Override
    public QuestionProgress get(long questionId) {
        QuestionProgress progress = progressById.get(questionId);
        if (progress == null) {
            return QuestionProgress.newFor(questionId);
        }
        return progress;
    }

    @Override
    public void save(QuestionProgress progress) {
        progressById.put(progress.getQuestionId(), progress);
    }

    @Override
    public List<QuestionProgress> getAll() {
        return new ArrayList<>(progressById.values());
    }
}
