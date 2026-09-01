package com.hoshi.qingkebiao;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_NEXT_DAY = "com.hoshi.qingkebiao.WIDGET_NEXT_DAY";
    private static final String ACTION_PREV_DAY = "com.hoshi.qingkebiao.WIDGET_PREV_DAY";
    private static final String PREFS = "qingkebiao";
    private static final String KEY_OFFSET = "widget_day_offset";

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
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
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
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int offset = sp.getInt(KEY_OFFSET, 0);
        if (offset > 1) offset = 1;
        if (offset < 0) offset = 0;

        Calendar selected = Calendar.getInstance();
        selected.add(Calendar.DAY_OF_YEAR, offset);

        int weekDay = selected.get(Calendar.DAY_OF_WEEK);
        int weekDayNum = (weekDay == Calendar.SUNDAY) ? 7 : weekDay - 1;
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        CourseDatabase db = new CourseDatabase(context);
        List<Course> list = db.getByDay(weekDayNum);

        int currentWeek = WeekDateManager.currentWeek(context);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd", Locale.CHINA);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);
        views.setTextViewText(R.id.widget_date, dateFmt.format(selected.getTime()));
        views.setTextViewText(R.id.widget_week_day, "第" + currentWeek + "周 " + dayNames[weekDayNum]);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);
        views.setTextViewText(R.id.widget_empty,
                offset == 0 ? "今日无课  (⁠*⁠´⁠ω⁠｀⁠*⁠)" : "明日无课  (⁠*⁠´⁠ω⁠｀⁠*⁠)");

        Intent serviceIntent = new Intent(context, TodayWidgetRemoteViewsService.class);
        views.setRemoteAdapter(R.id.widget_list, serviceIntent);

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

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        for (int id : ids) {
            mgr.updateAppWidget(id, views);
        }
        if (ids.length > 0) {
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
        }
    }
}
