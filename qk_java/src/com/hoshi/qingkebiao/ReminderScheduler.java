package com.hoshi.qingkebiao;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

public class ReminderScheduler {
    public static final String PREFS = "qingkebiao";
    public static final String KEY_REMINDER = "reminder_enabled";
    private static final String ACTION_REMIND = "com.hoshi.qingkebiao.ACTION_REMIND";

    private ReminderScheduler() {}

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_REMINDER, false);
    }

    public static void scheduleAll(Context context) {
        context = context.getApplicationContext();
        if (!isEnabled(context)) {
            return;
        }
        cancelAll(context);
        CourseDatabase db = new CourseDatabase(context);
        List<Course> courses = db.getAll();
        for (Course c : courses) {
            scheduleCourse(context, c);
        }
    }

    public static void scheduleCourse(Context context, Course c) {
        context = context.getApplicationContext();
        if (!isEnabled(context) || c == null) {
            return;
        }
        long trigger = nextTriggerMillis(c);
        if (trigger <= 0) {
            return;
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMIND);
        intent.putExtra("course_id", c.id);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int) c.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }

    public static void cancelCourse(Context context, long courseId) {
        context = context.getApplicationContext();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMIND);
        intent.putExtra("course_id", courseId);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int) courseId, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
        }
    }

    public static void cancelAll(Context context) {
        context = context.getApplicationContext();
        CourseDatabase db = new CourseDatabase(context);
        List<Course> courses = db.getAll();
        for (Course c : courses) {
            cancelCourse(context, c.id);
        }
    }

    public static long nextTriggerMillis(Course c) {
        if (c == null || c.day < 1 || c.day > 7) {
            return -1;
        }
        int nowDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // 1=Sun..7=Sat
        int targetDow = c.day == 7 ? Calendar.SUNDAY : c.day + 1; // 1=Mon..7=Sun -> Calendar.SUNDAY..SAT
        int diff = targetDow - nowDow;

        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, diff);
        int minute = TimeTable.startMinute(c.start);
        next.set(Calendar.HOUR_OF_DAY, minute / 60);
        next.set(Calendar.MINUTE, minute % 60);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        long now = System.currentTimeMillis();
        if (next.getTimeInMillis() <= now) {
            next.add(Calendar.DAY_OF_YEAR, 7);
        }
        return next.getTimeInMillis();
    }
}
