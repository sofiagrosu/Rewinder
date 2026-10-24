package com.example.myapplication1.util;

import java.util.Locale;

/**
 * Romanian plurals: "1 întrebare", "14 întrebări", "20 de întrebări".
 *
 * Android's own plurals depend on the phone's language, so they would be wrong
 * on a phone set to English. This helper always uses the Romanian rule.
 *
 * Usage (forms come from strings_home_stats.xml, e.g. R.array.plural_questions):
 *     String[] forms = getResources().getStringArray(R.array.plural_questions);
 *     String text = RoPlural.format(forms, 14);   // "14 întrebări"
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
