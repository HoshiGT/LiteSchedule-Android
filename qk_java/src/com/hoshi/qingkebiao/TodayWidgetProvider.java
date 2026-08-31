package com.hoshi.qingkebiao;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.List;

public class TodayWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        updateAll(context);
    }

    public static void updateAll(Context context) {
        CourseDatabase db = new CourseDatabase(context);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // 1=Sun
        int weekDay = (day == 1) ? 7 : day - 1;
        List<Course> list = db.getByDay(weekDay);
        StringBuilder sb = new StringBuilder();
        for (Course c : list) {
            sb.append(c.name).append(" ").append(c.sectionText()).append(" ").append(c.location).append("\n");
        }
        if (sb.length() == 0) sb.append("今日无课");

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);
        views.setTextViewText(R.id.widget_courses, sb.toString());
        for (int id : ids) mgr.updateAppWidget(id, views);
    }
}
