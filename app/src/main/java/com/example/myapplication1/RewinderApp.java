package com.example.myapplication1;

import android.app.Application;

import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.data.StatsProvider;
import com.example.myapplication1.stats.StatsCalculator;

/**
 * Runs once, before anything else, every time Android starts the app process.
 * Important: when the user taps an answer button on a notification while the app is closed,
 * Android starts the process and calls this first, THEN AnswerReceiver.
 * So the database is always open when the quiz needs it.
 *
 * Registered in AndroidManifest.xml with android:name=".RewinderApp".
 */
public class RewinderApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RepositoryProvider.init(this);
        // The only place that connects features: quiz/ reads the stats through data/StatsProvider,
        // without importing stats/. Swap the implementation here and nothing else changes.
        StatsProvider.set(() -> new StatsCalculator(RepositoryProvider.get()));
    }
}
