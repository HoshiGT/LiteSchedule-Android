package com.hoshi.qingkebiao;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TimeSettingsActivity extends Activity {
    private LinearLayout container;
    private final List<TextView> labels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_settings);
        container = findViewById(R.id.container);

        for (int sec = 1; sec <= TimeTable.maxSection(); sec++) {
            final int section = sec;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_card);
            row.setPadding(20, 16, 20, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 6, 0, 6);
            row.setLayoutParams(lp);

            TextView title = new TextView(this);
            title.setText("第" + sec + "节");
            title.setTextSize(16);
            title.setTextColor(0xFF2D3142);
            title.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView time = new TextView(this);
            time.setText(TimeTable.timeText(this, sec));
            time.setTextSize(16);
            time.setTextColor(0xFF4C5C92);
            time.setGravity(Gravity.CENTER);
            row.addView(time, new LinearLayout.LayoutParams(80, LinearLayout.LayoutParams.WRAP_CONTENT));

            labels.add(time);
            final TextView timeView = time;
            row.setOnClickListener(v -> {
                String[] parts = timeView.getText().toString().split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                new TimePickerDialog(this, (picker, h, m) -> {
                    TimeTable.setStartMinute(this, section, h * 60 + m);
                    timeView.setText(TimeTable.timeText(this, section));
                }, hour, minute, true).show();
            });

            container.addView(row);
        }
    }
}
