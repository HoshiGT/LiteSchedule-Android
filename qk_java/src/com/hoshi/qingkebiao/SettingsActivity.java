package com.hoshi.qingkebiao;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class SettingsActivity extends Activity {
    private static final String PREFS = "qingkebiao";
    private static final String KEY_REMINDER = "reminder_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        CheckBox cb = findViewById(R.id.cb_reminder);
        cb.setChecked(sp.getBoolean(KEY_REMINDER, false));
        ((android.view.View) findViewById(R.id.layout_background)).setOnClickListener(v ->
                startActivity(new Intent(this, BackgroundActivity.class)));
        ((android.view.View) findViewById(R.id.layout_start_date)).setOnClickListener(v -> {
            Calendar start = Calendar.getInstance();
            start.setTimeInMillis(WeekDateManager.getStartMillis(this));
            new DatePickerDialog(this, (picker, year, month, day) -> {
                Calendar c = Calendar.getInstance();
                c.clear();
                c.set(year, month, day);
                WeekDateManager.setStartMillis(this, c.getTimeInMillis());
                updateStartDateHint();
                Toast.makeText(this, "开学第一周已设置", Toast.LENGTH_SHORT).show();
            }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show();
        });
        updateStartDateHint();
        ((android.view.View) findViewById(R.id.layout_time_settings)).setOnClickListener(v ->
                startActivity(new Intent(this, TimeSettingsActivity.class)));
        ((android.view.View) findViewById(R.id.layout_import)).setOnClickListener(v ->
                startActivity(new Intent(this, ImportActivity.class)));
        ((android.view.View) findViewById(R.id.layout_open_source)).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.open_source_url))));
            } catch (Exception e) {
                Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
            }
        });
        ((android.view.View) findViewById(R.id.layout_donate)).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.donate_url))));
            } catch (Exception e) {
                Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
            }
        });
        updateBackgroundHint();

        cb.setOnCheckedChangeListener((button, checked) -> {
            sp.edit().putBoolean(KEY_REMINDER, checked).apply();
            if (checked) {
                ReminderScheduler.scheduleAll(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (am != null && !am.canScheduleExactAlarms()) {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName())));
                    }
                }
            } else {
                ReminderScheduler.cancelAll(this);
            }
            Toast.makeText(this, checked ? "上课提醒已开启" : "上课提醒已关闭", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBackgroundHint();
        if (ReminderScheduler.isEnabled(this)) {
            ReminderScheduler.scheduleAll(this);
        }
    }

    private void updateStartDateHint() {
        TextView hint = findViewById(R.id.tv_start_date_hint);
        if (hint != null) {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA);
            hint.setText("第一周周一：" + df.format(new java.util.Date(WeekDateManager.getStartMillis(this))));
        }
    }

    private void updateBackgroundHint() {
        TextView hint = findViewById(R.id.tv_background_hint);
        if (hint != null) {
            hint.setText(BackgroundManager.isUnlocked(this)
                    ? "已解锁，点击更换背景"
                    : "赞赏 1 元解锁");
        }
    }

    public static boolean reminderEnabled(Activity a) {
        return a.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_REMINDER, false);
    }
}
