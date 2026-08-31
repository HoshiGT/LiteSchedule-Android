package com.hoshi.qingkebiao;

import android.content.Context;
import android.content.SharedPreferences;

public class CurrentWeekManager {
    private static final String PREFS = "qingkebiao";
    private static final String KEY_WEEK = "current_week";

    private CurrentWeekManager() {}

    public static int get(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WEEK, 1);
    }

    public static void set(Context c, int week) {
        if (week < 1) week = 1;
        if (week > 30) week = 30;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_WEEK, week).apply();
    }
}
