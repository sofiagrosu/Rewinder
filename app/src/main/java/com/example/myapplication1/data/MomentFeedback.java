package com.example.myapplication1.data;

/**
 * The user's answer to "A fost un moment bun? Da / Nu" (screen "De ce acum?").
 * Stats shows the share of "good" moments ("Momente bune: 71%").
 */
public class MomentFeedback {

    private final long timestampMillis;
    private final boolean good;

    public MomentFeedback(long timestampMillis, boolean good) {
        this.timestampMillis = timestampMillis;
        this.good = good;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public boolean isGood() {
        return good;
    }
}

