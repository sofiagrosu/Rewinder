package com.example.myapplication1.quiz.delivery;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Notification setup for quiz questions: the channel and its constants.
 * On Android every notification must belong to a channel; the user can
 * turn each channel on/off in Settings → Apps → Rewinder → Notifications.
 */
public final class QuizNotifications {

    public static final String CHANNEL_ID = "quiz_questions";

    private QuizNotifications() {
        // only static members, never instantiated
    }

    /**
     * Creates the "Quiz questions" channel. Safe to call many times:
     * if the channel already exists, Android ignores the call.
     */
    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Quiz questions",                        // name shown in Settings
                NotificationManager.IMPORTANCE_HIGH);    // HIGH = pops up at the top of the screen
        channel.setDescription("Short questions sent in your free moments");

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}
