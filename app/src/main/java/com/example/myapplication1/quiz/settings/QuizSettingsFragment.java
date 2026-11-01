package com.example.myapplication1.quiz.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.R;
import com.example.myapplication1.quiz.trigger.blocked.BlockedTimeSource;
import com.example.myapplication1.quiz.trigger.blocked.CalendarBusySource;
import com.example.myapplication1.quiz.trigger.blocked.QuietHoursSource;
import com.example.myapplication1.quiz.trigger.blocked.TimeRange;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * TEMPORARY settings screen, to test the quiz: quiet hours + calendar.
 * Everything is saved through QuizSettings, so the team's real Settings screen can replace
 * this fragment without changing anything in the quiz.
 * Changes apply immediately: the quiz reads the settings at every moment it detects.
 */
public class QuizSettingsFragment extends Fragment {

    private QuizSettings settings;

    private MaterialSwitch switchQuietHours;
    private MaterialButton btnQuietStart;
    private MaterialButton btnQuietEnd;
    private MaterialSwitch switchCalendar;
    private LinearLayout calendarList;
    private TextView txtStatus;

    // asks for the calendar permission when the user turns the calendar on
    private final ActivityResultLauncher<String> calendarPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    settings.setCalendarEnabled(true);
                } else {
                    switchCalendar.setChecked(false);   // can't work without the permission
                    Toast.makeText(requireContext(), R.string.qs_calendar_denied, Toast.LENGTH_LONG).show();
                }
                refresh();
            });

    public QuizSettingsFragment() {
        super(R.layout.fragment_quiz_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings = new QuizSettings(requireContext());

        switchQuietHours = view.findViewById(R.id.switchQuietHours);
        btnQuietStart = view.findViewById(R.id.btnQuietStart);
        btnQuietEnd = view.findViewById(R.id.btnQuietEnd);
        switchCalendar = view.findViewById(R.id.switchCalendar);
        calendarList = view.findViewById(R.id.calendarList);
        txtStatus = view.findViewById(R.id.txtStatus);

        // ---- quiet hours ----
        switchQuietHours.setChecked(settings.isQuietHoursEnabled());
        switchQuietHours.setOnCheckedChangeListener((button, checked) -> {
            settings.setQuietHoursEnabled(checked);
            refresh();
        });
        btnQuietStart.setOnClickListener(v -> pickTime(true));
        btnQuietEnd.setOnClickListener(v -> pickTime(false));

        // ---- calendar ----
        // on only if it was turned on AND the permission is still there
        switchCalendar.setChecked(settings.isCalendarEnabled() && CalendarBusySource.hasPermission(requireContext()));
        switchCalendar.setOnCheckedChangeListener((button, checked) -> {
            if (!checked) {
                settings.setCalendarEnabled(false);
                refresh();
            } else if (CalendarBusySource.hasPermission(requireContext())) {
                settings.setCalendarEnabled(true);
                refresh();
            } else {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR);
            }
        });

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (txtStatus != null) refresh();   // e.g. the time changed while the screen was hidden
    }

    // ---------------------------------------------------------------- UI updates

    private void refresh() {
        boolean quietOn = settings.isQuietHoursEnabled();
        btnQuietStart.setEnabled(quietOn);
        btnQuietEnd.setEnabled(quietOn);
        btnQuietStart.setText(getString(R.string.qs_from, formatMinute(settings.getQuietStartMinute())));
        btnQuietEnd.setText(getString(R.string.qs_to, formatMinute(settings.getQuietEndMinute())));

        showCalendars(settings.isCalendarEnabled() && CalendarBusySource.hasPermission(requireContext()));
        showStatus();
    }

    /** One checkbox per calendar on the phone. Nothing saved yet = all calendars are used. */
    private void showCalendars(boolean visible) {
        calendarList.removeAllViews();
        if (!visible) return;

        List<String[]> calendars = CalendarBusySource.listCalendars(requireContext());
        TextView header = new TextView(requireContext());
        header.setText(calendars.isEmpty() ? R.string.qs_calendar_none : R.string.qs_calendar_pick);
        header.setTextColor(requireContext().getColor(R.color.text_primary));
        calendarList.addView(header);

        Set<Long> chosen = settings.getCalendarIds();
        for (String[] calendar : calendars) {
            long id = Long.parseLong(calendar[0]);
            MaterialCheckBox box = new MaterialCheckBox(requireContext());
            box.setText(calendar[1]);
            box.setTag(id);
            box.setChecked(chosen.isEmpty() || chosen.contains(id));
            box.setOnCheckedChangeListener((b, checked) -> onCalendarChecked(b, checked));
            calendarList.addView(box);
        }
    }

    private void onCalendarChecked(android.widget.CompoundButton box, boolean checked) {
        Set<Long> selected = new HashSet<>();
        int total = 0;
        for (int i = 0; i < calendarList.getChildCount(); i++) {
            View child = calendarList.getChildAt(i);
            if (child instanceof MaterialCheckBox) {
                total++;
                if (((MaterialCheckBox) child).isChecked()) selected.add((Long) child.getTag());
            }
        }
        if (selected.isEmpty()) {
            // "none saved" means "all calendars", so we don't allow unchecking the last one
            box.setChecked(true);
            Toast.makeText(requireContext(), R.string.qs_calendar_keep_one, Toast.LENGTH_SHORT).show();
            return;
        }
        // all checked → save "nothing" = all calendars (also includes calendars added later)
        settings.setCalendarIds(selected.size() == total ? new HashSet<>() : selected);
        showStatus();
    }

    /** "Right now": is a question allowed, or which blocked interval is active? */
    private void showStatus() {
        long now = System.currentTimeMillis();
        List<BlockedTimeSource> sources = Arrays.asList(
                new QuietHoursSource(settings),
                new CalendarBusySource(requireContext(), settings));
        for (BlockedTimeSource source : sources) {
            for (TimeRange range : source.blockedBetween(now, now + 1)) {
                if (range.contains(now)) {
                    txtStatus.setText(getString(R.string.qs_status_blocked, range.getLabel(),
                            DateFormat.getTimeFormat(requireContext()).format(range.getEnd())));
                    return;
                }
            }
        }
        txtStatus.setText(R.string.qs_status_allowed);
    }

    // ---------------------------------------------------------------- time picking

    private void pickTime(boolean start) {
        int minute = start ? settings.getQuietStartMinute() : settings.getQuietEndMinute();
        new TimePickerDialog(requireContext(), (picker, hour, min) -> {
            int picked = hour * 60 + min;
            if (start) {
                settings.setQuietHours(picked, settings.getQuietEndMinute());
            } else {
                settings.setQuietHours(settings.getQuietStartMinute(), picked);
            }
            refresh();
        }, minute / 60, minute % 60, DateFormat.is24HourFormat(requireContext())).show();
    }

    private static String formatMinute(int minuteOfDay) {
        return String.format(Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }
}
