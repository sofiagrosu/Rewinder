package com.example.myapplication1.quiz.ui;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.myapplication1.R;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.quiz.QuizCoordinator;
import com.example.myapplication1.quiz.delivery.QuestionNotifier;

import java.util.Locale;

/**
 * The in-app version of the question notification: a card at the top of MainActivity
 * (res/layout/view_question_banner.xml) showing the question that waits for an answer,
 * with its option buttons, and afterwards the result (✓ / ✗ + explanation + OK).
 *
 * It is shown whenever a question is pending — so also when the app is opened by tapping
 * the notification — and hides when the question is answered (here, from the notification
 * or from the watch) or when its protection time runs out.
 *
 * Display only: an answer given here goes through QuizCoordinator.onAnswer, exactly like a
 * notification button, so the progress, the history and the result notification are the same.
 *
 * Usage in MainActivity: new QuestionBanner(findViewById(R.id.questionBanner)),
 * then start() in onStart() and stop() in onStop().
 */
public final class QuestionBanner implements QuizCoordinator.PendingListener {

    private final View card;
    private final TextView label;        // subject title, or the result headline
    private final TextView reason;       // "why now"
    private final TextView text;         // the question, or the explanation
    private final View optionsRow;
    private final Button[] optionButtons;
    private final Button okButton;

    private final QuizCoordinator coordinator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable expiryCheck = this::refresh;

    private long shownQuestionId = -1;   // what the card displays now (-1 = hidden)
    private boolean showingResult;       // true while the card shows ✓/✗ instead of a question

    public QuestionBanner(View bannerView) {
        card = bannerView;
        label = bannerView.findViewById(R.id.txtBannerLabel);
        reason = bannerView.findViewById(R.id.txtBannerReason);
        text = bannerView.findViewById(R.id.txtBannerText);
        optionsRow = bannerView.findViewById(R.id.bannerOptions);
        optionButtons = new Button[] {
                bannerView.findViewById(R.id.btnOption1),
                bannerView.findViewById(R.id.btnOption2),
                bannerView.findViewById(R.id.btnOption3)};
        okButton = bannerView.findViewById(R.id.btnBannerOk);
        coordinator = QuizCoordinator.getInstance(bannerView.getContext());

        for (int i = 0; i < optionButtons.length; i++) {
            final int option = i + 1;              // buttons are 0-based, options 1-based
            optionButtons[i].setOnClickListener(v -> answer(option));
        }
        okButton.setOnClickListener(v -> hide());
    }

    /** Activity.onStart: follow the coordinator and show what is pending right now. */
    public void start() {
        coordinator.setPendingListener(this);
        refresh();
    }

    /** Activity.onStop: stop following; nothing to draw while the app is off screen. */
    public void stop() {
        coordinator.setPendingListener(null);
        handler.removeCallbacks(expiryCheck);
    }

    @Override
    public void onPendingChanged() {
        handler.post(this::refresh);   // the call may come from the receiver: back to the UI thread
    }

    /** Lines the card up with the coordinator: pending question → show it; none → hide (a result stays). */
    private void refresh() {
        Question pending = coordinator.getPendingQuestion();
        if (pending != null) {
            if (pending.getId() != shownQuestionId || showingResult) showQuestion(pending);
        } else if (!showingResult) {
            hide();
        }
    }

    private void showQuestion(Question question) {
        shownQuestionId = question.getId();
        showingResult = false;
        label.setText(coordinator.getSubjectTitle(question.getSubjectId()).toUpperCase(Locale.ROOT));
        label.setTextColor(color(R.color.primary));
        setOptionalText(reason, coordinator.getPendingReason());
        text.setText(question.getText());
        for (int i = 0; i < optionButtons.length; i++) {
            int option = i + 1;
            boolean exists = option <= question.getOptionCount();   // True/False has only 2
            optionButtons[i].setVisibility(exists ? View.VISIBLE : View.GONE);
            if (exists) optionButtons[i].setText(question.getOption(option));
        }
        optionsRow.setVisibility(View.VISIBLE);
        okButton.setVisibility(View.GONE);
        card.setVisibility(View.VISIBLE);
        // hides by itself when the protection time is over (the moment the notification disappears)
        handler.removeCallbacks(expiryCheck);
        handler.postDelayed(expiryCheck, coordinator.getPendingRemainingMillis() + 100);
    }

    private void answer(int option) {
        Question question = coordinator.getPendingQuestion();
        if (question == null || question.getId() != shownQuestionId) {
            hide();                                // expired or already answered elsewhere
            return;
        }
        boolean correct = question.isCorrect(option);
        coordinator.onAnswer(question.getId(), option);   // same path as the notification buttons
        showResult(question, correct);
    }

    private void showResult(Question question, boolean correct) {
        showingResult = true;
        handler.removeCallbacks(expiryCheck);
        label.setText(QuestionNotifier.resultHeadline(question, correct));
        label.setTextColor(color(correct ? R.color.primary : R.color.danger));
        reason.setVisibility(View.GONE);
        setOptionalText(text, question.getExplanation());
        optionsRow.setVisibility(View.GONE);
        okButton.setVisibility(View.VISIBLE);
    }

    private void hide() {
        shownQuestionId = -1;
        showingResult = false;
        handler.removeCallbacks(expiryCheck);
        card.setVisibility(View.GONE);
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(card.getContext(), colorRes);
    }

    private static void setOptionalText(TextView view, String value) {
        boolean has = value != null && !value.isEmpty();
        view.setVisibility(has ? View.VISIBLE : View.GONE);
        if (has) view.setText(value);
    }
}
