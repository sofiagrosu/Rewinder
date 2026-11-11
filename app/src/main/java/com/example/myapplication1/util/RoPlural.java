package com.example.myapplication1.util;

import java.util.Locale;

/**
 * Picks one of three plural forms {one, few, other} (rule written for Romanian: 1 / 2-19 / 20+).
 * The app is now in English, so the forms in strings_home_stats.xml are
 * {"%d question", "%d questions", "%d questions"}: the result is correct English.
 *
 * Usage (forms come from strings_home_stats.xml, e.g. R.array.plural_questions):
 *     String[] forms = getResources().getStringArray(R.array.plural_questions);
 *     String text = RoPlural.format(forms, 14);   // "14 questions"
 */
public final class RoPlural {

    private RoPlural() {
    }

    /** 0 = one ("1 zi"), 1 = few ("6 zile"), 2 = other ("20 de zile"). */
    public static int formIndex(int n) {
        if (n == 1) return 0;
        int lastTwo = Math.abs(n) % 100;
        if (n == 0 || (lastTwo >= 2 && lastTwo <= 19)) return 1;
        return 2;
    }

    /** forms = {one, few, other}, each containing %d. */
    public static String format(String[] forms, int n) {
        return String.format(Locale.ROOT, forms[formIndex(n)], n);
    }
}
