package com.example.myapplication1.quiz.trigger.blocked;

/** A time interval [start, end), in epoch millis, with a short label ("Quiet hours", "Lecture"). */
public class TimeRange {

    private final long start;
    private final long end;
    private final String label;

    public TimeRange(long start, long end, String label) {
        this.start = start;
        this.end = end;
        this.label = label;
    }

    public long getStart() { return start; }
    public long getEnd() { return end; }
    public String getLabel() { return label; }

    /** True if the moment `time` is inside the range (start included, end excluded). */
    public boolean contains(long time) {
        return time >= start && time < end;
    }
}
