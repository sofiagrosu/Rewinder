package com.example.myapplication1.quiz.trigger.detectors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.myapplication1.quiz.trigger.MomentDetector;
import com.example.myapplication1.quiz.trigger.MomentListener;
import com.example.myapplication1.quiz.trigger.TriggerConfig;

import java.util.ArrayDeque;

/**
 * Detects breaks: the time when the user is NOT walking, using the phone's STEP DETECTOR sensor
 * (a dedicated chip, one event per step; moving the phone in the hand does not make steps).
 *
 *   WALKING ──(no step for STOPPED_FOR_MILLIS)──▶ BREAK ──(walking again)──▶ WALKING
 *
 * The app starts in "not walking" too, so sitting with the phone also starts a break
 * after STOPPED_FOR_MILLIS without steps.
 *
 * During a break, moments come in sets:
 *   QUESTIONS_PER_SET moments, QUESTION_SPACING_MILLIS apart,
 *   then a pause of PAUSE_BETWEEN_SETS_MILLIS, then the next set... until the user walks again.
 * "Walking again" = MIN_STEPS_TO_COUNT_AS_WALKING steps within WALKING_WINDOW_MILLIS;
 * a few single steps (shifting feet at the bus stop) don't end the break.
 * All numbers: see TriggerConfig.
 *
 * Needs the ACTIVITY_RECOGNITION permission (Android 10+) and must run in a foreground
 * service, because Android doesn't give sensor data to background apps otherwise.
 */
public class BreakDetector implements MomentDetector, SensorEventListener {

    private static final String TAG = "TRIGGER";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<Long> recentSteps = new ArrayDeque<>();   // step times, ms since boot

    private SensorManager sensorManager;
    private MomentListener listener;

    private long lastStepAt;               // or the start time, if no step yet
    private boolean walking = false;       // walked enough recently
    private boolean cameFromWalking = false;
    private boolean inBreak = false;
    private int questionsInSet = 0;        // moments already sent in the current set
    private long nextMomentAt = 0;         // during a break: when the next moment is due

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            onTick(SystemClock.elapsedRealtime());
            handler.postDelayed(this, TriggerConfig.CHECK_INTERVAL_MILLIS);
        }
    };

    // ---------------------------------------------------------------- MomentDetector

    @Override
    public void start(Context context, MomentListener listener) {
        this.listener = listener;
        resetToNotWalking();

        if (!hasActivityPermission(context)) {
            Log.w(TAG, "BreakDetector: ACTIVITY_RECOGNITION permission missing, not started");
            return;
        }
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (stepDetector == null) {
            Log.w(TAG, "BreakDetector: this phone has no step detector sensor, not started");
            sensorManager = null;
            return;
        }
        sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TriggerConfig.CHECK_INTERVAL_MILLIS);
        Log.d(TAG, "BreakDetector started");
    }

    @Override
    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
        handler.removeCallbacks(tick);
        resetToNotWalking();
    }

    // ---------------------------------------------------------------- the state machine

    private void onTick(long now) {
        if (!inBreak) {
            // waiting for STOPPED_FOR_MILLIS without any step
            if (now - lastStepAt >= TriggerConfig.STOPPED_FOR_MILLIS) {
                inBreak = true;
                walking = false;
                recentSteps.clear();
                questionsInSet = 0;
                nextMomentAt = now;   // first question of the first set: right now
                Log.d(TAG, "Break started (" + (cameFromWalking ? "stopped walking" : "not walking") + ")");
            } else {
                return;
            }
        }

        if (now < nextMomentAt) return;

        if (questionsInSet == TriggerConfig.QUESTIONS_PER_SET) {
            questionsInSet = 0;   // the pause is over: a new set starts
        }
        questionsInSet++;
        listener.onMomentDetected(reasonFor(questionsInSet));
        Log.d(TAG, "Break: question " + questionsInSet + " of " + TriggerConfig.QUESTIONS_PER_SET);

        nextMomentAt = now + (questionsInSet < TriggerConfig.QUESTIONS_PER_SET
                ? TriggerConfig.QUESTION_SPACING_MILLIS
                : TriggerConfig.PAUSE_BETWEEN_SETS_MILLIS);
        cameFromWalking = false;   // only the very first question says "you stopped walking"
    }

    /** One call per step. */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // event.timestamp = when the step happened, in nanoseconds since the phone booted
        // (same clock as SystemClock.elapsedRealtime(), which is in milliseconds)
        long stepAt = event.timestamp / 1_000_000L;
        recentSteps.addLast(stepAt);
        while (!recentSteps.isEmpty()
                && stepAt - recentSteps.peekFirst() > TriggerConfig.WALKING_WINDOW_MILLIS) {
            recentSteps.removeFirst();
        }

        if (!inBreak) {
            lastStepAt = stepAt;   // before a break: any step restarts the "1 minute without steps"
        }

        if (!walking && recentSteps.size() >= TriggerConfig.MIN_STEPS_TO_COUNT_AS_WALKING) {
            // walking (again): no questions until the user stops
            walking = true;
            cameFromWalking = true;
            inBreak = false;
            lastStepAt = stepAt;
            Log.d(TAG, "Walking detected (" + recentSteps.size() + " steps), break over");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not needed for steps
    }

    // ---------------------------------------------------------------- helpers

    private void resetToNotWalking() {
        recentSteps.clear();
        lastStepAt = SystemClock.elapsedRealtime();   // the 1-minute wait starts now
        walking = false;
        cameFromWalking = false;
        inBreak = false;
        questionsInSet = 0;
        nextMomentAt = 0;
    }

    private String reasonFor(int questionNumber) {
        if (questionNumber == 1 && cameFromWalking) return "You stopped walking";
        if (questionNumber == 1) return "You've been still for a while";
        return "Break: question " + questionNumber + " of " + TriggerConfig.QUESTIONS_PER_SET;
    }

    private static boolean hasActivityPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true;   // before Android 10: no permission
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
