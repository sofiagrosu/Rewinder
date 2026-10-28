package com.example.myapplication1.quiz.delivery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication1.MainActivity;
import com.example.myapplication1.R;
import com.example.myapplication1.data.Question;

/**
 * Builds and shows the quiz notifications:
 *   - the question, with one button per option (max 3 → also fits on the watch)
 *   - the result after answering (✓ / ✗ + explanation), replacing the question
 * The question can't be swiped away for good during its protection time:
 * if it is, the coordinator shows it again (see QuizCoordinator.onDismissed).
 *
 * WATCH: phone notifications are bridged to a paired Wear OS watch automatically, with the same
 * NotificationCompat APIs, so there is no separate watch code. What shapes the look on the watch:
 *   - BigTextStyle = the Wear "Big text" template: short preview in the stream, full text when
 *     tapped, the action buttons stacked under it (max 3), plus an automatic "Open on phone";
 *   - action icons: the watch shows each button as a chip with icon + label (R.drawable.ic_option);
 *   - short titles/options (~20 chars) so nothing is cut on the round screen;
 *   - the service's permanent notification is setLocalOnly(true) in QuizService, so only the
 *     questions (and their results) reach the watch.
 */
public class QuestionNotifier {

    /** One quiz notification at a time: a new question replaces the previous one. */
    public static final int QUESTION_NOTIFICATION_ID = 1001;
    private static final long RESULT_VISIBLE_MILLIS = 15_000;   // result disappears after 15 s

    private final Context context;

    public QuestionNotifier(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Shows the question with its answer buttons.
     * @param silent        true when re-showing after an accidental swipe (no sound, no pop-up)
     * @param timeoutMillis the notification disappears by itself after this time if unanswered
     */
    public void showQuestion(Question question, String title, String reason,
                             boolean silent, long timeoutMillis) {
        NotificationCompat.Builder builder = baseBuilder(title)
                .setContentText(question.getText())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(question.getText()))
                .setSubText(reason)                          // "why now", shown in the header
                .setAutoCancel(false)                        // tapping the text opens the app, question stays
                .setSilent(silent)
                .setTimeoutAfter(timeoutMillis)
                .setDeleteIntent(dismissIntent(question.getId()));   // told when the user swipes it away

        for (int n = 1; n <= question.getOptionCount(); n++) {
            // the icon is only drawn on the watch (phones ignore action icons since Android 7)
            builder.addAction(R.drawable.ic_option, question.getOption(n),
                    answerIntent(question.getId(), n));
        }
        notifySafely(builder);
    }

    /** Replaces the question with the result; no buttons, does not ring again. */
    public void showResult(Question question, String title, boolean correct) {
        String headline = resultHeadline(question, correct);
        String body = headline;
        String explanation = question.getExplanation();
        if (explanation != null && !explanation.isEmpty()) {
            body = headline + "\n" + explanation;
        }

        NotificationCompat.Builder builder = baseBuilder(title)
                .setContentText(headline)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setOnlyAlertOnce(true)
                .setTimeoutAfter(RESULT_VISIBLE_MILLIS);
        notifySafely(builder);
    }

    /** "✓ Correct!" or "✗ The answer was: ..." — used by the notification and by the in-app banner. */
    public static String resultHeadline(Question question, boolean correct) {
        return correct
                ? "✓ Correct!"
                : "✗ The answer was: " + question.getOption(question.getCorrectOption());
    }

    // ---------------------------------------------------------------------------------

    private NotificationCompat.Builder baseBuilder(String title) {
        return new NotificationCompat.Builder(context, QuizNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)   // full question on the lock screen
                .setContentIntent(openAppIntent())   // tapping the text opens the app
                .setAutoCancel(true);
    }

    /** What an answer button does: send (questionId, option) to AnswerReceiver. */
    private PendingIntent answerIntent(long questionId, int option) {
        Intent intent = new Intent(context, AnswerReceiver.class);
        intent.setAction(AnswerReceiver.ACTION_ANSWER);
        intent.putExtra(AnswerReceiver.EXTRA_QUESTION_ID, questionId);
        intent.putExtra(AnswerReceiver.EXTRA_OPTION, option);
        // a different requestCode per button, otherwise Android would reuse one PendingIntent for all
        int requestCode = (int) (questionId * 10 + option);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** What happens when the user swipes the question away: tell AnswerReceiver. */
    private PendingIntent dismissIntent(long questionId) {
        Intent intent = new Intent(context, AnswerReceiver.class);
        intent.setAction(AnswerReceiver.ACTION_DISMISSED);
        intent.putExtra(AnswerReceiver.EXTRA_QUESTION_ID, questionId);
        int requestCode = (int) (questionId * 10);   // option buttons use *10 + 1..3, so no clash
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent openAppIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressLint("MissingPermission")   // checked in canNotify()
    private void notifySafely(NotificationCompat.Builder builder) {
        if (!canNotify()) {
            Log.w("QUIZ", "Notifications not allowed, nothing shown");
            return;
        }
        NotificationManagerCompat.from(context).notify(QUESTION_NOTIFICATION_ID, builder.build());
    }

    /**
     * Android 13+ (API 33): the POST_NOTIFICATIONS permission must be granted.
     * Android 12 or older: that permission doesn't exist (checking it would say "denied"),
     * so we only check that the user hasn't turned notifications off in Settings.
     */
    private boolean canNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
}
