package com.example.myapplication1.quiz;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication1.MainActivity;
import com.example.myapplication1.R;
import com.example.myapplication1.quiz.delivery.QuizNotifications;
import com.example.myapplication1.quiz.trigger.MomentCondition;
import com.example.myapplication1.quiz.trigger.MomentDetector;
import com.example.myapplication1.quiz.trigger.TriggerManager;
import com.example.myapplication1.quiz.trigger.detectors.IntervalDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Foreground service: runs the quiz trigger in the background, also when the app is closed.
 * Android requires a permanent notification while it runs ("Rewinder is looking for free moments"),
 * with a "Stop" button.
 *
 * While the service runs, Android keeps the app process alive, so the quiz's in-memory state
 * (progress, the question waiting for an answer) is not lost between notifications.
 *
 * Start: QuizService.start(context)   Stop: the "Stop" button, or QuizService.stop(context)
 */
public class QuizService extends Service {

    private static final String TAG = "QUIZ";
    private static final String ACTION_STOP = "com.example.myapplication1.quiz.STOP_SERVICE";
    private static final int SERVICE_NOTIFICATION_ID = 1000;

    // TEST: the screen-off "deep sleep" pauses normal timers, so we keep the CPU awake.
    // Costs battery: fine for testing, to be removed with the real (sensor/calendar) detectors.
    private static final long WAKE_LOCK_MAX_MILLIS = 2 * 60 * 60 * 1000L;   // safety limit: 2 hours

    private TriggerManager triggerManager;
    private PowerManager.WakeLock wakeLock;

    // ---------------------------------------------------------------- start / stop helpers

    public static void start(Context context) {
        Intent intent = new Intent(context, QuizService.class);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, QuizService.class));
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void onCreate() {
        super.onCreate();
        QuizNotifications.createChannel(this);

        List<MomentDetector> detectors = new ArrayList<>();
        detectors.add(new IntervalDetector(20_000));            // TEST: a "free moment" every 20 s
        List<MomentCondition> conditions = new ArrayList<>();   // none yet
        QuizCoordinator quiz = QuizCoordinator.getInstance(this);
        triggerManager = new TriggerManager(detectors, conditions, reason -> {
            Log.d("TRIGGER", "Moment detected: " + reason);
            quiz.onMomentDetected(reason);
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Log.d(TAG, "Service stopped from its notification");
            stopSelf();
            return START_NOT_STICKY;
        }

        goForeground();
        acquireWakeLock();
        triggerManager.start(this);   // safe to call again: it ignores a second start
        Log.d(TAG, "Service running");
        return START_STICKY;          // if Android kills it, restart it when possible
    }

    @Override
    public void onDestroy() {
        triggerManager.stop();
        releaseWakeLock();
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;   // nobody binds to this service, it is only started
    }

    // ---------------------------------------------------------------- helpers

    private void goForeground() {
        Notification notification = buildServiceNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: the type must match the one declared in AndroidManifest.xml
            startForeground(SERVICE_NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification);
        }
    }

    private Notification buildServiceNotification() {
        Intent stopIntent = new Intent(this, QuizService.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, QuizNotifications.SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.primary))
                .setContentTitle("Rewinder is looking for free moments")
                .setContentText("Test mode: a question every 20 seconds")
                .setOngoing(true)
                .setSilent(true)
                .setLocalOnly(true)   // phone only: don't bridge this to the Wear OS watch
                .setContentIntent(open)
                .addAction(0, "Stop", stop)
                .build();
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Rewinder:QuizTrigger");
        wakeLock.acquire(WAKE_LOCK_MAX_MILLIS);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }
}
