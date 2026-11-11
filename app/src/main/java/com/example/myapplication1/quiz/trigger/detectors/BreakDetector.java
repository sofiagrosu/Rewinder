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
import com.example.myapplication1.quiz.trigger.TriggerStatus;

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
 * During a break, questions come in sets of QUESTIONS_PER_SET. Spacing counts from the ANSWER:
 * the next question comes QUESTION_SPACING_MILLIS after the previous one was answered, the next
 * set PAUSE_BETWEEN_SETS_MILLIS after the last question of the set was answered (or right after
 * "bad moment"). While a question waits for an answer nothing new is sent. Only questions really
 * sent count; a blocked moment is retried after RETRY_AFTER_NOT_SENT_MILLIS.
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
    private int questionsInSet = 0;        // questions really sent in the current set
    private long nextMomentAt = 0;         // during a break: when the next moment is due
    private boolean waitingForAnswer = false;   // a question was sent and is not answered yet

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
            TriggerStatus.motion = TriggerStatus.Motion.UNKNOWN;
            return;
        }
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (stepDetector == null) {
            Log.w(TAG, "BreakDetector: this phone has no step detector sensor, not started");
            sensorManager = null;
            TriggerStatus.motion = TriggerStatus.Motion.UNKNOWN;
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
        TriggerStatus.motion = TriggerStatus.Motion.UNKNOWN;
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
                TriggerStatus.motion = TriggerStatus.Motion.BREAK;
                TriggerStatus.breakStartedAt = now;
                Log.d(TAG, "Break started (" + (cameFromWalking ? "stopped walking" : "not walking") + ")");
            } else {
                return;
            }
        }

        if (waitingForAnswer) return;          // the next question comes only after the answer
        if (now < nextMomentAt) return;

        if (questionsInSet == TriggerConfig.QUESTIONS_PER_SET) {
            questionsInSet = 0;   // the pause is over: a new set starts
        }
        TriggerStatus.questionsInSet = questionsInSet + 1;   // read by the coordinator while sending
        boolean sent = listener.onMomentDetected(reasonFor(questionsInSet + 1));
        if (!sent) {
            // blocked (quiet hours, calendar...) or nothing to ask: it does not count, try again later
            TriggerStatus.questionsInSet = questionsInSet;
            nextMomentAt = now + TriggerConfig.RETRY_AFTER_NOT_SENT_MILLIS;
            return;
        }
        questionsInSet++;
        waitingForAnswer = true;
        cameFromWalking = false;   // only the very first question says "you stopped walking"
        Log.d(TAG, "Break: question " + questionsInSet + " of " + TriggerConfig.QUESTIONS_PER_SET
                + " sent, waiting for the answer");
    }

    /** The question sent last was answered: the spacing starts now. */
    @Override
    public void onQuestionAnswered() {
        if (!waitingForAnswer) return;
        waitingForAnswer = false;
        long now = SystemClock.elapsedRealtime();
        boolean setFinished = questionsInSet >= TriggerConfig.QUESTIONS_PER_SET;
        nextMomentAt = now + (setFinished
                ? TriggerConfig.PAUSE_BETWEEN_SETS_MILLIS
                : TriggerConfig.QUESTION_SPACING_MILLIS);
        Log.d(TAG, "Answered: next " + (setFinished ? "set" : "question") + " in "
                + ((nextMomentAt - now) / 1000) + " s (if still on a break)");
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
            TriggerStatus.motion = TriggerStatus.Motion.WALKING;
            TriggerStatus.breakStartedAt = 0;
            lastStepAt = stepAt;
            waitingForAnswer = false;   // the question on screen is withdrawn (see onWalkingStarted)
            Log.d(TAG, "Walking detected (" + recentSteps.size() + " steps), break over");
            listener.onWalkingStarted();   // a question still on screen (also on the watch) is withdrawn
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not needed for steps
    }

    /** "Bad moment" answered Yes: skip the rest of this set, the next one comes after the long pause. */
    @Override
    public void postponeCycle() {
        if (!inBreak) return;
        questionsInSet = TriggerConfig.QUESTIONS_PER_SET;   // the set counts as finished
        waitingForAnswer = false;                           // the question was withdrawn
        nextMomentAt = SystemClock.elapsedRealtime() + TriggerConfig.PAUSE_BETWEEN_SETS_MILLIS;
        Log.d(TAG, "Bad moment: the rest of the set is skipped, trying again in "
                + TriggerConfig.PAUSE_BETWEEN_SETS_MILLIS / 60_000 + " min");
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
        waitingForAnswer = false;
        TriggerStatus.questionsInSet = 0;
        TriggerStatus.motion = TriggerStatus.Motion.STILL_WAITING;
        TriggerStatus.breakStartedAt = 0;
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
