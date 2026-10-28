package com.example.myapplication1.data.local;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.myapplication1.data.AnswerRecord;
import com.example.myapplication1.data.MomentFeedback;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionProgress;
import com.example.myapplication1.data.Subject;

import java.time.LocalDate;

/**
 * Table and column names + the conversion "Java object <-> database row".
 * Used by RewinderDbHelper (to create the tables) and by the Sqlite* classes (to read/write).
 * Package-private: nobody outside data/local/ needs to know how rows look.
 */
final class Rows {

    private Rows() {
    }

    // ---- table names ----
    static final String SUBJECTS = "subjects";
    static final String QUESTIONS = "questions";
    static final String ANSWERS = "answers";
    static final String FEEDBACK = "moment_feedback";
    static final String PROGRESS = "question_progress";

    // ---------------------------------------------------------------- Subject

    static ContentValues values(Subject s) {
        ContentValues cv = new ContentValues();
        cv.put("id", s.getId());
        cv.put("name", s.getName());
        cv.put("topic", s.getTopic());
        // LocalDate <-> text "2026-11-12"; null stays null
        cv.put("exam_date", s.getExamDate() == null ? null : s.getExamDate().toString());
        return cv;
    }

    static Subject subject(Cursor c) {
        String exam = c.getString(c.getColumnIndexOrThrow("exam_date"));
        return new Subject(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("topic")),
                exam == null ? null : LocalDate.parse(exam));
    }

    // ---------------------------------------------------------------- Question

    static ContentValues values(Question q) {
        ContentValues cv = new ContentValues();
        cv.put("id", q.getId());
        cv.put("subject_id", q.getSubjectId());
        cv.put("text", q.getText());
        cv.put("option1", q.getOption(1));
        cv.put("option2", q.getOption(2));
        cv.put("option3", q.getOption(3));          // null for true/false questions
        cv.put("correct_option", q.getCorrectOption());
        cv.put("explanation", q.getExplanation());   // may be null
        return cv;
    }

    static Question question(Cursor c) {
        return new Question(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("subject_id")),
                c.getString(c.getColumnIndexOrThrow("text")),
                c.getString(c.getColumnIndexOrThrow("option1")),
                c.getString(c.getColumnIndexOrThrow("option2")),
                c.getString(c.getColumnIndexOrThrow("option3")),   // getString returns null for NULL
                c.getInt(c.getColumnIndexOrThrow("correct_option")),
                c.getString(c.getColumnIndexOrThrow("explanation")));
    }

    // ---------------------------------------------------------------- AnswerRecord

    static ContentValues values(AnswerRecord a) {
        ContentValues cv = new ContentValues();
        cv.put("question_id", a.getQuestionId());
        cv.put("subject_id", a.getSubjectId());
        cv.put("timestamp", a.getTimestampMillis());
        cv.put("chosen_option", a.getChosenOption());
        cv.put("correct", a.isCorrect() ? 1 : 0);    // boolean stored as 0/1
        return cv;
    }

    static AnswerRecord answer(Cursor c) {
        return new AnswerRecord(
                c.getLong(c.getColumnIndexOrThrow("question_id")),
                c.getLong(c.getColumnIndexOrThrow("subject_id")),
                c.getLong(c.getColumnIndexOrThrow("timestamp")),
                c.getInt(c.getColumnIndexOrThrow("chosen_option")),
                c.getInt(c.getColumnIndexOrThrow("correct")) == 1);
    }

    // ---------------------------------------------------------------- MomentFeedback

    static ContentValues values(MomentFeedback f) {
        ContentValues cv = new ContentValues();
        cv.put("timestamp", f.getTimestampMillis());
        cv.put("good", f.isGood() ? 1 : 0);
        return cv;
    }

    static MomentFeedback feedback(Cursor c) {
        return new MomentFeedback(
                c.getLong(c.getColumnIndexOrThrow("timestamp")),
                c.getInt(c.getColumnIndexOrThrow("good")) == 1);
    }

    // ---------------------------------------------------------------- QuestionProgress

    static ContentValues values(QuestionProgress p) {
        ContentValues cv = new ContentValues();
        cv.put("question_id", p.getQuestionId());
        cv.put("box", p.getBox());
        cv.put("last_shown_at", p.getLastShownAt());
        cv.put("last_answered_at", p.getLastAnsweredAt());
        cv.put("times_correct", p.getTimesCorrect());
        cv.put("times_wrong", p.getTimesWrong());
        return cv;
    }

    static QuestionProgress progress(Cursor c) {
        return new QuestionProgress(
                c.getLong(c.getColumnIndexOrThrow("question_id")),
                c.getInt(c.getColumnIndexOrThrow("box")),
                c.getLong(c.getColumnIndexOrThrow("last_shown_at")),
                c.getLong(c.getColumnIndexOrThrow("last_answered_at")),
                c.getInt(c.getColumnIndexOrThrow("times_correct")),
                c.getInt(c.getColumnIndexOrThrow("times_wrong")));
    }
}
