package com.example.myapplication1.quiz;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication1.data.AnswerRecord;
import com.example.myapplication1.data.DailyStats;
import com.example.myapplication1.data.IgnoredRecord;
import com.example.myapplication1.data.ProgressStore;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionRepository;
import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.data.StatsProvider;
import com.example.myapplication1.data.Subject;
import com.example.myapplication1.quiz.delivery.QuestionNotifier;
import com.example.myapplication1.quiz.ordering.ProgressUpdater;
import com.example.myapplication1.quiz.ordering.QuestionPicker;
import com.example.myapplication1.quiz.trigger.TriggerStatus;
import com.example.myapplication1.quiz.delivery.AnswerReceiver;

import java.util.List;

/**
 * The "conductor": the only class that knows all four quiz parts.
 *   trigger  → onMomentDetected(reason) → ordering picks → delivery shows
 *   delivery → onAnswer(id, option)     → check → ordering records → delivery shows result
 *
 * One single instance for the whole app (getInstance), so MainActivity and
 * AnswerReceiver share the same questions and the same progress.
 *
 * Data:
 *   - questions, subjects and the answer history come from the team's QuestionRepository
 *     (RepositoryProvider.get()), the same data Home and Stats use;
 *   - every answer is saved there as an AnswerRecord, so it shows up in Home and Stats;
 *   - the Leitner/ordering progress is saved in the same database (RepositoryProvider.getProgressStore()).
 */
public final class QuizCoordinator {

    private static final String TAG = "QUIZ";

    /**
     * For this long after a question appears: no new question replaces it.
     * After that it disappears by itself and counts as ignored.
     * A swipe closes it for good at once (also counted as ignored, never as a wrong answer).
     */
    public static final long PROTECTION_MILLIS = 3 * 60_000;   // 3 minutes

    private static QuizCoordinator instance;

    private final Context appContext;
    private final QuestionRepository repository;
    private final QuestionPicker picker;
    private final ProgressUpdater updater;
    private final QuestionNotifier notifier;

    // The question currently waiting for an answer (-1 = none)
    private long pendingQuestionId = -1;
    private long pendingShownAt = 0;
    private String pendingReason = "";
    private boolean pendingFirstInSet = false;   // was it the first question of its set?

    // Marks a question as "ignored" when it expires unanswered (needs the app process alive,
    // which QuizService guarantees while it runs).
    private final Handler handler = new Handler(Looper.getMainLooper());

    // The last question that was actually sent, for the "De ce acum?" screen
    private String lastMomentReason = "";
    private long lastMomentAt = 0;

    /** Whoever shows the pending question inside the app (MainActivity's QuestionBanner). */
    public interface PendingListener {
        /** The pending question changed: a new one was sent, or it was answered / expired. */
        void onPendingChanged();
    }
    private PendingListener pendingListener;   // at most one; null while the app is not on screen

    public static synchronized QuizCoordinator getInstance(Context context) {
        if (instance == null) {
            instance = new QuizCoordinator(context.getApplicationContext());
        }
        return instance;
    }

    private QuizCoordinator(Context appContext) {
        this.appContext = appContext;
        repository = RepositoryProvider.get();
        // Leitner/ordering progress is saved in the local database (SqliteProgressStore),
        // opened by RewinderApp: it survives when Android stops the app process.
        ProgressStore progressStore = RepositoryProvider.getProgressStore();
        picker = QuestionPicker.withDefaultRules(progressStore);
        updater = new ProgressUpdater(progressStore);
        notifier = new QuestionNotifier(appContext);
    }

    /** Called by the trigger: pick the next question and send it. */
    public void onMomentDetected(String reason) {
        long now = System.currentTimeMillis();
        if (hasProtectedPendingQuestion(now)) {
            Log.d(TAG, "Question " + pendingQuestionId + " still waiting for an answer, not sending a new one");
            return;
        }

        List<Question> candidates = repository.getAllQuestions();   // all subjects

        Question question = picker.pickNext(candidates, now);
        if (question == null) {
            Log.d(TAG, "No question to send");
            return;
        }
        notifier.showQuestion(question, getSubjectTitle(question.getSubjectId()), reason,
                false, PROTECTION_MILLIS);
        updater.recordShown(question.getId(), now);
        pendingQuestionId = question.getId();
        pendingShownAt = now;
        pendingReason = reason;
        pendingFirstInSet = TriggerStatus.questionsInSet == 1;   // written by BreakDetector
        lastMomentReason = reason;
        lastMomentAt = now;
        scheduleExpiry(question.getId());
        notifyPendingChanged();
        Log.d(TAG, "Sent question " + question.getId() + " (" + reason + ")");
    }

