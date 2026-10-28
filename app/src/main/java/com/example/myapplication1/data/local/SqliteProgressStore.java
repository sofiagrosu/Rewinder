package com.example.myapplication1.data.local;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication1.data.ProgressStore;
import com.example.myapplication1.data.QuestionProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * The real ProgressStore: the Leitner box, last shown / answered time and counters of every
 * question are saved in the database. Replaces InMemoryProgressStore, so the quiz
 * "remembers" what it already asked even after Android kills the app.
 */
public class SqliteProgressStore implements ProgressStore {

    private final RewinderDbHelper helper;

    public SqliteProgressStore(RewinderDbHelper helper) {
        this.helper = helper;
    }

    @Override
    public synchronized QuestionProgress get(long questionId) {
        try (Cursor c = helper.getWritableDatabase().query(Rows.PROGRESS, null, "question_id = ?",
                new String[]{String.valueOf(questionId)}, null, null, null)) {
            if (c.moveToFirst()) return Rows.progress(c);
        }
        return QuestionProgress.newFor(questionId);   // never seen: same rule as the interface says
    }

    @Override
    public synchronized void save(QuestionProgress progress) {
        helper.getWritableDatabase().insertWithOnConflict(Rows.PROGRESS, null,
                Rows.values(progress), SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public synchronized List<QuestionProgress> getAll() {
        List<QuestionProgress> result = new ArrayList<>();
        try (Cursor c = helper.getWritableDatabase().query(Rows.PROGRESS, null, null, null,
                null, null, "question_id ASC")) {
            while (c.moveToNext()) result.add(Rows.progress(c));
        }
        return result;
    }
}
