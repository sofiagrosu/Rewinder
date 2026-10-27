package com.example.myapplication1.data;

import java.util.List;

/**
 * THE single place the whole app reads/writes data through.
 * Nobody else should keep their own lists of questions or answers.
 *
 * Today it is implemented by InMemoryQuestionRepository (hard-coded data).
 * When the database exists, write a RoomQuestionRepository that implements this
 * interface and change ONE line in RepositoryProvider. Nothing else changes.
 *
 * Get the instance with: RepositoryProvider.get()
 */
public interface QuestionRepository {

    // ---------------- Subjects ----------------

    List<Subject> getSubjects();

    /** @return the subject, or null if there is none with this id. */
    Subject getSubject(long subjectId);

    /** A fresh id, not used by any other subject. */
    long nextSubjectId();

    /** Insert, or replace if a subject with the same id already exists. */
    void saveSubject(Subject subject);

    // ---------------- Questions ----------------

    List<Question> getAllQuestions();

    List<Question> getQuestionsForSubject(long subjectId);

    /** @return the question, or null if there is none with this id. */
    Question getQuestion(long questionId);

    /** A fresh id, not used by any other question. */
    long nextQuestionId();

    /** Insert, or replace if a question with the same id already exists (used when editing). */
    void saveQuestion(Question question);

    void deleteQuestion(long questionId);

    // ---------------- History (what Home and Stats are built from) ----------------

    void saveAnswer(AnswerRecord answer);

    /** All answers with timestamp >= sinceMillis. Use 0 for everything. */
    List<AnswerRecord> getAnswersSince(long sinceMillis);

    void saveMomentFeedback(MomentFeedback feedback);

    /** All feedback with timestamp >= sinceMillis. Use 0 for everything. */
    List<MomentFeedback> getMomentFeedbackSince(long sinceMillis);
}

