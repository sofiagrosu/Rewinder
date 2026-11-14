package com.example.myapplication1.subjects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.MainActivity;
import com.example.myapplication1.R;
import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.stats.StatsCalculator;
import com.example.myapplication1.upload.UploadMaterialFragment;
import com.example.myapplication1.util.RoPlural;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * "Materii" tab: one card per subject (name, exam date, number of questions, accuracy)
 * and a button that opens the "Încarcă material" screen.
 * The numbers come from StatsCalculator.subjectStats(), the same source as Home and Stats.
 */
public class SubjectsFragment extends Fragment {

    private static final DateTimeFormatter EXAM_FORMAT =
            DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private LinearLayout subjectsList;
    private TextView txtEmpty;

    public SubjectsFragment() {
        super(R.layout.fragment_subjects);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subjectsList = view.findViewById(R.id.subjectsList);
        txtEmpty = view.findViewById(R.id.txtEmpty);
        view.findViewById(R.id.btnAddMaterial).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openScreen(new UploadMaterialFragment()));
    }

    /** Refresh every time the screen comes back (e.g. after new questions were approved). */
    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        StatsCalculator stats = new StatsCalculator(RepositoryProvider.get());
        List<StatsCalculator.SubjectStat> all = stats.subjectStats();
        String[] questionForms = getResources().getStringArray(R.array.plural_questions);

        subjectsList.removeAllViews();
        txtEmpty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (StatsCalculator.SubjectStat s : all) {
            View item = inflater.inflate(R.layout.item_subject, subjectsList, false);

            ((TextView) item.findViewById(R.id.txtSubjectName)).setText(s.subject.getTitle());

            // "Examen: 12 nov · 24 de întrebări"  (the exam part only if a date is set)
            String count = RoPlural.format(questionForms, s.questionCount);
            String sub = count;
            if (s.subject.getExamDate() != null) {
                String exam = getString(R.string.subject_exam_format, EXAM_FORMAT.format(s.subject.getExamDate()));
                sub = getString(R.string.subject_sub_format, exam, count);
            }
            ((TextView) item.findViewById(R.id.txtSubjectSub)).setText(sub);

            TextView percent = item.findViewById(R.id.txtSubjectPercent);
            LinearProgressIndicator bar = item.findViewById(R.id.progressSubject);
            if (s.answerCount > 0) {
                percent.setText(getString(R.string.percent_format, s.accuracyPercent));
                bar.setProgressCompat(s.accuracyPercent, false);
            } else {
                percent.setText(R.string.no_percent);       // never answered: no percent, empty bar
                bar.setProgressCompat(0, false);
            }
            subjectsList.addView(item);
        }
    }
}
