package com.hoshi.qingkebiao;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TodayWidgetProvider.updateAll(context);
        ReminderScheduler.scheduleAll(context);
    }
}
