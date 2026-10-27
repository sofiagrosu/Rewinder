package com.example.myapplication1.data;

import java.time.ZoneId;

/**
 * Gives every screen the same repository instance:
 *
 *     QuestionRepository repo = RepositoryProvider.get();
 *
 * When the Room database is ready, replace the line marked CHANGE HERE
 * with the Room implementation. No other file has to change.
 */
public final class RepositoryProvider {

    private static QuestionRepository instance;

    private RepositoryProvider() {
    }

    public static synchronized QuestionRepository get() {
        if (instance == null) {
            // CHANGE HERE when the database exists.
            instance = SampleData.createRepository(System.currentTimeMillis(), ZoneId.systemDefault());
        }
        return instance;
    }

    /** For tests, or to plug in another implementation at app start. */
    public static synchronized void set(QuestionRepository repository) {
        instance = repository;
    }
}

