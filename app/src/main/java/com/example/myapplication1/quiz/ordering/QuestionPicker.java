package com.example.myapplication1.quiz.ordering;

import com.example.myapplication1.data.ProgressStore;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;
import com.example.myapplication1.quiz.ordering.rules.LeastRecentlyShownRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the next question, as a pipeline:
 *   1. FILTERS  remove questions that may not be shown now
 *   2. RULES    give every remaining question a score (scores are added)
 *   3. PICK     the question with the highest score
 *
 * Only READS progress. Updating progress after a question is shown/answered
 * is the job of ProgressUpdater.
 */
public class QuestionPicker {

    private final ProgressStore progressStore;
    private final List<QuestionFilter> filters;
    private final List<ScoringRule> rules;

    public QuestionPicker(ProgressStore progressStore,
                          List<QuestionFilter> filters,
                          List<ScoringRule> rules) {
        this.progressStore = progressStore;
        this.filters = filters;
        this.rules = rules;
    }

    /** Default setup for now: no filters, one rule (least recently shown first). */
    public static QuestionPicker withDefaultRules(ProgressStore progressStore) {
        List<QuestionFilter> filters = new ArrayList<>();
        List<ScoringRule> rules = new ArrayList<>();
        rules.add(new LeastRecentlyShownRule());
        return new QuestionPicker(progressStore, filters, rules);
    }

    /**
     * The next question to show at time `now`, or null if there are no candidates.
     * If two questions have the same score, the one that comes first in the list wins.
     */
    public Question pickNext(List<Question> candidates, long now) {
        if (candidates == null || candidates.isEmpty()) return null;

        // 1. filters
        List<Question> allowed = new ArrayList<>();
        for (Question question : candidates) {
            if (passesAllFilters(question, now)) {
                allowed.add(question);
            }
        }
        // if the filters removed everything, better show something than nothing
        if (allowed.isEmpty()) allowed = candidates;

        // 2. + 3. score and keep the best
        Question best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Question question : allowed) {
            double score = totalScore(question, now);
            if (score > bestScore) {      // strictly greater: ties keep the earlier question
                best = question;
                bestScore = score;
            }
        }
        return best;
    }

    private boolean passesAllFilters(Question question, long now) {
        QuestionProgress progress = progressStore.get(question.getId());
        for (QuestionFilter filter : filters) {
            if (!filter.allows(question, progress, now)) return false;
        }
        return true;
    }

    private double totalScore(Question question, long now) {
        QuestionProgress progress = progressStore.get(question.getId());
        double total = 0;
        for (ScoringRule rule : rules) {
            total += rule.score(question, progress, now);
        }
        return total;
    }
}
