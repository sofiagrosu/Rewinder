package com.example.myapplication1.quiz.ordering;

import com.example.myapplication1.data.ProgressStore;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;
import com.example.myapplication1.quiz.ordering.rules.LeastRecentlyShownRule;
import com.example.myapplication1.quiz.ordering.rules.WrongAnswerPriorityRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
//        rules.add(new LeastRecentlyShownRule());
        rules.add(new WrongAnswerPriorityRule());
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
        List<Question> scoredQuestions = new ArrayList<>();   // for the debug log
        List<Double> scores = new ArrayList<>();
        for (Question question : allowed) {
            double score = totalScore(question, now);
            scoredQuestions.add(question);
            scores.add(score);
            if (score > bestScore) {      // strictly greater: ties keep the earlier question
                best = question;
                bestScore = score;
            }
        }
        logRanking(best, scoredQuestions, scores);
        return best;
    }

    /**
     * DEBUG: one Logcat line per pick, questions sorted from highest to lowest score, e.g.
     *   QUIZ-ORDER picked 3 |  3=0.82  7=0.75  1=0.50
     * Uses System.out (not android.util.Log) so this class stays plain Java and unit-testable.
     * In Logcat, filter with:  package:mine QUIZ-ORDER
     */
    private void logRanking(Question picked, List<Question> questions, List<Double> scores) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) order.add(i);
        order.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));   // highest first

        StringBuilder line = new StringBuilder("QUIZ-ORDER picked ")
                .append(picked == null ? "none" : String.valueOf(picked.getId()))
                .append(" |");
        for (int i : order) {
            line.append("  ")
                .append(questions.get(i).getId())
                .append('=')
                .append(String.format(Locale.US, "%.2f", scores.get(i)));
        }
        System.out.println(line);
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
