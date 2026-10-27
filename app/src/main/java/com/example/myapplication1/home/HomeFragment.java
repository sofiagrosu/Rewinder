package com.example.myapplication1.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.R;
import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.stats.StatsCalculator;
import com.example.myapplication1.util.RoPlural;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.time.LocalTime;
import java.util.List;

/**
 * Home ("Acasă"). XML (fragment_home.xml) says HOW it looks;
 * this class says WHAT data appears and WHAT happens on taps.
 * All numbers come from the repository through StatsCalculator.
 */
public class HomeFragment extends Fragment {

    // TODO: will come from Settings ("Limită zilnică") when that screen exists.
    private static final int DAILY_GOAL = 20;
    private static final int ACTIVE_SUBJECTS_SHOWN = 2;

    private TextView txtGreeting;
    private TextView txtSolvedToday;
    private TextView txtGoal;
    private LinearProgressIndicator progressGoal;
    private TextView txtStreak;
    private TextView txtAccuracy;
    private LinearLayout subjectsContainer;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---- 1. Bind the XML views ----
        txtGreeting = view.findViewById(R.id.txtGreeting);
        txtSolvedToday = view.findViewById(R.id.txtSolvedToday);
        txtGoal = view.findViewById(R.id.txtGoal);
        progressGoal = view.findViewById(R.id.progressGoal);
        txtStreak = view.findViewById(R.id.txtStreak);
        txtAccuracy = view.findViewById(R.id.txtAccuracy);
        subjectsContainer = view.findViewById(R.id.subjectsContainer);
        MaterialButton btnWhyNow = view.findViewById(R.id.btnWhyNow);
        MaterialButton btnUpload = view.findViewById(R.id.btnUpload);

        // ---- 2. Taps (just a message for now; the other screens belong to teammates) ----
        btnWhyNow.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.toast_why_now, Toast.LENGTH_SHORT).show());
        btnUpload.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.toast_upload, Toast.LENGTH_SHORT).show());
    }

    /** Refresh every time the screen comes back, so answers given meanwhile show up. */
    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    // ---- 3. Put the data on screen ----
    private void refresh() {
        Context ctx = requireContext();
        StatsCalculator stats = new StatsCalculator(RepositoryProvider.get());

        String[] questionForms = getResources().getStringArray(R.array.plural_questions);
        String[] dayForms = getResources().getStringArray(R.array.plural_days);

        int solvedToday = stats.questionsToday();

        txtGreeting.setText(greetingForHour(LocalTime.now().getHour()));
        txtSolvedToday.setText(getString(R.string.solved_today,
                RoPlural.format(questionForms, solvedToday)));

        txtGoal.setText(getString(R.string.goal_format, solvedToday, DAILY_GOAL));
        progressGoal.setMax(DAILY_GOAL);
        progressGoal.setProgressCompat(Math.min(solvedToday, DAILY_GOAL), true);

        txtStreak.setText(RoPlural.format(dayForms, stats.streakDays()));
        txtAccuracy.setText(getString(R.string.percent_format, stats.accuracyToday()));

        // ---- active subjects ----
        subjectsContainer.removeAllViews();
        List<StatsCalculator.SubjectStat> active = stats.recentSubjects(ACTIVE_SUBJECTS_SHOWN);
        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (StatsCalculator.SubjectStat s : active) {
            View item = inflater.inflate(R.layout.item_subject_progress, subjectsContainer, false);

            ((TextView) item.findViewById(R.id.txtSubjectName)).setText(s.subject.getTitle());
            ((TextView) item.findViewById(R.id.txtSubjectSub)).setText(getString(R.string.in_rotation,
                    RoPlural.format(questionForms, s.questionCount)));
            TextView percent = item.findViewById(R.id.txtSubjectPercent);
            if (s.answerCount > 0) {
                percent.setText(getString(R.string.percent_format, s.accuracyPercent));
            } else {
                percent.setText(R.string.no_percent);
            }

            subjectsContainer.addView(item);
        }
    }

    private String greetingForHour(int hour) {
        if (hour >= 5 && hour < 12) return getString(R.string.greeting_morning);
        if (hour >= 12 && hour < 18) return getString(R.string.greeting_day);
        return getString(R.string.greeting_evening);
    }
}