    /** After the protection time, a question nobody answered is saved as ignored. */
    private void scheduleExpiry(long questionId) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> onExpired(questionId), PROTECTION_MILLIS);
    }

    private void onExpired(long questionId) {
        if (questionId != pendingQuestionId) return;   // answered or swiped in the meantime
        markIgnored(questionId);
    }

    /** Saves the pending question as ignored (NOT as a wrong answer) and clears it. */
    private void markIgnored(long questionId) {
        handler.removeCallbacksAndMessages(null);
        Question question = repository.getQuestion(questionId);
        if (question != null) {
            repository.saveIgnored(new IgnoredRecord(
                    questionId, question.getSubjectId(), pendingShownAt));
            Log.d(TAG, "Question " + questionId + " ignored");
        }
        pendingQuestionId = -1;
        notifyPendingChanged();
    }

    /** Called by AnswerReceiver when the user taps an option. */
    public void onAnswer(long questionId, int chosenOption) {
        Question question = repository.getQuestion(questionId);
        if (question == null) {
            Log.w(TAG, "Unknown question " + questionId);
            return;
        }
        if (questionId == pendingQuestionId) {
            handler.removeCallbacksAndMessages(null);   // answered: it must not count as ignored
            pendingQuestionId = -1;   // a new question may come at the next moment
            notifyPendingChanged();
        }
        boolean dontKnow = chosenOption == AnswerReceiver.OPTION_DONT_KNOW;
        // "Nu știu" is an answer, not an ignored question; it counts as not correct,
        // so the question comes back sooner (Leitner) and the explanation is shown.
        boolean correct = !dontKnow && question.isCorrect(chosenOption);
        long now = System.currentTimeMillis();
        updater.recordAnswer(question, correct, now);   // quiz ordering (Leitner)
        repository.saveAnswer(new AnswerRecord(          // shared history (Home, Stats)
                question.getId(), question.getSubjectId(), now, chosenOption, correct));
        if (dontKnow) {
            notifier.showDontKnow(question, getSubjectTitle(question.getSubjectId()), statsLine());
        } else {
            notifier.showResult(question, getSubjectTitle(question.getSubjectId()), correct, statsLine());
        }
        Log.d(TAG, "Question " + questionId + ": " + (dontKnow ? "don't know" : correct ? "correct" : "wrong"));
    }

    /**
     * Called by AnswerReceiver when the user swipes the question away.
     * The swipe closes it for good: it is saved as ignored (not as a wrong answer) and does not
     * come back. If it was the first question of a set, we ask whether this is a bad moment.
     */
    public void onDismissed(long questionId) {
        if (questionId != pendingQuestionId) return;   // an old question, nothing to do

        // The system may also report a notification that simply timed out; the expiry timer
        // handles that case, and it is not a swipe.
        long elapsed = System.currentTimeMillis() - pendingShownAt;
        if (elapsed >= PROTECTION_MILLIS - 2_000) return;

        boolean wasFirstInSet = pendingFirstInSet;
        markIgnored(questionId);
        if (wasFirstInSet) {
            notifier.showBadMomentPrompt();   // "Este un moment nepotrivit pentru întrebări?"
        }
    }

    /**
     * Called by AnswerReceiver: the answer to "Este un moment nepotrivit pentru întrebări?".
     *   Da → the next two questions of the set are not sent; the detector tries again after 15 min.
     *   Nu → nothing changes: the set goes on, one question per minute.
     */
    public void onBadMoment(boolean bad) {
        notifier.cancelPrompt();
        if (!bad) return;

        withdrawPending();   // a question that arrived meanwhile is not wanted either
        QuizService.postponeCycle(appContext);
    }

    /**
     * Called when the user starts walking: the cycle of questions is interrupted, so the question
     * still on screen is removed (cancelling it here also removes it from the smartwatch).
     * It is not counted as ignored: the app withdrew it, the user did not skip it.
     */
    public void onWalkingStarted() {
        notifier.cancelPrompt();
        withdrawPending();
    }

    private void withdrawPending() {
        if (pendingQuestionId < 0) return;
        handler.removeCallbacksAndMessages(null);
        notifier.cancelQuestion();
        Log.d(TAG, "Question " + pendingQuestionId + " withdrawn");
        pendingQuestionId = -1;
        notifyPendingChanged();
    }

    /**
     * "Today: 9 answered · 78% correct · 3-day streak", or null if no stats are registered.
     * Called after saveAnswer, so the answer just given is included. The numbers come from
     * whatever RewinderApp registered (today: the same StatsCalculator as Home).
     */
    private String statsLine() {
        DailyStats stats = StatsProvider.today();
        if (stats == null) return null;
        return "Today: " + stats.questionsToday() + " answered · "
                + stats.accuracyToday() + "% correct · " + stats.streakDays() + "-day streak";
    }

    private boolean hasProtectedPendingQuestion(long now) {
        return pendingQuestionId >= 0 && now - pendingShownAt < PROTECTION_MILLIS;
    }

    // ---- read by the in-app banner (display only) ----

    /** The question still waiting for an answer (within its protection time), or null. */
    public Question getPendingQuestion() {
        if (!hasProtectedPendingQuestion(System.currentTimeMillis())) return null;
        return repository.getQuestion(pendingQuestionId);
    }

    /** The "why now" of the pending question ("" if none). */
    public String getPendingReason() {
        return pendingReason;
    }

    /** How long the pending question still stays on screen, in ms (0 if none). */
    public long getPendingRemainingMillis() {
        long remaining = PROTECTION_MILLIS - (System.currentTimeMillis() - pendingShownAt);
        return pendingQuestionId >= 0 && remaining > 0 ? remaining : 0;
    }

    /** Why the last question was sent ("" if none yet), and when (0 if none yet). */
    public String getLastMomentReason() {
        return lastMomentReason;
    }

    public long getLastMomentAt() {
        return lastMomentAt;
    }

    public void setPendingListener(PendingListener listener) {
        pendingListener = listener;
    }

    private void notifyPendingChanged() {
        if (pendingListener != null) pendingListener.onPendingChanged();
    }

    /** Notification / banner title, e.g. "Databases · SQL". */
    public String getSubjectTitle(long subjectId) {
        Subject subject = repository.getSubject(subjectId);
        return subject != null ? subject.getTitle() : "Rewinder";
    }
}
