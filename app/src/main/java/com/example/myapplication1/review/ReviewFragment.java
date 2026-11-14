package com.example.myapplication1.review;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.MainActivity;
import com.example.myapplication1.R;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionRepository;
import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.data.Subject;
import com.example.myapplication1.generation.PendingReview;
import com.example.myapplication1.generation.QuestionType;
import com.example.myapplication1.util.RoPlural;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * "Verificare": shows the questions waiting in PendingReview. For each one the user can edit
 * or delete it. "Aprobă toate" saves the subject (if new) and all remaining questions into the
 * repository, from where the quiz takes them. "Anulează" throws everything away.
 */
public class ReviewFragment extends Fragment {

    private TextView txtCount;
    private TextView txtEmpty;
    private LinearLayout reviewList;
    private MaterialButton btnApproveAll;

    public ReviewFragment() {
        super(R.layout.fragment_review);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        txtCount = view.findViewById(R.id.txtReviewCount);
        txtEmpty = view.findViewById(R.id.txtReviewEmpty);
        reviewList = view.findViewById(R.id.reviewList);
        btnApproveAll = view.findViewById(R.id.btnApproveAll);

        btnApproveAll.setOnClickListener(v -> approveAll());
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            PendingReview.clear();
            ((MainActivity) requireActivity()).closeSubScreens();
        });

        render();
    }

    // ------------------------------------------------------------------ screen

    /** Redraws the whole list (simple and safe for ~12 items). */
    private void render() {
        List<Question> pending = PendingReview.getQuestions();

        txtCount.setText(getString(R.string.review_count_format, pending.size()));
        txtEmpty.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);
        btnApproveAll.setEnabled(!pending.isEmpty());

        reviewList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Question q : new ArrayList<>(pending)) {
            View item = inflater.inflate(R.layout.item_review_question, reviewList, false);

            ((TextView) item.findViewById(R.id.txtType)).setText(typeLabel(QuestionType.of(q)));
            ((TextView) item.findViewById(R.id.txtQuestion)).setText(q.getText());
            ((TextView) item.findViewById(R.id.txtAnswer)).setText(
                    getString(R.string.review_answer_format, q.getOption(q.getCorrectOption())));

            item.findViewById(R.id.btnEdit).setOnClickListener(v -> showEditDialog(q));
            item.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                PendingReview.getQuestions().remove(q);
                render();
            });
            reviewList.addView(item);
        }
    }

    private String typeLabel(QuestionType type) {
        switch (type) {
            case TRUE_FALSE: return getString(R.string.type_label_true_false);
            case TWO_OPTIONS: return getString(R.string.type_label_two_options);
            default: return getString(R.string.type_label_missing_word);
        }
    }

    // ------------------------------------------------------------------ edit

    /** Dialog with the question text, the options and "which one is correct". */
    private void showEditDialog(Question q) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_question, null);
        EditText editText = form.findViewById(R.id.editQuestionText);
        EditText[] options = {
                form.findViewById(R.id.editOption1),
                form.findViewById(R.id.editOption2),
                form.findViewById(R.id.editOption3)};
        RadioButton[] radios = {
                form.findViewById(R.id.radio1),
                form.findViewById(R.id.radio2),
                form.findViewById(R.id.radio3)};

        int optionCount = q.getOptionCount();               // 2 or 3
        form.findViewById(R.id.rowOption3).setVisibility(optionCount == 3 ? View.VISIBLE : View.GONE);

        editText.setText(q.getText());
        for (int i = 0; i < optionCount; i++) {
            options[i].setText(q.getOption(i + 1));
            options[i].setHint(getString(R.string.edit_option_hint, i + 1));
            radios[i].setChecked(q.getCorrectOption() == i + 1);
            final int index = i;
            // the 3 radios are in different rows, so we make them exclusive ourselves
            radios[i].setOnClickListener(v -> {
                for (int j = 0; j < radios.length; j++) radios[j].setChecked(j == index);
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_title)
                .setView(form)
                .setNegativeButton(R.string.review_cancel, null)
                .setPositiveButton(R.string.edit_save, null)   // set below, so we can keep it open on error
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newText = editText.getText().toString().trim();
            String[] newOptions = new String[3];
            int correct = 1;
            boolean ok = !newText.isEmpty();
            for (int i = 0; i < optionCount; i++) {
                newOptions[i] = options[i].getText().toString().trim();
                if (newOptions[i].isEmpty()) ok = false;
                if (radios[i].isChecked()) correct = i + 1;
            }
            if (!ok) {
                Toast.makeText(requireContext(), R.string.edit_err_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            // Question is immutable: build a new one with the same id and put it in the same place
            Question edited = new Question(q.getId(), q.getSubjectId(), newText,
                    newOptions[0], newOptions[1], optionCount == 3 ? newOptions[2] : null,
                    correct, q.getExplanation());
            List<Question> list = PendingReview.getQuestions();
            int position = list.indexOf(q);
            if (position >= 0) list.set(position, edited);
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    // ------------------------------------------------------------------ approve

    private void approveAll() {
        QuestionRepository repo = RepositoryProvider.get();
        Subject subject = PendingReview.getSubject();
        List<Question> approved = new ArrayList<>(PendingReview.getQuestions());
        if (subject == null || approved.isEmpty()) return;

        if (repo.getSubject(subject.getId()) == null) {
            repo.saveSubject(subject);                       // new subject: save it first
        }
        for (Question q : approved) {
            repo.saveQuestion(q);                            // from now on the quiz can ask it
        }

        String[] questionForms = getResources().getStringArray(R.array.plural_questions);
        Toast.makeText(requireContext(),
                getString(R.string.review_approved_format, RoPlural.format(questionForms, approved.size())),
                Toast.LENGTH_LONG).show();

        PendingReview.clear();
        ((MainActivity) requireActivity()).closeSubScreens();
    }
}
