package com.example.myapplication1.quiz;

import android.content.Context;
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
import com.example.myapplication1.quiz.delivery.AnswerReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
 *
 * The pending question (sent, not answered yet) has no time limit:
 *   - while it is pending, no other question is sent;
 *   - a swipe does not remove it: it comes back silently, and the "bad moment?" prompt appears;
 *   - it goes away only when it is answered, when the user says "bad moment" (→ 15-min pause),
 *     or when the user starts walking (withdrawn by the app).
 * Besides notifications, the user can answer a practice set inside the app (PracticeActivity).
 */
public final class QuizCoordinator {

    private static final String TAG = "QUIZ";

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

    /**
     * Called by the trigger: pick the next question and send it.
     * @return true if a question was sent (false: one is still waiting for an answer, or none available)
     */
    public boolean onMomentDetected(String reason) {
        long now = System.currentTimeMillis();
        if (hasPendingQuestion()) {
            Log.d(TAG, "Question " + pendingQuestionId + " still waiting for an answer, not sending a new one");
            return false;
        }

        List<Question> candidates = repository.getAllQuestions();   // all subjects

        Question question = picker.pickNext(candidates, now);
        if (question == null) {
            Log.d(TAG, "No question to send");
            return false;
        }
        // timeout 0: the question stays until it is answered
        notifier.showQuestion(question, getSubjectTitle(question.getSubjectId()), reason, false, 0);
        updater.recordShown(question.getId(), now);
        pendingQuestionId = question.getId();
        pendingShownAt = now;
        pendingReason = reason;
        lastMomentReason = reason;
        lastMomentAt = now;
        notifyPendingChanged();
        Log.d(TAG, "Sent question " + question.getId() + " (" + reason + ")");
        return true;
    }

    /** Saves the pending question as ignored (NOT as a wrong answer). */
    private void recordPendingIgnored() {
        if (pendingQuestionId < 0) return;
        Question question = repository.getQuestion(pendingQuestionId);
        if (question != null) {
            repository.saveIgnored(new IgnoredRecord(
                    pendingQuestionId, question.getSubjectId(), pendingShownAt));
            Log.d(TAG, "Question " + pendingQuestionId + " ignored (bad moment)");
        }
    }

    /** Called by AnswerReceiver (notification / watch) and the in-app banner. Shows the result notification. */
    public void onAnswer(long questionId, int chosenOption) {
        answer(questionId, chosenOption, true);
    }

    /**
     * Called by PracticeActivity: the result is shown in the app, so no result notification.
     * If it was the pending question, its notification is removed.
     */
    public void onAnswerInApp(long questionId, int chosenOption) {
        answer(questionId, chosenOption, false);
    }

    private void answer(long questionId, int chosenOption, boolean showResultNotification) {
        Question question = repository.getQuestion(questionId);
        if (question == null) {
            Log.w(TAG, "Unknown question " + questionId);
            return;
        }
        boolean wasPending = questionId == pendingQuestionId;
        if (wasPending) {
            pendingQuestionId = -1;   // a new question may come at the next moment
            notifier.cancelPrompt();  // a "bad moment?" prompt is no longer needed
            notifyPendingChanged();
            QuizService.questionAnswered(appContext);   // BreakDetector: the 1-minute spacing starts now
        }
        boolean dontKnow = chosenOption == AnswerReceiver.OPTION_DONT_KNOW;
        // "Nu știu" is an answer, not an ignored question; it counts as not correct,
        // so the question comes back sooner (Leitner) and the explanation is shown.
        boolean correct = !dontKnow && question.isCorrect(chosenOption);
        long now = System.currentTimeMillis();
        updater.recordAnswer(question, correct, now);   // quiz ordering (Leitner)
        repository.saveAnswer(new AnswerRecord(          // shared history (Home, Stats)
                question.getId(), question.getSubjectId(), now, chosenOption, correct));
        if (!showResultNotification) {
            if (wasPending) notifier.cancelQuestion();   // answered in the app: remove the question
        } else if (dontKnow) {
            notifier.showDontKnow(question, getSubjectTitle(question.getSubjectId()), statsLine());
        } else {
            notifier.showResult(question, getSubjectTitle(question.getSubjectId()), correct, statsLine());
        }
        Log.d(TAG, "Question " + questionId + ": " + (dontKnow ? "don't know" : correct ? "correct" : "wrong"));
    }

    /**
     * Called by AnswerReceiver when the user swipes the question away.
     * While the user is on a break the question cannot be removed: it comes back silently, and
     * the "bad moment?" prompt appears. Only "good time? → No" (or walking) takes it away.
     */
    public void onDismissed(long questionId) {
        if (questionId != pendingQuestionId) return;   // an old question, nothing to do
        Question question = repository.getQuestion(questionId);
        if (question == null) return;
        notifier.showQuestion(question, getSubjectTitle(question.getSubjectId()), pendingReason, true, 0);
        notifier.showBadMomentPrompt();   // "Is this a good time for questions?"
        Log.d(TAG, "Question " + questionId + " swiped: shown again + bad-moment prompt");
    }

    /**
     * Called by AnswerReceiver: the answer to "Is this a good time for questions?".
     * @param bad true when the user answered No (it is NOT a good time).
     *   No  → the question is removed (saved as ignored) and the set is skipped:
     *        the next question comes after the 15-minute pause.
     *   Yes → nothing changes: the question stays until it is answered.
     */
    public void onBadMoment(boolean bad) {
        notifier.cancelPrompt();
        if (!bad) return;

        recordPendingIgnored();
        withdrawPending();
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
    public String statsLine() {
        DailyStats stats = StatsProvider.today();
        if (stats == null) return null;
        return "Today: " + stats.questionsToday() + " answered · "
                + stats.accuracyToday() + "% correct · " + stats.streakDays() + "-day streak";
    }

    private boolean hasPendingQuestion() {
        return pendingQuestionId >= 0;
    }

    // ---------------------------------------------------------------- practice set (in the app)

    /**
     * The next question of an in-app practice set. The pending question (if any, and not asked
     * yet in this set) comes first; then the normal ordering picks among the others.
     * @param alreadyAsked ids already shown in this practice set
     * @return the question, or null if there is nothing left to ask
     */
    public Question nextPracticeQuestion(Set<Long> alreadyAsked) {
        if (hasPendingQuestion() && !alreadyAsked.contains(pendingQuestionId)) {
            Question pending = repository.getQuestion(pendingQuestionId);
            if (pending != null) return pending;
        }
        List<Question> candidates = new ArrayList<>();
        for (Question q : repository.getAllQuestions()) {
            if (!alreadyAsked.contains(q.getId()) && q.getId() != pendingQuestionId) candidates.add(q);
        }
        long now = System.currentTimeMillis();
        Question question = picker.pickNext(candidates, now);
        if (question != null) {
            updater.recordShown(question.getId(), now);   // it goes "to the end of the queue" too
        }
        return question;
    }

    // ---- read by the in-app banner (display only) ----

    /** The question still waiting for an answer, or null. */
    public Question getPendingQuestion() {
        if (!hasPendingQuestion()) return null;
        return repository.getQuestion(pendingQuestionId);
    }

    /** The "why now" of the pending question ("" if none). */
    public String getPendingReason() {
        return pendingReason;
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
