package com.hoshi.qingkebiao;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AddCourseActivity extends Activity {
    private static final int[] COLORS = {
            0xFFFFCDD2, 0xFFFFE0B2, 0xFFFFF9C4,
            0xFFC8E6C9, 0xFFB2DFDB, 0xFFBBDEFB,
            0xFFD1C4E9, 0xFFF8BBD0, 0xFFD7CCC8,
            0xFFCFD8DC
    };

    private int selectedColor = 0;
    private final List<View> colorViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        int day = getIntent().getIntExtra("day", 1);
        int start = getIntent().getIntExtra("start", 1);
        int end = getIntent().getIntExtra("end", start);

        ((EditText) findViewById(R.id.et_start)).setText(String.valueOf(start));
        ((EditText) findViewById(R.id.et_end)).setText(String.valueOf(end));
        ((EditText) findViewById(R.id.et_weeks)).setText("1-16");

        buildColorRow();

        ((Button) findViewById(R.id.btn_save)).setOnClickListener(v -> {
            String name = ((EditText) findViewById(R.id.et_course_name)).getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            int s = parseInt(((EditText) findViewById(R.id.et_start)).getText().toString(), start);
            int e = parseInt(((EditText) findViewById(R.id.et_end)).getText().toString(), end);
            if (s <= 0 || e < s) {
                Toast.makeText(this, "节次不正确", Toast.LENGTH_SHORT).show();
                return;
            }
            String weeks = ((EditText) findViewById(R.id.et_weeks)).getText().toString().trim();
            if (weeks.isEmpty()) weeks = "1-16";
            String teacher = ((EditText) findViewById(R.id.et_teacher)).getText().toString().trim();
            String location = ((EditText) findViewById(R.id.et_location)).getText().toString().trim();
            String remark = ((EditText) findViewById(R.id.et_remark)).getText().toString().trim();

            Course c = new Course(name, teacher, location, day, s, e, weeks, selectedColor);
            CourseDatabase db = new CourseDatabase(this);
            db.add(c);
            Toast.makeText(this, "已添加 " + name, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void buildColorRow() {
        LinearLayout row = findViewById(R.id.color_row);
        for (int i = 0; i < COLORS.length; i++) {
            View color = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(COLORS[i]);
            bg.setCornerRadius(dp(18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            color.setLayoutParams(lp);
            color.setBackground(bg);
            final int index = i;
            color.setOnClickListener(v -> {
                selectedColor = index;
                updateColorSelection();
            });
            row.addView(color);
            colorViews.add(color);
        }
        updateColorSelection();
    }

    private void updateColorSelection() {
        for (int i = 0; i < colorViews.size(); i++) {
            float alpha = (i == selectedColor) ? 1f : 0.35f;
            colorViews.get(i).setAlpha(alpha);
            int border = (i == selectedColor) ? 0xFF2D3142 : 0x00000000;
            GradientDrawable bg = (GradientDrawable) colorViews.get(i).getBackground();
            bg.setStroke(dp(3), border);
            colorViews.get(i).invalidate();
        }
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
