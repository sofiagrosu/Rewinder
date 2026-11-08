package com.example.myapplication1.quiz.trigger.blocked;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.myapplication1.quiz.settings.QuizSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Busy events from the calendars on the phone (Calendar Provider).
 * A Google account on the phone syncs Google Calendar here automatically, so no Google login,
 * no internet and no Google API are needed: only the READ_CALENDAR permission.
 *
 * PRIVACY: only the start and end time of an event are read. The title (and every other text
 * field) is never requested from the Calendar Provider.
 *
 * Optional (off by default). Counts only events that:
 *   - are marked "Busy" (not "Free"),
 *   - are NOT all-day events (otherwise a birthday would block a whole day),
 *   - were NOT declined by the user,
 *   - belong to the calendars chosen in the settings (none chosen = all calendars).
 * Repeating events (a lecture every Tuesday) come already expanded, from the Instances table.
 */
public class CalendarBusySource implements BlockedTimeSource {

    private static final String TAG = "TRIGGER";

    private final Context context;
    private final QuizSettings settings;

    public CalendarBusySource(Context context, QuizSettings settings) {
        this.context = context.getApplicationContext();
        this.settings = settings;
    }

    @Override
    public List<TimeRange> blockedBetween(long from, long to) {
        List<TimeRange> result = new ArrayList<>();
        if (!settings.isCalendarEnabled()) return result;
        if (!hasPermission(context)) {
            Log.w(TAG, "Calendar is on in settings, but READ_CALENDAR is not granted");
            return result;
        }

        // the Instances table needs the time window in its URI
        Uri.Builder uri = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uri, from);
        ContentUris.appendId(uri, to);

        String[] projection = {
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
        };

        StringBuilder selection = new StringBuilder()
                .append(CalendarContract.Instances.ALL_DAY).append(" = 0")
                .append(" AND ").append(CalendarContract.Instances.AVAILABILITY)
                .append(" = ").append(CalendarContract.Events.AVAILABILITY_BUSY)
                .append(" AND ").append(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                .append(" != ").append(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED);
        Set<Long> calendarIds = settings.getCalendarIds();
        if (!calendarIds.isEmpty()) {
            selection.append(" AND ").append(CalendarContract.Instances.CALENDAR_ID).append(" IN (");
            int i = 0;
            for (Long id : calendarIds) {
                if (i++ > 0) selection.append(',');
                selection.append(id);
            }
            selection.append(')');
        }

        try (Cursor c = context.getContentResolver().query(
                uri.build(), projection, selection.toString(), null, null)) {
            if (c == null) return result;
            while (c.moveToNext()) {
                result.add(new TimeRange(c.getLong(0), c.getLong(1), "Calendar event"));
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Calendar permission was revoked", e);
        }
        return result;
    }

    /** The calendars on the phone (id + name), e.g. for a "choose calendars" list in Settings. */
    public static List<String[]> listCalendars(Context context) {
        List<String[]> result = new ArrayList<>();
        if (!hasPermission(context)) return result;
        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        };
        try (Cursor c = context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)) {
            if (c == null) return result;
            while (c.moveToNext()) {
                result.add(new String[]{String.valueOf(c.getLong(0)), c.getString(1)});
            }
        }
        return result;
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }
}
