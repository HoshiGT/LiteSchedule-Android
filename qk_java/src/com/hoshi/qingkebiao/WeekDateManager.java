package com.hoshi.qingkebiao;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeekDateManager {
    private static final String PREFS = "qingkebiao";
    private static final String KEY_START = "semester_start_millis";

    private WeekDateManager() {}

    public static long getStartMillis(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long v = sp.getLong(KEY_START, -1);
        if (v > 0) return v;
        // 默认开学第一周周一：2026-08-31
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2026, Calendar.AUGUST, 31, 0, 0, 0);
        return cal.getTimeInMillis();
    }

    public static void setStartMillis(Context c, long millis) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_START, millis).apply();
    }

    public static int currentWeek(Context c) {
        long start = getStartMillis(c);
        Calendar today = Calendar.getInstance();
        long diff = today.getTimeInMillis() - start;
        if (diff < 0) return 1;
        int days = (int) (diff / (1000L * 60 * 60 * 24));
        return days / 7 + 1;
    }

    public static String dateText(Context c, int week, int day) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getStartMillis(c));
        cal.add(Calendar.DAY_OF_YEAR, (week - 1) * 7 + (day - 1));
        SimpleDateFormat df = new SimpleDateFormat("MM-dd", Locale.CHINA);
        return df.format(new Date(cal.getTimeInMillis()));
    }
}
