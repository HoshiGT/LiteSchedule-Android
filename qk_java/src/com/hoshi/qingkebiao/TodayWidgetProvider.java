package com.hoshi.qingkebiao;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 今日课程小组件。
 *
 * 课程列表**不使用** ListView + RemoteViewsService，而是当成普通静态 RemoteViews 直接下发：
 * 远程集合视图要靠绑定服务取数，App 被系统冻结/回收时服务绑不上，
 * 就会出现「日期、周次都对，但课程列表空白」——所以两部分必须在同一次 updateAppWidget 里更新。
 *
 * 显示几行由小组件当前高度决定（见 {@link #capacityFor}），避免行数过多被裁掉。
 */
public class TodayWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_NEXT_DAY = "com.hoshi.qingkebiao.WIDGET_NEXT_DAY";
    private static final String ACTION_PREV_DAY = "com.hoshi.qingkebiao.WIDGET_PREV_DAY";
    private static final String ACTION_DAY_TICK = "com.hoshi.qingkebiao.WIDGET_DAY_TICK";
    private static final String PREFS = "qingkebiao";
    private static final String KEY_OFFSET = "widget_day_offset";

    /** 布局里预留的行数（6 行：最多 5 门课 + 1 行「还有 N 门课」） */
    private static final int MAX_ROWS = 6;
    /** 头部（日期 + 周次 + 箭头 + 外边距）大约占的高度，单位 dp */
    private static final int HEADER_DP = 60;
    /**
     * 每行课程**至少**要占的高度（8dp 内边距 ×2 + 一行 12sp 文字 + 4dp 间隔），单位 dp。
     * 课程行本身是等分权重（0dp + weight=1），实际高度 = 可用高度 / 可见子项数，
     * 这里只用来估算「大概放得下几行」，不再直接把行高定死。
     */
    private static final int ROW_DP = 36;
    /**
     * 低于这个高度就认为启动器上报的值不可信，按这个高度算。
     * 实测荣耀(HONOR)启动器只上报默认最小值 110dp，不随实际尺寸更新，
     * 而小组件实际有 185dp —— 不兜底就会算成「只够 1 行」。
     * 180dp = 头部 60dp + 3 行课程，也是默认放置尺寸下能正常显示的行数。
     */
    private static final int MIN_TRUSTED_HEIGHT_DP = 180;

    private static final int[] ITEM_IDS = {
            R.id.widget_item_0, R.id.widget_item_1, R.id.widget_item_2,
            R.id.widget_item_3, R.id.widget_item_4, R.id.widget_item_5
    };
    private static final int[] ITEM_NAME_IDS = {
            R.id.widget_item_name_0, R.id.widget_item_name_1, R.id.widget_item_name_2,
            R.id.widget_item_name_3, R.id.widget_item_name_4, R.id.widget_item_name_5
    };
    private static final int[] ITEM_INFO_IDS = {
            R.id.widget_item_info_0, R.id.widget_item_info_1, R.id.widget_item_info_2,
            R.id.widget_item_info_3, R.id.widget_item_info_4, R.id.widget_item_info_5
    };
    /** 课程行是等分权重，课少时用这些空格补齐，避免行高被撑变形 */
    private static final int[] SPACER_IDS = {
            R.id.widget_spacer_0, R.id.widget_spacer_1, R.id.widget_spacer_2,
            R.id.widget_spacer_3, R.id.widget_spacer_4
    };
    private static final int[] BUBBLE_BACKGROUNDS = {
            R.drawable.widget_bubble_0, R.drawable.widget_bubble_1,
            R.drawable.widget_bubble_2, R.drawable.widget_bubble_3,
            R.drawable.widget_bubble_4, R.drawable.widget_bubble_5,
            R.drawable.widget_bubble_6, R.drawable.widget_bubble_7,
            R.drawable.widget_bubble_8, R.drawable.widget_bubble_9
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_NEXT_DAY.equals(action)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(KEY_OFFSET, 1).apply();
            updateAll(context);
            return;
        } else if (ACTION_PREV_DAY.equals(action)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(KEY_OFFSET, 0).apply();
            updateAll(context);
            return;
        } else if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || ACTION_DAY_TICK.equals(action)) {
            // 跨天（含跨周）时把「预览明天」状态复位，否则新的一天会继续停在昨天选的偏移上
            if (Intent.ACTION_DATE_CHANGED.equals(action) || ACTION_DAY_TICK.equals(action)) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putInt(KEY_OFFSET, 0).apply();
            }
            updateAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        updateAll(context);
    }

    public static void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        for (int id : ids) {
            mgr.updateAppWidget(id, buildViews(context, capacityFor(mgr, id)));
        }
        scheduleNextDayRefresh(context);
    }

    /** 当前小组件高度大概能放下几行（含「还有 N 门课」那行） */
    private static int capacityFor(AppWidgetManager mgr, int widgetId) {
        int heightDp = 0;
        try {
            Bundle opts = mgr.getAppWidgetOptions(widgetId);
            if (opts != null) {
                heightDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
            }
        } catch (Exception ignored) {
        }
        int effective = Math.max(heightDp, MIN_TRUSTED_HEIGHT_DP);
        int fit = (effective - HEADER_DP) / ROW_DP;
        // 至少 1 行，最多布局里预留的行数
        return Math.max(1, Math.min(MAX_ROWS, fit));
    }

    private static RemoteViews buildViews(Context context, int capacity) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int offset = sp.getInt(KEY_OFFSET, 0);
        if (offset > 1) offset = 1;
        if (offset < 0) offset = 0;

        Calendar selected = Calendar.getInstance();
        selected.add(Calendar.DAY_OF_YEAR, offset);

        int weekDay = selected.get(Calendar.DAY_OF_WEEK);
        int weekDayNum = (weekDay == Calendar.SUNDAY) ? 7 : weekDay - 1;
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        int currentWeek = WeekDateManager.weekForDate(context, selected.getTimeInMillis());
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd", Locale.CHINA);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);
        views.setTextViewText(R.id.widget_date, dateFmt.format(selected.getTime()));
        views.setTextViewText(R.id.widget_week_day, "第" + currentWeek + "周 " + dayNames[weekDayNum]);

        // 当天（或预览的明天）的课程
        List<Course> today = new ArrayList<>();
        CourseDatabase db = new CourseDatabase(context);
        for (Course c : db.getByDay(weekDayNum)) {
            if (c.isInWeek(currentWeek)) {
                today.add(c);
            }
        }
        db.close();

        int cap = Math.max(1, Math.min(capacity, MAX_ROWS));
        int shown = Math.min(today.size(), cap);
        boolean overflow = today.size() > shown;
        boolean inlineHint = false;
        if (overflow) {
            if (cap >= 2) {
                // 留一行给「还有 N 门课」
                shown = Math.max(0, Math.min(today.size(), cap - 1));
                overflow = today.size() > shown;
            } else {
                // 只够一行：优先显示第一门课，溢出提示并到这一行的信息里，不再单独占一行
                shown = Math.min(today.size(), 1);
                inlineHint = true;
                overflow = false;
            }
        }

        for (int i = 0; i < ITEM_IDS.length; i++) {
            if (i < shown) {
                Course c = today.get(i);
                views.setViewVisibility(ITEM_IDS[i], View.VISIBLE);
                views.setTextViewText(ITEM_NAME_IDS[i], c.name);
                String info = courseInfo(c);
                if (inlineHint && i == shown - 1) {
                    info = info + " · 还有" + (today.size() - shown) + "门";
                }
                views.setTextViewText(ITEM_INFO_IDS[i], info);
                views.setInt(ITEM_IDS[i], "setBackgroundResource", bubbleFor(c.name));
            } else if (i == shown && overflow) {
                views.setViewVisibility(ITEM_IDS[i], View.VISIBLE);
                views.setTextViewText(ITEM_NAME_IDS[i], "还有 " + (today.size() - shown) + " 门课…");
                views.setTextViewText(ITEM_INFO_IDS[i], "");
                views.setInt(ITEM_IDS[i], "setBackgroundResource", R.drawable.widget_bubble_9);
            } else {
                views.setViewVisibility(ITEM_IDS[i], View.GONE);
            }
        }

        // 课程行是等分权重：可见子项数 = 显示的课程行（含「还有 N 门课」行）+ 补齐的空格。
        // 课少时用空格占位，保证每行高度稳定（否则只有一门课时会被撑成一个大泡泡）。
        int visibleRows = shown + (overflow ? 1 : 0);
        int spacers = today.isEmpty() ? 0 : Math.max(0, cap - visibleRows);
        for (int i = 0; i < SPACER_IDS.length; i++) {
            views.setViewVisibility(SPACER_IDS[i], i < spacers ? View.VISIBLE : View.GONE);
        }
        // 没课时把整个列表容器收起来，让「今日无课」占满空间并居中
        views.setViewVisibility(R.id.widget_items, today.isEmpty() ? View.GONE : View.VISIBLE);

        views.setTextViewText(R.id.widget_empty,
                offset == 0 ? "今日无课  (⁠*⁠´⁠ω⁠｀⁠*⁠)" : "明日无课  (⁠*⁠´⁠ω⁠｀⁠*⁠)");
        views.setViewVisibility(R.id.widget_empty, today.isEmpty() ? View.VISIBLE : View.GONE);

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, openPi);

        if (offset == 0) {
            views.setTextViewText(R.id.widget_arrow, "→");
            Intent next = new Intent(context, TodayWidgetProvider.class).setAction(ACTION_NEXT_DAY);
            PendingIntent nextPi = PendingIntent.getBroadcast(context, 1, next,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_arrow, nextPi);
        } else {
            views.setTextViewText(R.id.widget_arrow, "←");
            Intent prev = new Intent(context, TodayWidgetProvider.class).setAction(ACTION_PREV_DAY);
            PendingIntent prevPi = PendingIntent.getBroadcast(context, 1, prev,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_arrow, prevPi);
        }
        return views;
    }

    private static String courseInfo(Course c) {
        String info = c.sectionText();
        if (c.location != null && !c.location.isEmpty()) {
            info = info + " · " + c.location;
        }
        return info;
    }

    private static int bubbleFor(String name) {
        // 注意：不能直接 Math.abs(hashCode())——hashCode()==Integer.MIN_VALUE 时 Math.abs 仍为负，
        // 会算出负下标导致数组越界（小组件渲染失败 -> 列表空白）
        long h = (name == null) ? 0L : (long) name.hashCode();
        int idx = (int) (Math.abs(h) % BUBBLE_BACKGROUNDS.length);
        return BUBBLE_BACKGROUNDS[idx];
    }

    private static void scheduleNextDayRefresh(Context context) {
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(context, TodayWidgetProvider.class).setAction(ACTION_DAY_TICK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        // 有精确闹钟权限就用精确闹钟：doze 下 setAndAllowWhileIdle 不保证准点跨天
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        try {
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }
    }
}
