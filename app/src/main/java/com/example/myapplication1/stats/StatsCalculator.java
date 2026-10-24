package com.example.myapplication1.stats;

import com.example.myapplication1.data.AnswerRecord;
import com.example.myapplication1.data.MomentFeedback;
import com.example.myapplication1.data.QuestionRepository;
import com.example.myapplication1.data.Subject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the raw history (answers, feedback) from the repository into the numbers
 * shown on Home and Stats. No Android classes here, so it is easy to test.
 */
public class StatsCalculator {

    /** Numbers for one subject. accuracyPercent is 0 when answerCount is 0. */
    public static class SubjectStat {
        public final Subject subject;
        public final int questionCount;
        public final int answerCount;
        public final int accuracyPercent;
        public final long lastAnswerAt;   // 0 if never answered

        SubjectStat(Subject subject, int questionCount, int answerCount,
                    int accuracyPercent, long lastAnswerAt) {
            this.subject = subject;
            this.questionCount = questionCount;
            this.answerCount = answerCount;
            this.accuracyPercent = accuracyPercent;
            this.lastAnswerAt = lastAnswerAt;
        }
    }

    private final QuestionRepository repo;
    private final ZoneId zone;
    private final LocalDate today;

    public StatsCalculator(QuestionRepository repo) {
        this(repo, System.currentTimeMillis(), ZoneId.systemDefault());
    }

    public StatsCalculator(QuestionRepository repo, long now, ZoneId zone) {
        this.repo = repo;
        this.zone = zone;
        this.today = dateOf(now);
    }

    // ------------------------------------------------------------------ today

    public int questionsToday() {
        return answersFrom(today).size();
    }

    /** Percent of correct answers today (0 if no answers). */
    public int accuracyToday() {
        return accuracyOf(answersFrom(today));
    }

    /**
     * Consecutive days with at least one answer. If there is no answer yet today,
     * the streak still counts up to yesterday (it only breaks when a full day is missed).
     */
    public int streakDays() {
        Set<LocalDate> days = new HashSet<>();
        for (AnswerRecord a : repo.getAnswersSince(0)) {
            days.add(dateOf(a.getTimestampMillis()));
        }
        LocalDate d = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    // ------------------------------------------------------------------ last N days

    /** Percent of correct answers in the last {@code days} days, today included (0 if none). */
    public int accuracyLastDays(int days) {
        return accuracyOf(answersFrom(today.minusDays(days - 1)));
    }

    /** Percent of "good moment" answers in the last {@code days} days; -1 if there is no feedback. */
    public int goodMomentsPercent(int days) {
        long since = startOf(today.minusDays(days - 1));
        int total = 0;
        int good = 0;
        for (MomentFeedback f : repo.getMomentFeedbackSince(since)) {
            total++;
            if (f.isGood()) good++;
        }
        if (total == 0) return -1;
        return percent(good, total);
    }

    /** The last {@code days} dates, oldest first, today last. */
    public LocalDate[] lastDates(int days) {
        LocalDate[] dates = new LocalDate[days];
        for (int i = 0; i < days; i++) {
            dates[i] = today.minusDays(days - 1 - i);
        }
        return dates;
    }

    /** Number of answers per day, same order as {@link #lastDates(int)}. */
    public int[] questionsPerDay(int days) {
        int[] counts = new int[days];
        LocalDate first = today.minusDays(days - 1);
        for (AnswerRecord a : answersFrom(first)) {
            long index = ChronoUnit.DAYS.between(first, dateOf(a.getTimestampMillis()));
            if (index >= 0 && index < days) counts[(int) index]++;
        }
        return counts;
    }

    // ------------------------------------------------------------------ subjects

    /** One entry per subject (all-time numbers). */
    public List<SubjectStat> subjectStats() {
        Map<Long, int[]> sums = new HashMap<>();   // subjectId -> {answers, correct}
        Map<Long, Long> last = new HashMap<>();
        for (AnswerRecord a : repo.getAnswersSince(0)) {
            int[] s = sums.get(a.getSubjectId());
            if (s == null) {
                s = new int[2];
                sums.put(a.getSubjectId(), s);
            }
            s[0]++;
            if (a.isCorrect()) s[1]++;
            Long prev = last.get(a.getSubjectId());
            if (prev == null || a.getTimestampMillis() > prev) {
                last.put(a.getSubjectId(), a.getTimestampMillis());
            }
        }

        List<SubjectStat> result = new ArrayList<>();
        for (Subject subject : repo.getSubjects()) {
            int[] s = sums.get(subject.getId());
            int answers = s == null ? 0 : s[0];
            int correct = s == null ? 0 : s[1];
            Long lastAt = last.get(subject.getId());
            result.add(new SubjectStat(
                    subject,
                    repo.getQuestionsForSubject(subject.getId()).size(),
                    answers,
                    answers == 0 ? 0 : percent(correct, answers),
                    lastAt == null ? 0 : lastAt));
        }
        return result;
    }

    /** Subjects practised most recently first (the "Materii active" list on Home). */
    public List<SubjectStat> recentSubjects(int limit) {
        List<SubjectStat> list = subjectStats();
        Collections.sort(list, (a, b) -> Long.compare(b.lastAnswerAt, a.lastAnswerAt));
        return list.subList(0, Math.min(limit, list.size()));
    }

    /** Subjects with the lowest accuracy first; subjects never answered are left out. */
    public List<SubjectStat> weakestSubjects(int limit) {
        List<SubjectStat> list = new ArrayList<>();
        for (SubjectStat s : subjectStats()) {
            if (s.answerCount > 0) list.add(s);
        }
        Collections.sort(list, (a, b) -> Integer.compare(a.accuracyPercent, b.accuracyPercent));
        return list.subList(0, Math.min(limit, list.size()));
    }

    // ------------------------------------------------------------------ helpers

    private List<AnswerRecord> answersFrom(LocalDate firstDay) {
        return repo.getAnswersSince(startOf(firstDay));
    }

    private int accuracyOf(List<AnswerRecord> answers) {
        if (answers.isEmpty()) return 0;
        int correct = 0;
        for (AnswerRecord a : answers) {
            if (a.isCorrect()) correct++;
        }
        return percent(correct, answers.size());
    }

    private static int percent(int part, int total) {
        return Math.round(100f * part / total);
    }

    private LocalDate dateOf(long millis) {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
    }

    private long startOf(LocalDate date) {
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }
}

