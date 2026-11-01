package com.example.myapplication1.quiz.trigger.blocked;

import com.example.myapplication1.quiz.settings.QuizSettings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiet hours from the settings (default 22:00 – 08:00), repeated every day.
 * Handles intervals that cross midnight: 22:00 – 08:00 means "from 22:00 until 08:00 the next day".
 */
public class QuietHoursSource implements BlockedTimeSource {

    private final QuizSettings settings;

    public QuietHoursSource(QuizSettings settings) {
        this.settings = settings;
    }

    @Override
    public List<TimeRange> blockedBetween(long from, long to) {
        List<TimeRange> result = new ArrayList<>();
        if (!settings.isQuietHoursEnabled()) return result;

        int startMinute = settings.getQuietStartMinute();
        int endMinute = settings.getQuietEndMinute();
        if (startMinute == endMinute) return result;   // empty interval: nothing blocked

        ZoneId zone = ZoneId.systemDefault();
        // start one day earlier: last night's quiet hours may still be running this morning
        LocalDate day = Instant.ofEpochMilli(from).atZone(zone).toLocalDate().minusDays(1);
        LocalDate lastDay = Instant.ofEpochMilli(to).atZone(zone).toLocalDate();

        while (!day.isAfter(lastDay)) {
            long start = day.atStartOfDay(zone).plusMinutes(startMinute).toInstant().toEpochMilli();
            LocalDate endDay = endMinute > startMinute ? day : day.plusDays(1);   // crosses midnight?
            long end = endDay.atStartOfDay(zone).plusMinutes(endMinute).toInstant().toEpochMilli();
            if (end > from && start < to) {
                result.add(new TimeRange(start, end, "Quiet hours"));
            }
            day = day.plusDays(1);
        }
        return result;
    }
}
