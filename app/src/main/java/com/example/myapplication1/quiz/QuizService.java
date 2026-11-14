package com.example.myapplication1.quiz;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication1.MainActivity;
import com.example.myapplication1.R;
import com.example.myapplication1.quiz.delivery.QuizNotifications;
import com.example.myapplication1.quiz.settings.QuizSettings;
import com.example.myapplication1.quiz.trigger.MomentCondition;
import com.example.myapplication1.quiz.trigger.MomentDetector;
import com.example.myapplication1.quiz.trigger.MomentListener;
import com.example.myapplication1.quiz.trigger.TriggerConfig;
import com.example.myapplication1.quiz.trigger.TriggerManager;
import com.example.myapplication1.quiz.trigger.TriggerStatus;
import com.example.myapplication1.quiz.trigger.blocked.BlockedTimeSource;
import com.example.myapplication1.quiz.trigger.blocked.CalendarBusySource;
import com.example.myapplication1.quiz.trigger.blocked.QuietHoursSource;
import com.example.myapplication1.quiz.trigger.blocked.TimeRange;
import com.example.myapplication1.quiz.trigger.conditions.BlockedTimesCondition;
import com.example.myapplication1.quiz.trigger.detectors.BreakDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Foreground service: runs the quiz trigger in the background, also when the app is closed.
 * Android requires a permanent notification while it runs, with a "Stop" button.
 *
 * MONITORING STATES
 *   ACTIVE    detectors run, the wake lock is held (renewed every minute), and the service checks
 *             every minute whether a busy calendar event or quiet hours have started.
 *   SUSPENDED busy calendar event or quiet hours: detectors stopped (no sensor), wake lock
 *             RELEASED (no battery cost). One alarm is set for the moment the block ends,
 *             and it brings the service back to ACTIVE.
 *   OFF       the user turned monitoring off ("Stop" or the Settings switch): the service ends,
 *             and neither opening the app nor a phone restart starts it again
 *             (QuizSettings.isMonitoringEnabled).
 *
 * While the service runs, Android keeps the app process alive, so the quiz's in-memory state
 * (the question waiting for an answer) is not lost between notifications.
 */
public class QuizService extends Service {

    private static final String TAG = "QUIZ";
    private static final String ACTION_STOP = "com.example.myapplication1.quiz.STOP_SERVICE";
    private static final String ACTION_RESTART_DETECTORS = "com.example.myapplication1.quiz.RESTART_DETECTORS";
    private static final String ACTION_EVALUATE = "com.example.myapplication1.quiz.EVALUATE";
    private static final String ACTION_POSTPONE_CYCLE = "com.example.myapplication1.quiz.POSTPONE_CYCLE";
    private static final String ACTION_QUESTION_ANSWERED = "com.example.myapplication1.quiz.QUESTION_ANSWERED";
    private static final int SERVICE_NOTIFICATION_ID = 1000;
    private static final int ALARM_REQUEST_CODE = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable evaluateRunnable = this::evaluate;

    private QuizSettings settings;
    private List<BlockedTimeSource> blockedSources;
    private TriggerManager triggerManager;
    private PowerManager.WakeLock wakeLock;
    private boolean suspended = false;

    // ---------------------------------------------------------------- start / stop helpers

    /** Starts monitoring, unless the user turned it off (then this does nothing). */
    public static void start(Context context) {
        if (!new QuizSettings(context).isMonitoringEnabled()) return;
        ContextCompat.startForegroundService(context, new Intent(context, QuizService.class));
    }

    /**
     * Restarts the detectors, e.g. after the user granted a permission they need
     * (a detector that was missing its permission did not start the first time).
     */
    public static void restartDetectors(Context context) {
        if (!new QuizSettings(context).isMonitoringEnabled()) return;
        Intent intent = new Intent(context, QuizService.class).setAction(ACTION_RESTART_DETECTORS);
        ContextCompat.startForegroundService(context, intent);
    }

    /** The user said "bad moment": skip the rest of the current set (see MomentDetector.postponeCycle). */
    public static void postponeCycle(Context context) {
        if (!new QuizSettings(context).isMonitoringEnabled()) return;
        Intent intent = new Intent(context, QuizService.class).setAction(ACTION_POSTPONE_CYCLE);
        ContextCompat.startForegroundService(context, intent);
    }

    /** The pending question was answered: the detectors start counting the spacing from now. */
    public static void questionAnswered(Context context) {
        if (!new QuizSettings(context).isMonitoringEnabled()) return;
        Intent intent = new Intent(context, QuizService.class).setAction(ACTION_QUESTION_ANSWERED);
        ContextCompat.startForegroundService(context, intent);
    }

