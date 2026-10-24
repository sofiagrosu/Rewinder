package com.example.myapplication1.stats;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.R;
import com.example.myapplication1.data.RepositoryProvider;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.time.LocalDate;
import java.util.List;

/**
 * "Statistici" screen: success rate (last 7 days), questions per day, weakest subjects.
 * Everything is computed from the repository by StatsCalculator.
 */
public class StatsFragment extends Fragment {

    private static final int DAYS = 7;
    private static final int WEAKEST_SHOWN = 3;

    private CircularProgressIndicator ringAccuracy;
    private TextView txtRingPercent;
    private TextView txtGoodMoments;
    private BarChartView barChart;
    private LinearLayout weakContainer;
    private TextView txtWeakEmpty;

    public StatsFragment() {
        super(R.layout.fragment_stats);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ringAccuracy = view.findViewById(R.id.ringAccuracy);
        txtRingPercent = view.findViewById(R.id.txtRingPercent);
        txtGoodMoments = view.findViewById(R.id.txtGoodMoments);
        barChart = view.findViewById(R.id.barChart);
        weakContainer = view.findViewById(R.id.weakContainer);
        txtWeakEmpty = view.findViewById(R.id.txtWeakEmpty);
    }

    /** Refresh every time the screen comes back, so new answers show up. */
    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Context ctx = requireContext();
        StatsCalculator stats = new StatsCalculator(RepositoryProvider.get());

        // ---- success rate ring ----
        int accuracy = stats.accuracyLastDays(DAYS);
        ringAccuracy.setProgressCompat(accuracy, true);
        txtRingPercent.setText(getString(R.string.percent_format, accuracy));

        int goodMoments = stats.goodMomentsPercent(DAYS);
        if (goodMoments < 0) {
            txtGoodMoments.setText(R.string.good_moments_none);
        } else {
            txtGoodMoments.setText(getString(R.string.good_moments_format, goodMoments));
        }

        // ---- questions per day ----
        String[] initials = getResources().getStringArray(R.array.weekday_initials);
        LocalDate[] dates = stats.lastDates(DAYS);
        String[] labels = new String[DAYS];
        for (int i = 0; i < DAYS; i++) {
            labels[i] = initials[dates[i].getDayOfWeek().getValue() - 1];   // Monday = 1
        }
        barChart.setData(stats.questionsPerDay(DAYS), labels);

        // ---- weakest subjects ----
        weakContainer.removeAllViews();
        List<StatsCalculator.SubjectStat> weakest = stats.weakestSubjects(WEAKEST_SHOWN);
        txtWeakEmpty.setVisibility(weakest.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (StatsCalculator.SubjectStat s : weakest) {
            View item = inflater.inflate(R.layout.item_weak_subject, weakContainer, false);
            ((TextView) item.findViewById(R.id.txtWeakName)).setText(s.subject.getName());
            ((TextView) item.findViewById(R.id.txtWeakPercent))
                    .setText(getString(R.string.percent_format, s.accuracyPercent));

            LinearProgressIndicator bar = item.findViewById(R.id.progressWeak);
            bar.setIndicatorColor(colorForPercent(ctx, s.accuracyPercent));
            bar.setProgressCompat(s.accuracyPercent, true);

            weakContainer.addView(item);
        }
    }

    /** Red under 40%, amber under 70%, green otherwise. */
    private static int colorForPercent(Context ctx, int percent) {
        if (percent < 40) return ContextCompat.getColor(ctx, R.color.danger);
        if (percent < 70) return ContextCompat.getColor(ctx, R.color.warning);
        return ContextCompat.getColor(ctx, R.color.primary);
    }
}
