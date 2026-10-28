package com.example.myapplication1.data.local;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication1.data.AnswerRecord;
import com.example.myapplication1.data.MomentFeedback;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionRepository;
import com.example.myapplication1.data.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * The real implementation of QuestionRepository: everything is saved in the local database,
 * so it is still there after the app is closed or killed by Android.
 * It does exactly what InMemoryQuestionRepository did (same methods, same behaviour),
 * so no screen has to change.
 *
 * Note: reads and writes happen on the calling thread. With this little data that is
 * fast enough for the screens and for the notification receiver.
 */
public class SqliteQuestionRepository implements QuestionRepository {

    private final RewinderDbHelper helper;

    // Same idea as InMemoryQuestionRepository: nextXId() gives a NEW number every call,
    // even if the previous one was not saved yet.
    private long lastSubjectId;
    private long lastQuestionId;

    public SqliteQuestionRepository(RewinderDbHelper helper) {
        this.helper = helper;
        this.lastSubjectId = maxId(Rows.SUBJECTS);
        this.lastQuestionId = maxId(Rows.QUESTIONS);
    }

    private SQLiteDatabase db() {
        return helper.getWritableDatabase();
    }

    private long maxId(String table) {
        try (Cursor c = db().rawQuery("SELECT COALESCE(MAX(id), 0) FROM " + table, null)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    // ---------------- Subjects ----------------

    @Override
    public synchronized List<Subject> getSubjects() {
        List<Subject> result = new ArrayList<>();
        try (Cursor c = db().query(Rows.SUBJECTS, null, null, null, null, null, "id ASC")) {
            while (c.moveToNext()) result.add(Rows.subject(c));
        }
        return result;
    }

    @Override
    public synchronized Subject getSubject(long subjectId) {
        try (Cursor c = db().query(Rows.SUBJECTS, null, "id = ?",
                new String[]{String.valueOf(subjectId)}, null, null, null)) {
            return c.moveToFirst() ? Rows.subject(c) : null;
        }
    }

    @Override
    public synchronized long nextSubjectId() {
        return ++lastSubjectId;
    }

    @Override
    public synchronized void saveSubject(Subject subject) {
        db().insertWithOnConflict(Rows.SUBJECTS, null, Rows.values(subject),
                SQLiteDatabase.CONFLICT_REPLACE);   // insert, or replace if the id exists
        if (subject.getId() > lastSubjectId) lastSubjectId = subject.getId();
    }

    // ---------------- Questions ----------------

    @Override
    public synchronized List<Question> getAllQuestions() {
        List<Question> result = new ArrayList<>();
        try (Cursor c = db().query(Rows.QUESTIONS, null, null, null, null, null, "id ASC")) {
            while (c.moveToNext()) result.add(Rows.question(c));
        }
        return result;
    }

    @Override
    public synchronized List<Question> getQuestionsForSubject(long subjectId) {
        List<Question> result = new ArrayList<>();
        try (Cursor c = db().query(Rows.QUESTIONS, null, "subject_id = ?",
                new String[]{String.valueOf(subjectId)}, null, null, "id ASC")) {
            while (c.moveToNext()) result.add(Rows.question(c));
        }
        return result;
    }

    @Override
    public synchronized Question getQuestion(long questionId) {
        try (Cursor c = db().query(Rows.QUESTIONS, null, "id = ?",
                new String[]{String.valueOf(questionId)}, null, null, null)) {
            return c.moveToFirst() ? Rows.question(c) : null;
        }
    }

    @Override
    public synchronized long nextQuestionId() {
        return ++lastQuestionId;
    }

    @Override
    public synchronized void saveQuestion(Question question) {
        db().insertWithOnConflict(Rows.QUESTIONS, null, Rows.values(question),
                SQLiteDatabase.CONFLICT_REPLACE);
        if (question.getId() > lastQuestionId) lastQuestionId = question.getId();
    }

    @Override
    public synchronized void deleteQuestion(long questionId) {
        String[] arg = {String.valueOf(questionId)};
        db().delete(Rows.QUESTIONS, "id = ?", arg);
        // its Leitner progress has no meaning any more; the old ANSWERS stay (statistics)
        db().delete(Rows.PROGRESS, "question_id = ?", arg);
    }

    // ---------------- History ----------------

    @Override
    public synchronized void saveAnswer(AnswerRecord answer) {
        db().insertOrThrow(Rows.ANSWERS, null, Rows.values(answer));
    }

    @Override
    public synchronized List<AnswerRecord> getAnswersSince(long sinceMillis) {
        List<AnswerRecord> result = new ArrayList<>();
        try (Cursor c = db().query(Rows.ANSWERS, null, "timestamp >= ?",
                new String[]{String.valueOf(sinceMillis)}, null, null, "timestamp ASC")) {
            while (c.moveToNext()) result.add(Rows.answer(c));
        }
        return result;
    }

    @Override
    public synchronized void saveMomentFeedback(MomentFeedback feedback) {
        db().insertOrThrow(Rows.FEEDBACK, null, Rows.values(feedback));
    }

    @Override
    public synchronized List<MomentFeedback> getMomentFeedbackSince(long sinceMillis) {
        List<MomentFeedback> result = new ArrayList<>();
        try (Cursor c = db().query(Rows.FEEDBACK, null, "timestamp >= ?",
                new String[]{String.valueOf(sinceMillis)}, null, null, "timestamp ASC")) {
            while (c.moveToNext()) result.add(Rows.feedback(c));
        }
        return result;
    }
}
