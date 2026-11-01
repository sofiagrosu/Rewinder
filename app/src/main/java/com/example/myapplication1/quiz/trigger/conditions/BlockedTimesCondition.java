package com.example.myapplication1.quiz.trigger.conditions;

import android.content.Context;
import android.util.Log;

import com.example.myapplication1.quiz.trigger.MomentCondition;
import com.example.myapplication1.quiz.trigger.blocked.BlockedTimeSource;
import com.example.myapplication1.quiz.trigger.blocked.TimeRange;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * VETO: no question while "now" is inside a blocked interval from ANY source
 * (quiet hours, calendar, ...). Adding a source = adding it to the list, nothing else changes.
 */
public class BlockedTimesCondition implements MomentCondition {

    private static final String TAG = "TRIGGER";

    private final List<BlockedTimeSource> sources;

    public BlockedTimesCondition(List<BlockedTimeSource> sources) {
        this.sources = sources;
    }

    @Override
    public boolean allowsNow(Context context, long now) {
        for (BlockedTimeSource source : sources) {
            for (TimeRange range : source.blockedBetween(now, now + 1)) {
                if (range.contains(now)) {
                    Log.d(TAG, "Moment blocked: " + range.getLabel() + " until "
                            + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(range.getEnd())));
                    return false;
                }
            }
        }
        return true;
    }
}
