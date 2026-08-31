package com.hoshi.qingkebiao;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 默认节次时间表（单位：分钟）。
 * 下标即节次，0 不使用。
 * 支持用户在设置里自定义每节课的开始时间。
 */
public class TimeTable {
    private static final String PREFS = "qingkebiao";
    private static final String KEY_PREFIX = "section_start_";

    public static final int[] DEFAULT_START_MINUTES = {
            0,
            8 * 60,       // 第1节 08:00
            9 * 60,       // 第2节 09:00
            10 * 60 + 10, // 第3节 10:10
            11 * 60 + 10, // 第4节 11:10
            12 * 60 + 10, // 第5节 12:10
            13 * 60,      // 第6节 13:00
            14 * 60 + 10, // 第7节 14:10
            15 * 60 + 10, // 第8节 15:10
            16 * 60 + 10, // 第9节 16:10
            17 * 60 + 10, // 第10节 17:10
            18 * 60,      // 第11节 18:00
            19 * 60,      // 第12节 19:00
            20 * 60       // 第13节 20:00
    };

    public static int startMinute(int section) {
        if (section <= 0 || section >= DEFAULT_START_MINUTES.length) {
            return 8 * 60;
        }
        return DEFAULT_START_MINUTES[section];
    }

    public static int startMinute(Context context, int section) {
        if (section <= 0 || section >= DEFAULT_START_MINUTES.length) {
            return 8 * 60;
        }
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int custom = sp.getInt(KEY_PREFIX + section, -1);
        if (custom >= 0) {
            return custom;
        }
        return DEFAULT_START_MINUTES[section];
    }

    public static void setStartMinute(Context context, int section, int minute) {
        if (section <= 0 || section >= DEFAULT_START_MINUTES.length) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_PREFIX + section, minute).apply();
    }

    public static int maxSection() {
        return DEFAULT_START_MINUTES.length - 1;
    }

    public static String timeText(int section) {
        int min = startMinute(section);
        int h = min / 60;
        int m = min % 60;
        return String.format(java.util.Locale.CHINA, "%02d:%02d", h, m);
    }

    public static String timeText(Context context, int section) {
        int min = startMinute(context, section);
        int h = min / 60;
        int m = min % 60;
        return String.format(java.util.Locale.CHINA, "%02d:%02d", h, m);
    }
}
