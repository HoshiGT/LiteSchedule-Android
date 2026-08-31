package com.hoshi.qingkebiao;

import android.content.Context;
import android.content.SharedPreferences;

public class BackgroundManager {
    public static final String PREFS = "qingkebiao";
    public static final String KEY_UNLOCKED = "background_unlocked";
    public static final String KEY_BACKGROUND = "background_index";
    public static final String KEY_IMAGE_MODE = "background_image_mode";
    public static final String KEY_IMAGE_PATH = "background_image_path";

    private static final int[] BACKGROUNDS = {
            R.drawable.bg_home_default,
            R.drawable.bg_home_blue,
            R.drawable.bg_home_warm,
            R.drawable.bg_home_mint,
            R.drawable.bg_home_lavender
    };

    public static final String[] NAMES = {
            "默认浅色",
            "清新蓝",
            "暖阳橙",
            "薄荷绿",
            "淡雅紫"
    };

    private BackgroundManager() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isUnlocked(Context c) {
        return sp(c).getBoolean(KEY_UNLOCKED, false);
    }

    public static void setUnlocked(Context c, boolean unlocked) {
        sp(c).edit().putBoolean(KEY_UNLOCKED, unlocked).apply();
    }

    public static int getIndex(Context c) {
        return sp(c).getInt(KEY_BACKGROUND, 0);
    }

    public static void setIndex(Context c, int index) {
        sp(c).edit().putInt(KEY_BACKGROUND, index).apply();
    }

    public static int backgroundRes(Context c) {
        int index = getIndex(c);
        if (index < 0 || index >= BACKGROUNDS.length) {
            return BACKGROUNDS[0];
        }
        return BACKGROUNDS[index];
    }

    public static int backgroundRes(int index) {
        if (index < 0 || index >= BACKGROUNDS.length) {
            return BACKGROUNDS[0];
        }
        return BACKGROUNDS[index];
    }

    public static boolean isImageMode(Context c) {
        return sp(c).getBoolean(KEY_IMAGE_MODE, false);
    }

    public static void setImageMode(Context c, boolean imageMode) {
        sp(c).edit().putBoolean(KEY_IMAGE_MODE, imageMode).apply();
    }

    public static String getImagePath(Context c) {
        return sp(c).getString(KEY_IMAGE_PATH, null);
    }

    public static void setImagePath(Context c, String path) {
        sp(c).edit().putString(KEY_IMAGE_PATH, path).apply();
    }
}
