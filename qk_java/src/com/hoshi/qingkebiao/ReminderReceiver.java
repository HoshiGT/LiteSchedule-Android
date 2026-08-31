package com.hoshi.qingkebiao;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "qingkebiao_reminder";
    private static final String ACTION_REMIND = "com.hoshi.qingkebiao.ACTION_REMIND";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_REMIND.equals(intent.getAction())) {
            return;
        }
        long id = intent.getLongExtra("course_id", -1);
        if (id <= 0) {
            return;
        }
        if (!ReminderScheduler.isEnabled(context)) {
            return;
        }
        CourseDatabase db = new CourseDatabase(context);
        Course c = db.getById(id);
        if (c == null) {
            return;
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "上课提醒", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, (int) id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        String dayText = "周" + "一二三四五六日".charAt(c.day - 1);
        String body = dayText + " " + c.name + " " + c.sectionText()
                + " " + TimeTable.timeText(c.start) + "  " + c.location;
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("上课提醒")
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        try {
            nm.notify((int) id, builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS 被拒绝时静默跳过
        }

        // 已触发后继续安排下一次（下一周同一节次）
        ReminderScheduler.scheduleCourse(context, c);
    }
}
