package com.example.myapplication1.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps everything in memory (lost when the app process dies).
 * Good enough until the real database is ready.
 * Methods are synchronized because the quiz / watch / notifications may call from other threads.
 */
public class InMemoryQuestionRepository implements QuestionRepository {

    private final List<Subject> subjects = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final List<AnswerRecord> answers = new ArrayList<>();
    private final List<MomentFeedback> feedback = new ArrayList<>();

    private long lastSubjectId = 0;
    private long lastQuestionId = 0;

    public InMemoryQuestionRepository() {
    }

    public InMemoryQuestionRepository(List<Subject> subjects, List<Question> questions,
                                      List<AnswerRecord> answers, List<MomentFeedback> feedback) {
        for (Subject s : subjects) saveSubject(s);
        for (Question q : questions) saveQuestion(q);
        this.answers.addAll(answers);
        this.feedback.addAll(feedback);
    }

    // ---------------- Subjects ----------------

    @Override
    public synchronized List<Subject> getSubjects() {
        return new ArrayList<>(subjects);
    }

    @Override
    public synchronized Subject getSubject(long subjectId) {
        for (Subject s : subjects) {
            if (s.getId() == subjectId) return s;
        }
        return null;
    }

    @Override
    public synchronized long nextSubjectId() {
        return ++lastSubjectId;
    }

    @Override
    public synchronized void saveSubject(Subject subject) {
        for (int i = 0; i < subjects.size(); i++) {
            if (subjects.get(i).getId() == subject.getId()) {
                subjects.set(i, subject);
                return;
            }
        }
        subjects.add(subject);
        if (subject.getId() > lastSubjectId) lastSubjectId = subject.getId();
    }

    // ---------------- Questions ----------------

    @Override
    public synchronized List<Question> getAllQuestions() {
        return new ArrayList<>(questions);
    }

    @Override
    public synchronized List<Question> getQuestionsForSubject(long subjectId) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions) {
            if (q.getSubjectId() == subjectId) result.add(q);
        }
        return result;
    }

    @Override
    public synchronized Question getQuestion(long questionId) {
        for (Question q : questions) {
            if (q.getId() == questionId) return q;
        }
        return null;
    }

    @Override
    public synchronized long nextQuestionId() {
        return ++lastQuestionId;
    }

    @Override
    public synchronized void saveQuestion(Question question) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId() == question.getId()) {
                questions.set(i, question);
                return;
            }
        }
        questions.add(question);
        if (question.getId() > lastQuestionId) lastQuestionId = question.getId();
    }

    @Override
    public synchronized void deleteQuestion(long questionId) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId() == questionId) {
                questions.remove(i);
                return;
            }
        }
    }

    // ---------------- History ----------------

    @Override
    public synchronized void saveAnswer(AnswerRecord answer) {
        answers.add(answer);
    }

    @Override
    public synchronized List<AnswerRecord> getAnswersSince(long sinceMillis) {
        List<AnswerRecord> result = new ArrayList<>();
        for (AnswerRecord a : answers) {
            if (a.getTimestampMillis() >= sinceMillis) result.add(a);
        }
        return result;
    }

    @Override
    public synchronized void saveMomentFeedback(MomentFeedback f) {
        feedback.add(f);
    }

    @Override
    public synchronized List<MomentFeedback> getMomentFeedbackSince(long sinceMillis) {
        List<MomentFeedback> result = new ArrayList<>();
        for (MomentFeedback f : feedback) {
            if (f.getTimestampMillis() >= sinceMillis) result.add(f);
        }
        return result;
    }
}
