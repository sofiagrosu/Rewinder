package com.example.myapplication1.quiz.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication1.R;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.quiz.QuizCoordinator;
import com.example.myapplication1.quiz.delivery.QuestionNotifier;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * In-app practice set: SET_SIZE questions one after the other, whenever the user wants
 * (e.g. on a busy day, to keep the daily goal and the streak).
 *
 * The question waiting in the notification (if any) comes first; the others are chosen by the
 * normal ordering. Answers go through QuizCoordinator.onAnswerInApp, so they are saved exactly
 * like notification answers (history for Home/Stats + Leitner progress).
 *
 * Opened from Home ("Practice now · 3 questions").
 */
public class PracticeActivity extends AppCompatActivity {

    private static final int SET_SIZE = 3;

    private QuizCoordinator coordinator;
    private final Set<Long> asked = new HashSet<>();
    private int questionNumber = 0;   // 1-based, of the question on screen
    private int correctCount = 0;
    private Question current;

    private TextView txtProgress;
    private TextView txtLabel;
    private TextView txtText;
    private View optionsBox;
    private Button[] optionButtons;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_practice);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.practiceRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        coordinator = QuizCoordinator.getInstance(this);
        txtProgress = findViewById(R.id.txtPracticeProgress);
        txtLabel = findViewById(R.id.txtPracticeLabel);
        txtText = findViewById(R.id.txtPracticeText);
        optionsBox = findViewById(R.id.practiceOptions);
        optionButtons = new Button[] {
                findViewById(R.id.btnPracticeOption1),
                findViewById(R.id.btnPracticeOption2),
                findViewById(R.id.btnPracticeOption3)};
        btnNext = findViewById(R.id.btnPracticeNext);

        for (int i = 0; i < optionButtons.length; i++) {
            final int option = i + 1;   // buttons 0-based, options 1-based
            optionButtons[i].setOnClickListener(v -> answer(option));
        }
        showNextQuestion();
    }

    private void showNextQuestion() {
        current = coordinator.nextPracticeQuestion(asked);
        if (current == null) {
            showSummary();
            return;
        }
        asked.add(current.getId());
        questionNumber++;

        txtProgress.setText(getString(R.string.practice_progress, questionNumber, SET_SIZE));
        txtLabel.setText(coordinator.getSubjectTitle(current.getSubjectId()).toUpperCase(Locale.ROOT));
        txtLabel.setTextColor(ContextCompat.getColor(this, R.color.primary));
        txtText.setText(current.getText());
        for (int i = 0; i < optionButtons.length; i++) {
            int option = i + 1;
            boolean exists = option <= current.getOptionCount();   // True/False has only 2
            optionButtons[i].setVisibility(exists ? View.VISIBLE : View.GONE);
            if (exists) optionButtons[i].setText(current.getOption(option));
        }
        optionsBox.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
    }

    private void answer(int option) {
        boolean correct = current.isCorrect(option);
        if (correct) correctCount++;
        coordinator.onAnswerInApp(current.getId(), option);   // saved like a notification answer

        txtLabel.setText(QuestionNotifier.resultHeadline(current, correct));
        txtLabel.setTextColor(ContextCompat.getColor(this, correct ? R.color.primary : R.color.danger));
        String explanation = current.getExplanation();
        txtText.setText(explanation != null && !explanation.isEmpty() ? explanation : "");
        optionsBox.setVisibility(View.GONE);

        boolean last = questionNumber >= SET_SIZE;
        btnNext.setText(last ? R.string.practice_finish : R.string.practice_next);
        btnNext.setOnClickListener(v -> {
            if (last) showSummary();
            else showNextQuestion();
        });
        btnNext.setVisibility(View.VISIBLE);
    }

    private void showSummary() {
        optionsBox.setVisibility(View.GONE);
        txtProgress.setText("");
        if (questionNumber == 0) {
            txtLabel.setText("");
            txtText.setText(R.string.practice_nothing);
        } else {
            txtLabel.setText(getString(R.string.practice_done_title).toUpperCase(Locale.ROOT));
            txtLabel.setTextColor(ContextCompat.getColor(this, R.color.primary));
            String score = getString(R.string.practice_done_score, correctCount, questionNumber);
            String stats = coordinator.statsLine();   // "Today: 12 answered · ..."
            txtText.setText(stats != null ? score + "\n\n" + stats : score);
        }
        btnNext.setText(R.string.practice_close);
        btnNext.setOnClickListener(v -> finish());   // back to Home, which refreshes in onResume
        btnNext.setVisibility(View.VISIBLE);
    }
}
