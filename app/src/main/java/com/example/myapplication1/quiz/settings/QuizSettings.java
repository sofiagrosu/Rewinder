package com.example.myapplication1.quiz.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication1.quiz.trigger.TriggerConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * The user's quiz settings, saved on the phone (SharedPreferences, survives restarts).
 * Read by the quiz (QuietHoursSource, CalendarBusySource) and, later, written by the Settings screen.
 *
 * Times of day are stored as "minutes after midnight": 22:00 → 22 * 60 = 1320.
 * Defaults: see TriggerConfig.
 */
public class QuizSettings {

    private static final String FILE = "quiz_settings";

    private static final String QUIET_ENABLED = "quiet_hours_enabled";
    private static final String QUIET_START = "quiet_hours_start_minute";
    private static final String QUIET_END = "quiet_hours_end_minute";
    private static final String CALENDAR_ENABLED = "calendar_enabled";
    private static final String CALENDAR_IDS = "calendar_ids";
    private static final String DAILY_GOAL = "daily_goal";
    private static final String MONITORING = "monitoring_enabled";

    private final SharedPreferences prefs;

    public QuizSettings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- quiet hours

    public boolean isQuietHoursEnabled() {
        return prefs.getBoolean(QUIET_ENABLED, TriggerConfig.DEFAULT_QUIET_HOURS_ENABLED);
    }

    public void setQuietHoursEnabled(boolean enabled) {
        prefs.edit().putBoolean(QUIET_ENABLED, enabled).apply();
    }

    /** Minutes after midnight, e.g. 1320 = 22:00. */
    public int getQuietStartMinute() {
        return prefs.getInt(QUIET_START, TriggerConfig.DEFAULT_QUIET_START_HOUR * 60);
    }

    /** Minutes after midnight, e.g. 480 = 08:00. */
    public int getQuietEndMinute() {
        return prefs.getInt(QUIET_END, TriggerConfig.DEFAULT_QUIET_END_HOUR * 60);
    }

    /** E.g. setQuietHours(1 * 60, 9 * 60) = 01:00 – 09:00 (while studying late). */
    public void setQuietHours(int startMinute, int endMinute) {
        prefs.edit().putInt(QUIET_START, startMinute).putInt(QUIET_END, endMinute).apply();
    }

    // ---------------------------------------------------------------- monitoring on/off

    /**
     * The user's own on/off switch for the whole quiz monitoring (default: on).
     * False after "Stop" in the service notification or the switch in Settings; the service
     * is then NOT restarted by the app opening or by the phone rebooting.
     */
    public boolean isMonitoringEnabled() {
        return prefs.getBoolean(MONITORING, true);
    }

    public void setMonitoringEnabled(boolean enabled) {
        prefs.edit().putBoolean(MONITORING, enabled).apply();
    }

    // ---------------------------------------------------------------- daily goal

    /** Questions the user wants to answer per day (Home shows the progress towards it). */
    public int getDailyGoal() {
        return prefs.getInt(DAILY_GOAL, TriggerConfig.DEFAULT_DAILY_GOAL);
    }

    public void setDailyGoal(int goal) {
        int clamped = Math.max(TriggerConfig.MIN_DAILY_GOAL, Math.min(TriggerConfig.MAX_DAILY_GOAL, goal));
        prefs.edit().putInt(DAILY_GOAL, clamped).apply();
    }

    // ---------------------------------------------------------------- calendar

    public boolean isCalendarEnabled() {
        return prefs.getBoolean(CALENDAR_ENABLED, TriggerConfig.DEFAULT_CALENDAR_ENABLED);
    }

    public void setCalendarEnabled(boolean enabled) {
        prefs.edit().putBoolean(CALENDAR_ENABLED, enabled).apply();
    }

    /** Ids of the calendars that count as "busy". Empty = all calendars on the phone. */
    public Set<Long> getCalendarIds() {
        Set<Long> ids = new HashSet<>();
        for (String id : prefs.getStringSet(CALENDAR_IDS, new HashSet<>())) {
            ids.add(Long.parseLong(id));
        }
        return ids;
    }

    public void setCalendarIds(Set<Long> calendarIds) {
        Set<String> ids = new HashSet<>();
        for (Long id : calendarIds) ids.add(String.valueOf(id));
        prefs.edit().putStringSet(CALENDAR_IDS, ids).apply();
    }
}
