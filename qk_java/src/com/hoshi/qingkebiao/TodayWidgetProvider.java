package com.hoshi.qingkebiao;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayWidgetProvider extends AppWidgetProvider {
    private static final int[] COURSE_COLORS = {
            0xFFFFCDD2, 0xFFFFE0B2, 0xFFFFF9C4,
            0xFFC8E6C9, 0xFFB2DFDB, 0xFFBBDEFB,
            0xFFD1C4E9, 0xFFF8BBD0, 0xFFD7CCC8,
            0xFFCFD8DC
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        updateAll(context);
    }

    public static void updateAll(Context context) {
        CourseDatabase db = new CourseDatabase(context);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // 1=Sun
        int weekDay = (day == 1) ? 7 : day - 1;
        List<Course> list = db.getByDay(weekDay);

        int currentWeek = WeekDateManager.currentWeek(context);
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd", Locale.CHINA);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);
        views.setTextViewText(R.id.widget_date, dateFmt.format(new Date()));
        views.setTextViewText(R.id.widget_week_day, "第" + currentWeek + "周 " + dayNames[weekDay]);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);
        Intent serviceIntent = new Intent(context, TodayWidgetRemoteViewsService.class);
        views.setRemoteAdapter(R.id.widget_list, serviceIntent);

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);
        views.setOnClickPendingIntent(R.id.widget_arrow, pi);

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
