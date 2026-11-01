package com.example.myapplication1.data;

import java.util.function.Supplier;

/**
 * Where a feature asks for the summary stats, without importing the stats package:
 *
 *     DailyStats stats = StatsProvider.today();   // null if nobody registered an implementation
 *
 * The implementation is plugged in ONCE, at app start, by RewinderApp:
 *     StatsProvider.set(() -> new StatsCalculator(RepositoryProvider.get()));
 *
 * It stores a factory, not an object, because a calculator fixes "today" when it is created;
 * every call gets a fresh one, so the numbers are always current (also after midnight).
 */
public final class StatsProvider {

    private static Supplier<? extends DailyStats> factory;

    private StatsProvider() {
    }

    public static synchronized void set(Supplier<? extends DailyStats> newFactory) {
        factory = newFactory;
    }

    /** Fresh numbers for right now, or null if no implementation was registered. */
    public static synchronized DailyStats today() {
        return factory != null ? factory.get() : null;
    }
}
