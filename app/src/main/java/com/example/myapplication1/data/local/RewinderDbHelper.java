package com.example.myapplication1.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.myapplication1.data.AnswerRecord;
import com.example.myapplication1.data.MomentFeedback;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.SampleData;
import com.example.myapplication1.data.Subject;

import java.time.ZoneId;
import java.util.List;

/**
 * Creates and opens the local database file "rewinder.db" (stored on the phone, survives
 * when Android kills the app). Uses plain SQLite from the Android SDK: no extra library,
 * no change in build.gradle.
 *
 * Tables (see Rows for the columns):
 *   subjects, questions, answers (history), moment_feedback, question_progress (Leitner box etc.)
 *
 * The first time the app runs, the database is filled with SampleData
 * (4 subjects, 32 questions) so that every screen has something to show.
 */
public final class RewinderDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "rewinder.db";

    /**
     * Raise this number when you change the tables below. Then onUpgrade() runs.
     * For now onUpgrade simply deletes everything and starts again (fine while developing).
     */
    private static final int DB_VERSION = 1;

    /**
     * true  = on first launch, also insert a made-up answer history for the last 7 days,
     *         so Home and Stats look alive (good for the lab presentation).
     * false = start with an EMPTY history (real use: stats start from zero).
     * Only matters when the database file is created. To apply a change: uninstall the app
     * (or Settings > Apps > Rewinder > Storage > Clear data) and run again.
     */
    public static final boolean SEED_DEMO_HISTORY = true;

    private static RewinderDbHelper instance;

    /** One single helper for the whole app (SQLite wants one connection owner). */
    public static synchronized RewinderDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new RewinderDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private RewinderDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Rows.SUBJECTS + " ("
                + "id INTEGER PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "topic TEXT, "
                + "exam_date TEXT)");                       // "2026-11-12" or NULL

        db.execSQL("CREATE TABLE " + Rows.QUESTIONS + " ("
                + "id INTEGER PRIMARY KEY, "
                + "subject_id INTEGER NOT NULL, "
                + "text TEXT NOT NULL, "
                + "option1 TEXT NOT NULL, "
                + "option2 TEXT NOT NULL, "
                + "option3 TEXT, "                          // NULL for true/false
                + "correct_option INTEGER NOT NULL, "
                + "explanation TEXT)");
        db.execSQL("CREATE INDEX idx_questions_subject ON " + Rows.QUESTIONS + "(subject_id)");

        // History. No link to questions on purpose: if a question is deleted,
        // the old answers stay, so the statistics do not change.
        db.execSQL("CREATE TABLE " + Rows.ANSWERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "question_id INTEGER NOT NULL, "
                + "subject_id INTEGER NOT NULL, "
                + "timestamp INTEGER NOT NULL, "
                + "chosen_option INTEGER NOT NULL, "
                + "correct INTEGER NOT NULL)");             // 0 or 1
        db.execSQL("CREATE INDEX idx_answers_time ON " + Rows.ANSWERS + "(timestamp)");

        db.execSQL("CREATE TABLE " + Rows.FEEDBACK + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "timestamp INTEGER NOT NULL, "
                + "good INTEGER NOT NULL)");                // 0 or 1
        db.execSQL("CREATE INDEX idx_feedback_time ON " + Rows.FEEDBACK + "(timestamp)");

        db.execSQL("CREATE TABLE " + Rows.PROGRESS + " ("
                + "question_id INTEGER PRIMARY KEY, "
                + "box INTEGER NOT NULL, "
                + "last_shown_at INTEGER NOT NULL, "
                + "last_answered_at INTEGER NOT NULL, "
                + "times_correct INTEGER NOT NULL, "
                + "times_wrong INTEGER NOT NULL)");

        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Development shortcut: delete everything and create it again.
        db.execSQL("DROP TABLE IF EXISTS " + Rows.PROGRESS);
        db.execSQL("DROP TABLE IF EXISTS " + Rows.FEEDBACK);
        db.execSQL("DROP TABLE IF EXISTS " + Rows.ANSWERS);
        db.execSQL("DROP TABLE IF EXISTS " + Rows.QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + Rows.SUBJECTS);
        onCreate(db);
    }

    /** Fills the new, empty database. Runs inside the transaction Android opens for onCreate. */
    private void seed(SQLiteDatabase db) {
        List<Question> questions = SampleData.questions();

        for (Subject s : SampleData.subjects()) {
            db.insertOrThrow(Rows.SUBJECTS, null, Rows.values(s));
        }
        for (Question q : questions) {
            db.insertOrThrow(Rows.QUESTIONS, null, Rows.values(q));
        }

        if (SEED_DEMO_HISTORY) {
            long now = System.currentTimeMillis();
            for (AnswerRecord a : SampleData.answerHistory(questions, now, ZoneId.systemDefault())) {
                db.insertOrThrow(Rows.ANSWERS, null, Rows.values(a));
            }
            for (MomentFeedback f : SampleData.momentFeedback(now)) {
                db.insertOrThrow(Rows.FEEDBACK, null, Rows.values(f));
            }
        }
    }
}