    /** Turns monitoring off for good (until the user turns it on again). */
    public static void stop(Context context) {
        new QuizSettings(context).setMonitoringEnabled(false);
        context.stopService(new Intent(context, QuizService.class));
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void onCreate() {
        super.onCreate();
        QuizNotifications.createChannel(this);
        settings = new QuizSettings(this);

        List<MomentDetector> detectors = new ArrayList<>();
        detectors.add(new BreakDetector());   // not walking → sets of questions (see TriggerConfig)
        // TEST alternative: detectors.add(new IntervalDetector(20_000));   // a "moment" every 20 s
        // no question during quiet hours (default 22-08) or busy calendar events (if turned on)
        blockedSources = Arrays.asList(
                new QuietHoursSource(settings),
                new CalendarBusySource(this, settings));
        List<MomentCondition> conditions = new ArrayList<>();
        conditions.add(new BlockedTimesCondition(blockedSources));
        QuizCoordinator quiz = QuizCoordinator.getInstance(this);
        triggerManager = new TriggerManager(detectors, conditions, new MomentListener() {
            @Override
            public boolean onMomentDetected(String reason) {
                Log.d("TRIGGER", "Moment detected: " + reason);
                return quiz.onMomentDetected(reason);   // true = a question was sent
            }

            @Override
            public void onWalkingStarted() {
                quiz.onWalkingStarted();   // withdraw the question that is still on screen
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP.equals(action)) {
            Log.d(TAG, "Monitoring turned off from the service notification");
            settings.setMonitoringEnabled(false);
            stopSelf();
            return START_NOT_STICKY;
        }

        goForeground();   // must come first: Android requires it after startForegroundService()

        if (!settings.isMonitoringEnabled()) {
            stopSelf();   // e.g. the system restarted us after the user turned monitoring off
            return START_NOT_STICKY;
        }

        if (ACTION_POSTPONE_CYCLE.equals(action)) {
            triggerManager.postponeCycle();
            return START_STICKY;
        }
        if (ACTION_QUESTION_ANSWERED.equals(action)) {
            triggerManager.onQuestionAnswered();
            return START_STICKY;
        }
        if (ACTION_RESTART_DETECTORS.equals(action)) {
            triggerManager.stop();
            Log.d(TAG, "Restarting detectors");
        }

        evaluate();   // also what ACTION_EVALUATE (the alarm) does
        return START_STICKY;   // if Android kills it, restart it when possible
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(evaluateRunnable);
        cancelAlarm();
        triggerManager.stop();
        releaseWakeLock();
        TriggerStatus.motion = TriggerStatus.Motion.UNKNOWN;
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;   // nobody binds to this service, it is only started
    }

    // ---------------------------------------------------------------- ACTIVE / SUSPENDED

    /** Decides between ACTIVE and SUSPENDED from the blocked times, now. Runs on the main thread. */
    private void evaluate() {
        handler.removeCallbacks(evaluateRunnable);
        long until = blockedUntil(System.currentTimeMillis());
        if (until > 0) {
            suspend(until);
        } else {
            resume();
            handler.postDelayed(evaluateRunnable, TriggerConfig.EVALUATE_INTERVAL_MILLIS);
        }
    }

    private void resume() {
        cancelAlarm();
        acquireWakeLock();                  // takes it, or renews the time limit
        if (suspended) Log.d(TAG, "Monitoring resumed");
        suspended = false;
        triggerManager.start(this);         // safe to call again: it ignores a second start
    }

    private void suspend(long until) {
        if (!suspended) {
            suspended = true;
            triggerManager.stop();          // the step sensor is released too
            releaseWakeLock();              // nothing to keep awake while we wait
            TriggerStatus.motion = TriggerStatus.Motion.PAUSED;
            Log.d(TAG, "Monitoring suspended (busy calendar or quiet hours)");
        }
        scheduleAlarm(until);               // wakes the service when the block is over
    }

    /** End of the latest blocked interval that contains {@code now}; 0 if nothing blocks now. */
    private long blockedUntil(long now) {
        long until = 0;
        for (BlockedTimeSource source : blockedSources) {
            for (TimeRange range : source.blockedBetween(now, now + 1)) {
                if (range.contains(now)) until = Math.max(until, range.getEnd());
            }
        }
        return until;
    }

    /**
     * Inexact on purpose (no special permission): in Doze it can be a few minutes late,
     * which only delays the first question after quiet hours.
     */
    private void scheduleAlarm(long atMillis) {
        AlarmManager alarms = getSystemService(AlarmManager.class);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                Math.max(atMillis, System.currentTimeMillis() + 1_000), alarmIntent());
    }

    private void cancelAlarm() {
        getSystemService(AlarmManager.class).cancel(alarmIntent());
    }

    private PendingIntent alarmIntent() {
        Intent intent = new Intent(this, QuizService.class).setAction(ACTION_EVALUATE);
        return PendingIntent.getService(this, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // ---------------------------------------------------------------- foreground notification

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
                .setContentText("Questions come while you take a break from walking")
                .setOngoing(true)
                .setSilent(true)
                .setLocalOnly(true)   // phone only: don't bridge this to the Wear OS watch
                .setContentIntent(open)
                .addAction(0, "Stop", stop)
                .build();
    }

    // ---------------------------------------------------------------- wake lock

    /**
     * Keeps the CPU awake so the 5-second checks and the step events are not frozen by Doze.
     * Only held while ACTIVE, with a short time limit that evaluate() renews every minute:
     * if the service freezes, the lock expires by itself instead of draining the battery.
     */
    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Rewinder:QuizTrigger");
            wakeLock.setReferenceCounted(false);   // acquire() again = renew, not "two holders"
        }
        wakeLock.acquire(TriggerConfig.WAKE_LOCK_TIMEOUT_MILLIS);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
