package com.example.myapplication1.quiz;

import android.content.Context;
import android.util.Log;

import com.example.myapplication1.data.AnswerRecord;
import com.example.myapplication1.data.InMemoryProgressStore;
import com.example.myapplication1.data.ProgressStore;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionRepository;
import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.data.Subject;
import com.example.myapplication1.quiz.delivery.QuestionNotifier;
import com.example.myapplication1.quiz.ordering.ProgressUpdater;
import com.example.myapplication1.quiz.ordering.QuestionPicker;

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
 *   - the Leitner/ordering progress stays in the quiz's own ProgressStore.
 */
public final class QuizCoordinator {

    private static final String TAG = "QUIZ";

    /**
     * For this long after a question appears: no new question replaces it,
     * and if it is swiped away it comes back (silently). After that it disappears by itself.
     */
    public static final long PROTECTION_MILLIS = 3 * 60_000;   // 3 minutes

    private static QuizCoordinator instance;

    private final QuestionRepository repository;
    private final QuestionPicker picker;
    private final ProgressUpdater updater;
    private final QuestionNotifier notifier;

    // The question currently waiting for an answer (-1 = none)
    private long pendingQuestionId = -1;
    private long pendingShownAt = 0;
    private String pendingReason = "";

    public static synchronized QuizCoordinator getInstance(Context context) {
        if (instance == null) {
            instance = new QuizCoordinator(context.getApplicationContext());
        }
        return instance;
    }

    private QuizCoordinator(Context appContext) {
        repository = RepositoryProvider.get();
        // NOTE: in memory = progress is lost when Android stops the app process.
        // Replace with a persistent store (SharedPreferences / Room) before real use.
        ProgressStore progressStore = new InMemoryProgressStore();
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
        notifier.showQuestion(question, subjectTitle(question.getSubjectId()), reason,
                false, PROTECTION_MILLIS);
        updater.recordShown(question.getId(), now);
        pendingQuestionId = question.getId();
        pendingShownAt = now;
        pendingReason = reason;
        Log.d(TAG, "Sent question " + question.getId() + " (" + reason + ")");
    }

    /** Called by AnswerReceiver when the user taps an option. */
    public void onAnswer(long questionId, int chosenOption) {
        Question question = repository.getQuestion(questionId);
        if (question == null) {
            Log.w(TAG, "Unknown question " + questionId);
            return;
        }
        if (questionId == pendingQuestionId) {
            pendingQuestionId = -1;   // answered: a new question may come at the next moment
        }
        boolean correct = question.isCorrect(chosenOption);
        long now = System.currentTimeMillis();
        updater.recordAnswer(question, correct, now);   // quiz ordering (Leitner)
        repository.saveAnswer(new AnswerRecord(          // shared history (Home, Stats)
                question.getId(), question.getSubjectId(), now, chosenOption, correct));
        notifier.showResult(question, subjectTitle(question.getSubjectId()), correct);
        Log.d(TAG, "Question " + questionId + ": " + (correct ? "correct" : "wrong"));
    }

    /**
     * Called by AnswerReceiver when the user swipes the question away.
     * Within the protection time it comes back silently, for the remaining time only.
     */
    public void onDismissed(long questionId) {
        if (questionId != pendingQuestionId) return;   // an old question, nothing to do

        long now = System.currentTimeMillis();
        long elapsed = now - pendingShownAt;
        if (elapsed >= PROTECTION_MILLIS) {
            pendingQuestionId = -1;                    // protection over: let it go
            return;
        }
        Question question = repository.getQuestion(questionId);
        if (question == null) return;
        notifier.showQuestion(question, subjectTitle(question.getSubjectId()), pendingReason,
                true, PROTECTION_MILLIS - elapsed);
        Log.d(TAG, "Question " + questionId + " shown again (" + (elapsed / 1000) + " s after it appeared)");
    }

    private boolean hasProtectedPendingQuestion(long now) {
        return pendingQuestionId >= 0 && now - pendingShownAt < PROTECTION_MILLIS;
    }

    /** Notification title, e.g. "Databases · SQL". */
    private String subjectTitle(long subjectId) {
        Subject subject = repository.getSubject(subjectId);
        return subject != null ? subject.getTitle() : "Rewinder";
    }
}
