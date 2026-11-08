package com.example.myapplication1.quiz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.myapplication1.quiz.settings.QuizSettings;

/**
 * Starts the monitoring again after the phone restarts (and after the app is updated),
 * but only if the user had not turned it off: QuizService.start() checks that.
 * Without this, nothing would be monitored until the user opened the app.
 *
 * Test without rebooting:
 *   adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.example.myapplication1
 */
public class QuizBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        boolean enabled = new QuizSettings(context).isMonitoringEnabled();
        Log.d("QUIZ", "Boot/update: monitoring " + (enabled ? "restarted" : "stays off (user turned it off)"));
        if (enabled) QuizService.start(context);
    }
}
