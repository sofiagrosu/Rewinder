package com.example.myapplication1.upload;

import android.os.Bundle;
import android.view.View;
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
import com.example.myapplication1.generation.LocalQuestionGenerator;
import com.example.myapplication1.generation.PendingReview;
import com.example.myapplication1.generation.QuestionGenerator;
import com.example.myapplication1.generation.QuestionType;
import com.example.myapplication1.review.ReviewFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * "Încarcă material": the user pastes notes, writes subject + chapter, chooses the question
 * types and taps "Generează întrebări". The questions are NOT saved yet: they go to
 * PendingReview and the Review screen opens, where the user checks them.
 */
public class UploadMaterialFragment extends Fragment {

    private static final int MIN_TEXT_LENGTH = 40;

    /**
     * THE place to switch to the AI later:
     *     private final QuestionGenerator generator = new AiQuestionGenerator(...);
     */
    private final QuestionGenerator generator = new LocalQuestionGenerator();

    private TextInputEditText editSubject;
    private TextInputEditText editChapter;
    private TextInputEditText editText;
    private Chip chipTrueFalse;
    private Chip chipTwoOptions;
    private Chip chipMissingWord;

    public UploadMaterialFragment() {
        super(R.layout.fragment_upload_material);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editSubject = view.findViewById(R.id.editSubject);
        editChapter = view.findViewById(R.id.editChapter);
        editText = view.findViewById(R.id.editText);
        chipTrueFalse = view.findViewById(R.id.chipTrueFalse);
        chipTwoOptions = view.findViewById(R.id.chipTwoOptions);
        chipMissingWord = view.findViewById(R.id.chipMissingWord);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnGenerate).setOnClickListener(v -> generate());
    }

    private void generate() {
        String name = textOf(editSubject);
        String chapter = textOf(editChapter);
        String text = textOf(editText);

        // ---- 1. validate ----
        if (name.isEmpty()) {
            editSubject.setError(getString(R.string.upload_err_subject));
            return;
        }
        if (text.length() < MIN_TEXT_LENGTH) {
            editText.setError(getString(R.string.upload_err_text));
            return;
        }
        Set<QuestionType> types = EnumSet.noneOf(QuestionType.class);
        if (chipTrueFalse.isChecked()) types.add(QuestionType.TRUE_FALSE);
        if (chipTwoOptions.isChecked()) types.add(QuestionType.TWO_OPTIONS);
        if (chipMissingWord.isChecked()) types.add(QuestionType.MISSING_WORD);
        if (types.isEmpty()) {
            toast(R.string.upload_err_types);
            return;
        }

        // ---- 2. the subject: reuse an existing one (same name + chapter) or prepare a new one ----
        QuestionRepository repo = RepositoryProvider.get();
        Subject subject = findSubject(repo, name, chapter);
        if (subject == null) {
            // not saved yet: it is saved together with the questions, when the user approves
            subject = new Subject(repo.nextSubjectId(), name, chapter.isEmpty() ? null : chapter, null);
        }

        // ---- 3. generate ----
        List<Question> questions = generator.generate(text, subject.getId(), types, repo::nextQuestionId);
        if (questions.isEmpty()) {
            toast(R.string.upload_err_nothing);
            return;
        }

        // ---- 4. go to the Review screen ----
        PendingReview.set(subject, questions);
        ((MainActivity) requireActivity()).openScreen(new ReviewFragment());
    }

    /** An existing subject with the same name and chapter (ignoring upper/lower case), or null. */
    private static Subject findSubject(QuestionRepository repo, String name, String chapter) {
        for (Subject s : repo.getSubjects()) {
            String topic = s.getTopic() == null ? "" : s.getTopic();
            if (s.getName().equalsIgnoreCase(name) && topic.equalsIgnoreCase(chapter)) return s;
        }
        return null;
    }

    private static String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(int messageRes) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show();
    }
}
