package com.example.myapplication1.quiz.whynow;

import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.R;
import com.example.myapplication1.data.MomentFeedback;
import com.example.myapplication1.data.RepositoryProvider;
import com.example.myapplication1.quiz.QuizCoordinator;
import com.example.myapplication1.quiz.settings.QuizSettings;
import com.example.myapplication1.quiz.trigger.TriggerStatus;
import com.example.myapplication1.quiz.trigger.blocked.CalendarBusySource;
import com.example.myapplication1.quiz.trigger.blocked.QuietHoursSource;
import com.example.myapplication1.quiz.trigger.blocked.TimeRange;

import java.util.Date;
import java.util.List;

/**
 * "De ce acum?": explains, with the SAME rules the quiz uses, whether this is a good moment.
 *   - movement: BreakDetector's state (TriggerStatus)
 *   - quiet hours: QuietHoursSource
 *   - calendar: CalendarBusySource (only time is shown, never the event title)
 * Plus the reason of the last question sent and the "A fost un moment bun?" Da / Nu feedback.
 */
public class WhyNowFragment extends Fragment {

    private static final String OK = "\u2713";
    private static final String NO = "\u2717";
    private static final String NEUTRAL = "\u2022";

    private TextView txtVerdict;
    private LinearLayout checksContainer;
    private TextView txtLast;

    public WhyNowFragment() {
        super(R.layout.fragment_why_now);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        txtVerdict = view.findViewById(R.id.txtVerdict);
        checksContainer = view.findViewById(R.id.checksContainer);
        txtLast = view.findViewById(R.id.txtLast);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        View btnYes = view.findViewById(R.id.btnYes);
        View btnNo = view.findViewById(R.id.btnNo);
        btnYes.setOnClickListener(v -> saveFeedback(true, btnYes, btnNo));
        btnNo.setOnClickListener(v -> saveFeedback(false, btnYes, btnNo));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        checksContainer.removeAllViews();
        long now = System.currentTimeMillis();
        QuizSettings settings = new QuizSettings(requireContext());
        boolean allGood = true;

        // ---- movement ----
        switch (TriggerStatus.motion) {
            case BREAK:
                long minutes = Math.max(0, (SystemClock.elapsedRealtime() - TriggerStatus.breakStartedAt) / 60_000);
                String since = minutes < 1 ? "sub un minut" : minutes + " min";
                addCheck(OK, getString(R.string.wn_motion_still, since));
                break;
            case PAUSED:
                addCheck(NEUTRAL, getString(R.string.wn_motion_paused));
                break;
            case WALKING:
                addCheck(NO, getString(R.string.wn_motion_walking));
                allGood = false;
                break;
            case STILL_WAITING:
                addCheck(NO, getString(R.string.wn_motion_waiting));
                allGood = false;
                break;
            default:
                addCheck(NO, getString(R.string.wn_motion_unknown));
                allGood = false;
        }

        // ---- quiet hours ----
        if (!settings.isQuietHoursEnabled()) {
            addCheck(OK, getString(R.string.wn_quiet_off));
        } else {
            TimeRange quiet = activeRange(new QuietHoursSource(settings).blockedBetween(now, now + 1), now);
            if (quiet != null) {
                addCheck(NO, getString(R.string.wn_quiet_in, formatTime(quiet.getEnd())));
                allGood = false;
            } else {
                addCheck(OK, getString(R.string.wn_quiet_out));
            }
        }

        // ---- calendar ----
        if (!settings.isCalendarEnabled()) {
            addCheck(OK, getString(R.string.wn_calendar_off));
        } else if (!CalendarBusySource.hasPermission(requireContext())) {
            addCheck(NEUTRAL, getString(R.string.wn_calendar_no_permission));
        } else {
            TimeRange busy = activeRange(
                    new CalendarBusySource(requireContext(), settings).blockedBetween(now, now + 1), now);
            if (busy != null) {
                addCheck(NO, getString(R.string.wn_calendar_busy, formatTime(busy.getEnd())));
                allGood = false;
            } else {
                addCheck(OK, getString(R.string.wn_calendar_free));
            }
        }

        txtVerdict.setText(allGood ? R.string.wn_verdict_good : R.string.wn_verdict_bad);

        // ---- last question sent ----
        QuizCoordinator quiz = QuizCoordinator.getInstance(requireContext());
        if (quiz.getLastMomentAt() == 0) {
            txtLast.setText(R.string.wn_last_none);
        } else {
            txtLast.setText(getString(R.string.wn_last_format,
                    formatTime(quiz.getLastMomentAt()), quiz.getLastMomentReason()));
        }
    }

    private void saveFeedback(boolean good, View btnYes, View btnNo) {
        RepositoryProvider.get().saveMomentFeedback(new MomentFeedback(System.currentTimeMillis(), good));
        btnYes.setEnabled(false);   // one answer per visit
        btnNo.setEnabled(false);
        Toast.makeText(requireContext(), R.string.wn_thanks, Toast.LENGTH_SHORT).show();
    }

    private static TimeRange activeRange(List<TimeRange> ranges, long now) {
        for (TimeRange r : ranges) {
            if (r.contains(now)) return r;
        }
        return null;
    }

    private String formatTime(long millis) {
        return DateFormat.getTimeFormat(requireContext()).format(new Date(millis));
    }

    /** One row: a mark and the explanation. */
    private void addCheck(String mark, String text) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (6 * d), 0, (int) (6 * d));

        TextView markView = new TextView(requireContext());
        markView.setText(mark);
        markView.setTextSize(18);
        markView.setTextColor(requireContext().getColor(mark.equals(NO) ? R.color.danger : R.color.primary));
        markView.setMinWidth((int) (28 * d));

        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(requireContext().getColor(R.color.text_primary));

        row.addView(markView);
        row.addView(textView);
        checksContainer.addView(row);
    }
}
