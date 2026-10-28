package com.example.myapplication1.data;

import android.content.Context;

import com.example.myapplication1.data.local.RewinderDbHelper;
import com.example.myapplication1.data.local.SqliteProgressStore;
import com.example.myapplication1.data.local.SqliteQuestionRepository;

/**
 * Gives every screen the same data objects:
 *
 *     QuestionRepository repo = RepositoryProvider.get();            // subjects, questions, history
 *     ProgressStore store = RepositoryProvider.getProgressStore();   // Leitner progress (quiz)
 *
 * init(context) is called ONCE, by RewinderApp, when the app process starts
 * (also when a notification button wakes the app up while it is closed).
 * It opens the local database; first time ever, it fills it with SampleData.
 */
public final class RepositoryProvider {

    private static QuestionRepository repository;
    private static ProgressStore progressStore;

    private RepositoryProvider() {
    }

    /** Opens the database. Safe to call more than once: only the first call does something. */
    public static synchronized void init(Context context) {
        if (repository != null) return;
        RewinderDbHelper helper = RewinderDbHelper.getInstance(context);
        repository = new SqliteQuestionRepository(helper);
        progressStore = new SqliteProgressStore(helper);
    }

    public static synchronized QuestionRepository get() {
        if (repository == null) {
            throw new IllegalStateException("RepositoryProvider.init(context) was not called. "
                    + "Check that android:name=\".RewinderApp\" is set on <application> in AndroidManifest.xml");
        }
        return repository;
    }

    public static synchronized ProgressStore getProgressStore() {
        if (progressStore == null) {
            throw new IllegalStateException("RepositoryProvider.init(context) was not called.");
        }
        return progressStore;
    }

    /** For tests, or to plug in another implementation (e.g. InMemoryQuestionRepository). */
    public static synchronized void set(QuestionRepository newRepository, ProgressStore newProgressStore) {
        repository = newRepository;
        progressStore = newProgressStore;
    }
}
