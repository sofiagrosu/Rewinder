package com.example.myapplication1.quiz.delivery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Notification setup for the quiz: the channels and their constants.
 *   - "Quiz questions": the questions themselves (pop up, visible on the lock screen)
 *   - "Background status": the silent, permanent notification of QuizService
 * On Android every notification must belong to a channel; the user can
 * turn each channel on/off in Settings → Apps → Rewinder → Notifications.
 */
public final class QuizNotifications {

    public static final String CHANNEL_ID = "quiz_questions";
    public static final String SERVICE_CHANNEL_ID = "quiz_service";

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
        // show the question (not "content hidden") on the lock screen.
        // NOTE: Android applies channel settings only when the channel is first created;
        // for an already installed app, the per-notification visibility in QuestionNotifier does the job.
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationChannel serviceChannel = new NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Background status",
                NotificationManager.IMPORTANCE_LOW);     // LOW = no sound, no pop-up
        serviceChannel.setDescription("Shown while Rewinder is looking for free moments");

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
        manager.createNotificationChannel(serviceChannel);
    }
}
