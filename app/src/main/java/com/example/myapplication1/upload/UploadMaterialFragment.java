package com.example.myapplication1.upload;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.MainActivity;
import com.example.myapplication1.R;
import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionRepository;
import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.data.Subject;
import com.example.myapplication1.generation.AiConfig;
import com.example.myapplication1.generation.AiQuestionGenerator;
import com.example.myapplication1.generation.Difficulty;
import com.example.myapplication1.generation.GenerationException;
import com.example.myapplication1.generation.GenerationRequest;
import com.example.myapplication1.generation.PendingReview;
import com.example.myapplication1.generation.QuestionGenerator;
import com.example.myapplication1.generation.QuestionType;
import com.example.myapplication1.review.ReviewFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Upload material": the user pastes notes and/or chooses a PDF, writes subject + chapter,
 * and picks the question types, the number of questions and the difficulty.
 * "Generate questions" asks the AI (through our backend); the questions are NOT saved yet:
 * they go to PendingReview and the Review screen opens, where the user checks them.
 */
public class UploadMaterialFragment extends Fragment {

    private static final int MIN_TEXT_LENGTH = 40;

    /** One generation at a time; the network call must not run on the main thread. */
    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();

    private final QuestionGenerator generator = new AiQuestionGenerator();

    private TextInputEditText editSubject;
    private TextInputEditText editChapter;
    private TextInputEditText editText;
    private Chip chipTrueFalse;
    private Chip chipTwoOptions;
    private Chip chipMissingWord;
    private Chip chipCount5;
    private Chip chipCount15;
    private Chip chipEasy;
    private Chip chipHard;
    private MaterialButton btnGenerate;
    private MaterialButton btnRemovePdf;
    private TextView txtPdfName;

    /** The PDF the user chose (read only when generating), or null. */
    private Uri pdfUri;
    private boolean generating;

    private final ActivityResultLauncher<String[]> pdfPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) setPdf(uri);
            });

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
        chipCount5 = view.findViewById(R.id.chipCount5);
        chipCount15 = view.findViewById(R.id.chipCount15);
        chipEasy = view.findViewById(R.id.chipEasy);
        chipHard = view.findViewById(R.id.chipHard);
        btnGenerate = view.findViewById(R.id.btnGenerate);
        btnRemovePdf = view.findViewById(R.id.btnRemovePdf);
        txtPdfName = view.findViewById(R.id.txtPdfName);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnPickPdf).setOnClickListener(v ->
                pdfPicker.launch(new String[]{"application/pdf"}));
        btnRemovePdf.setOnClickListener(v -> clearPdf());
        btnGenerate.setOnClickListener(v -> generate());
    }

    // ---------------------------------------------------------------- PDF

    private void setPdf(Uri uri) {
        pdfUri = uri;
        txtPdfName.setText(displayName(uri));
        btnRemovePdf.setVisibility(View.VISIBLE);
    }

    private void clearPdf() {
        pdfUri = null;
        txtPdfName.setText(R.string.upload_no_pdf);
        btnRemovePdf.setVisibility(View.GONE);
    }

    private String displayName(Uri uri) {
        try (Cursor c = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col >= 0 && c.getString(col) != null) return c.getString(col);
            }
        }
        return "PDF";
    }

    /** Reads the PDF fully; null if it is too big. Runs on the background thread. */
    private static byte[] readPdf(android.content.Context ctx, Uri uri) throws IOException {
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("cannot open");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if (out.size() > AiConfig.MAX_PDF_BYTES) return null;
            }
            return out.toByteArray();
        }
    }

    // ---------------------------------------------------------------- generate

    private void generate() {
        if (generating) return;

        String name = textOf(editSubject);
        String chapter = textOf(editChapter);
        String text = textOf(editText);

        // ---- 1. validate ----
        if (name.isEmpty()) {
            editSubject.setError(getString(R.string.upload_err_subject));
            return;
        }
        if (text.length() < MIN_TEXT_LENGTH && pdfUri == null) {
            editText.setError(getString(R.string.upload_err_need_material));
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
        int count = chipCount5.isChecked() ? AiConfig.COUNT_CHOICES[0]
                : chipCount15.isChecked() ? AiConfig.COUNT_CHOICES[2]
                : AiConfig.COUNT_CHOICES[1];
        Difficulty difficulty = chipEasy.isChecked() ? Difficulty.EASY
                : chipHard.isChecked() ? Difficulty.HARD
                : Difficulty.MEDIUM;

        // ---- 2. the subject: reuse an existing one (same name + chapter) or prepare a new one ----
        QuestionRepository repo = RepositoryProvider.get();
        Subject subject = findSubject(repo, name, chapter);
        if (subject == null) {
            // not saved yet: it is saved together with the questions, when the user approves
            subject = new Subject(repo.nextSubjectId(), name, chapter.isEmpty() ? null : chapter, null);
        }
        final Subject finalSubject = subject;
        final Uri pdf = pdfUri;
        final MainActivity activity = (MainActivity) requireActivity();

        // ---- 3. generate in the background ----
        setGenerating(true);
        BACKGROUND.execute(() -> {
            try {
                byte[] pdfBytes = null;
                if (pdf != null) {
                    pdfBytes = readPdf(activity, pdf);
                    if (pdfBytes == null) throw new GenerationException(
                            GenerationException.Kind.TOO_LARGE, "pdf too big");
                }
                GenerationRequest request = new GenerationRequest(text, pdfBytes, finalSubject.getId(),
                        name, chapter, types, count, difficulty);
                List<Question> questions = generator.generate(request, repo::nextQuestionId);
                activity.runOnUiThread(() -> onGenerated(activity, finalSubject, questions));
            } catch (GenerationException e) {
            Log.e("RewinderAI", "Generation failed: " + e.getMessage(), e);
            activity.runOnUiThread(() -> onFailed(messageFor(e.getKind())));            } catch (IOException e) {
                activity.runOnUiThread(() -> onFailed(R.string.upload_err_pdf_read));
            }
        });
    }

    /** Back on the main thread: open Review (unless the user already left this screen). */
    private void onGenerated(MainActivity activity, Subject subject, List<Question> questions) {
        if (!isAdded()) return;
        setGenerating(false);
        // 4. go to the Review screen; nothing is saved until the user approves
        PendingReview.set(subject, questions);
        activity.openScreen(new ReviewFragment());
    }

    private void onFailed(@StringRes int message) {
        if (!isAdded()) return;
        setGenerating(false);
        toast(message);
    }

    @StringRes
    private static int messageFor(GenerationException.Kind kind) {
        switch (kind) {
            case NETWORK: return R.string.ai_err_network;
            case BUSY: return R.string.ai_err_busy;
            case TOO_LARGE: return R.string.ai_err_too_large;
            case INVALID_RESPONSE: return R.string.ai_err_invalid;
            default: return R.string.ai_err_failed;
        }
    }

    private void setGenerating(boolean value) {
        generating = value;
        btnGenerate.setEnabled(!value);
        btnGenerate.setText(value ? R.string.upload_generating : R.string.upload_generate);
    }

    // ---------------------------------------------------------------- helpers

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

    private void toast(@StringRes int messageRes) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show();
    }
}
