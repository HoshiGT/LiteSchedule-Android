package com.hoshi.qingkebiao;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String[] DAY_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final int[] COURSE_COLORS = {
            0xFFFFCDD2, 0xFFFFE0B2, 0xFFFFF9C4,
            0xFFC8E6C9, 0xFFB2DFDB, 0xFFBBDEFB,
            0xFFD1C4E9, 0xFFF8BBD0, 0xFFD7CCC8,
            0xFFCFD8DC
    };
    private static final int MAX_WEEK = 20;

    private CourseDatabase db;
    private TextView title;
    private TextView currentWeekText;
    private TextView todayWeekText;
    private View emptyPanel;
    private ViewPager2 weekPager;
    private List<Course> allCourses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new CourseDatabase(this);
        title = findViewById(R.id.tv_title);
        currentWeekText = findViewById(R.id.tv_current_week);
        todayWeekText = findViewById(R.id.tv_today_week);
        emptyPanel = findViewById(R.id.empty_panel);
        weekPager = findViewById(R.id.week_pager);

        ((ImageButton) findViewById(R.id.btn_settings)).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        ((Button) findViewById(R.id.btn_go_import)).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        ((Button) findViewById(R.id.btn_prev_week)).setOnClickListener(v -> changeWeek(-1));
        ((Button) findViewById(R.id.btn_next_week)).setOnClickListener(v -> changeWeek(1));
        setupWeekPager();
        reload();
    }

    private void setupWeekPager() {
        weekPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        weekPager.setOffscreenPageLimit(1);
        weekPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int week = clampWeek(position + 1);
                if (week != CurrentWeekManager.get(MainActivity.this)) {
                    CurrentWeekManager.set(MainActivity.this, week);
                    updateHeader(week);
                }
            }
        });
    }

    private void reload() {
        allCourses = db.getAll();
        buildWeekPages(allCourses);

        int week = CurrentWeekManager.get(this);
        boolean hasAny = !allCourses.isEmpty();
        if (hasAny) {
            emptyPanel.setVisibility(View.GONE);
            weekPager.setVisibility(View.VISIBLE);
            scrollToWeek(week, false);
        } else {
            emptyPanel.setVisibility(View.VISIBLE);
            weekPager.setVisibility(View.GONE);
        }
        updateHeader(week);
        TodayWidgetProvider.updateAll(this);
        ReminderScheduler.scheduleAll(this);
    }

    private void buildWeekPages(List<Course> all) {
        weekPager.setAdapter(new WeekPagerAdapter(all));
    }

    private List<Course> filterByWeek(List<Course> all, int week) {
        List<Course> list = new ArrayList<>();
        for (Course c : all) {
            if (c.isInWeek(week)) list.add(c);
        }
        return list;
    }


    private View createWeekPageContent(List<Course> weekCourses, int week) {
        int maxSection = TimeTable.maxSection();
        for (Course c : weekCourses) {
            if (c.end > maxSection) maxSection = c.end;
        }
        if (maxSection > 20) maxSection = 20;

        int rowH = dp(54);
        int timeW = dp(56);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // 表头：星期
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView timeHeader = new TextView(this);
        timeHeader.setText("上课时间");
        timeHeader.setGravity(Gravity.CENTER);
        timeHeader.setTextSize(11);
        timeHeader.setTextColor(0xFF5A6478);
        timeHeader.setPadding(dp(2), dp(6), dp(2), dp(6));
        header.addView(timeHeader, new LinearLayout.LayoutParams(timeW, LinearLayout.LayoutParams.WRAP_CONTENT));
        for (int di = 0; di < DAY_NAMES.length; di++) {
            String day = DAY_NAMES[di];
            TextView dayView = new TextView(this);
            dayView.setText(day + "\n" + WeekDateManager.dateText(this, week, di + 1));
            dayView.setGravity(Gravity.CENTER);
            dayView.setTextSize(12);
            dayView.setTypeface(null, Typeface.BOLD);
            dayView.setTextColor(0xFF4C5C92);
            dayView.setPadding(dp(2), dp(6), dp(2), dp(6));
            header.addView(dayView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        content.addView(header);

        // 左侧时间列
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout timeColumn = new LinearLayout(this);
        timeColumn.setOrientation(LinearLayout.VERTICAL);
        for (int sec = 1; sec <= maxSection; sec++) {
            TextView timeCell = new TextView(this);
            timeCell.setText(TimeTable.timeText(this, sec));
            timeCell.setGravity(Gravity.CENTER);
            timeCell.setTextSize(9);
            timeCell.setTextColor(0xFF6B7280);
            timeCell.setBackgroundColor(0xFFEDF1FA);
            timeCell.setPadding(dp(1), dp(1), dp(1), dp(1));
            timeColumn.addView(timeCell, new LinearLayout.LayoutParams(timeW, rowH));
        }
        body.addView(timeColumn, new LinearLayout.LayoutParams(timeW, maxSection * rowH));

        // 课程区
        FrameLayout dataArea = new FrameLayout(this);
        body.addView(dataArea, new LinearLayout.LayoutParams(0, maxSection * rowH, 1f));

        // 计算每列宽度
        int availableWidth = getResources().getDisplayMetrics().widthPixels - timeW - dp(16);
        float colW = availableWidth / 7f;

        for (Course c : weekCourses) {
            TextView bubble = new TextView(this);
            bubble.setGravity(Gravity.CENTER);
            bubble.setTextSize(10);
            bubble.setTextColor(0xFF2D3142);
            bubble.setPadding(dp(2), dp(1), dp(2), dp(1));
            StringBuilder sb = new StringBuilder();
            sb.append(c.name);
            if (c.location != null && !c.location.isEmpty()) {
                sb.append("\n\n").append(c.location);
            }
            bubble.setText(sb.toString());
            bubble.setBackground(roundedBg(colorFor(c), 8));

            int left = (int) ((c.day - 1) * colW + dp(2));
            int top = (int) ((c.start - 1) * rowH + dp(2));
            int width = (int) (colW - dp(4));
            int height = (int) ((c.end - c.start + 1) * rowH - dp(4));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
            lp.leftMargin = left;
            lp.topMargin = top;
            final int secFinal = c.start;
            final int dayFinal = c.day;
            List<Course> single = new ArrayList<>();
            single.add(c);
            bubble.setOnClickListener(v -> showCourseDialog(single, secFinal, dayFinal));
            dataArea.addView(bubble, lp);
        }

        content.addView(body);
        return content;
    }

    private TableLayout createWeekTable(List<Course> weekCourses) {
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(true);
        table.setColumnStretchable(1, true);
        table.setColumnStretchable(2, true);
        table.setColumnStretchable(3, true);
        table.setColumnStretchable(4, true);
        table.setColumnStretchable(5, true);
        table.setColumnStretchable(6, true);
        table.setColumnStretchable(7, true);
        table.setPadding(dp(4), dp(4), dp(4), dp(8));

        TableRow header = new TableRow(this);
        header.addView(makeHeaderCell("时间", true));
        for (String day : DAY_NAMES) {
            header.addView(makeHeaderCell(day, false));
        }
        table.addView(header);

        List<int[]> blocks = collectBlocks(weekCourses);
        for (int[] block : blocks) {
            int start = block[0];
            int end = block[1];

            TableRow row = new TableRow(this);
            row.setPadding(dp(3), dp(3), dp(3), dp(3));

            TextView timeCell = new TextView(this);
            timeCell.setText("第" + start + "-" + end + "节\n" + TimeTable.timeText(start));
            timeCell.setGravity(Gravity.CENTER);
            timeCell.setTextSize(11);
            timeCell.setTextColor(0xFF5A6478);
            timeCell.setPadding(dp(4), dp(2), dp(4), dp(2));
            timeCell.setHeight(dp(108));
            timeCell.setGravity(Gravity.CENTER);
            timeCell.setBackground(roundedBg(0xFFEDF1FA, 4));
            row.addView(timeCell);

            for (int day = 1; day <= 7; day++) {
                List<Course> cellCourses = findCoursesAt(weekCourses, start, end, day);
                TextView cell = new TextView(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(11);
                cell.setTextColor(0xFF2D3142);
                cell.setPadding(dp(2), dp(2), dp(2), dp(2));
                cell.setHeight(dp(108));
                cell.setGravity(Gravity.CENTER);
                if (!cellCourses.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < cellCourses.size(); i++) {
                        Course c = cellCourses.get(i);
                        if (i > 0) sb.append('\n');
                        sb.append(c.name);
                        if (c.location != null && !c.location.isEmpty()) {
                            sb.append('\n').append(c.location);
                        }
                    }
                    cell.setText(sb.toString());
                    cell.setBackground(roundedBg(colorFor(cellCourses.get(0)), 6));
                    final int secFinal = start;
                    final int dayFinal = day;
                    cell.setOnClickListener(v -> showCourseDialog(cellCourses, secFinal, dayFinal));
                } else {
                    cell.setBackground(roundedBg(0x00000000, 6));
                }
                row.addView(cell);
            }
            table.addView(row);
        }
        return table;
    }

    private List<int[]> collectBlocks(List<Course> weekCourses) {
        List<int[]> blocks = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        // 标准大节：即使没课也占行，避免下半部分空一大块
        int[][] standard = {
                {1, 2}, {3, 4}, {5, 6}, {7, 8}, {9, 10}, {11, 13}
        };
        for (int[] b : standard) {
            String key = b[0] + "-" + b[1];
            if (seen.add(key)) blocks.add(new int[]{b[0], b[1]});
        }
        for (Course c : weekCourses) {
            String key = c.start + "-" + c.end;
            if (seen.add(key)) {
                blocks.add(new int[]{c.start, c.end});
            }
        }
        blocks.sort((a, b) -> {
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]);
        });
        return blocks;
    }

    private TextView makeHeaderCell(String text, boolean timeHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(timeHeader ? 0xFF5A6478 : 0xFF4C5C92);
        tv.setPadding(dp(4), dp(8), dp(4), dp(8));
        tv.setBackground(roundedBg(0xFFEDF1FA, 6));
        return tv;
    }

    private List<Course> findCoursesAt(List<Course> weekCourses, int blockStart, int blockEnd, int day) {
        List<Course> res = new ArrayList<>();
        for (Course c : weekCourses) {
            if (c.day == day && c.start == blockStart && c.end == blockEnd) {
                res.add(c);
            }
        }
        return res;
    }

    private int colorFor(Course c) {
        return COURSE_COLORS[Math.abs(c.name.hashCode()) % COURSE_COLORS.length];
    }

    private GradientDrawable roundedBg(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private void showCourseDialog(List<Course> list, int section, int day) {
        if (list.isEmpty()) return;
        StringBuilder msg = new StringBuilder();
        for (Course c : list) {
            if (msg.length() > 0) msg.append("\n\n");
            msg.append(c.name)
                    .append("  ").append(c.sectionText())
                    .append("  周").append(DAY_NAMES[c.day - 1].substring(1))
                    .append("\n教室：").append(c.location == null ? "" : c.location)
                    .append("\n教师：").append(c.teacher == null ? "" : c.teacher)
                    .append("\n周次：").append(c.weeks);
        }
        new AlertDialog.Builder(this)
                .setTitle("第" + section + "节 周" + DAY_NAMES[day - 1].substring(1))
                .setMessage(msg.toString())
                .setPositiveButton("删除", (d, w) -> {
                    for (Course c : list) {
                        ReminderScheduler.cancelCourse(this, c.id);
                        db.delete(c.id);
                    }
                    reload();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void changeWeek(int delta) {
        int newWeek = clampWeek(CurrentWeekManager.get(this) + delta);
        CurrentWeekManager.set(this, newWeek);
        scrollToWeek(newWeek, true);
        updateHeader(newWeek);
    }

    private void scrollToWeek(int week, boolean smooth) {
        int index = clampWeek(week) - 1;
        weekPager.setCurrentItem(index, smooth);
    }

    private void updateHeader(int week) {
        currentWeekText.setText("第" + week + "周");
        if (todayWeekText != null) {
            todayWeekText.setText("实际本周：第" + WeekDateManager.currentWeek(this) + "周");
        }
        int count = filterByWeek(allCourses, week).size();
        if (allCourses.isEmpty()) {
            title.setText("本周课程（0门）");
        } else {
            title.setText("第" + week + "周 · 本周课程（" + count + "门）");
        }
    }

    private int clampWeek(int week) {
        if (week < 1) return 1;
        if (week > MAX_WEEK) return MAX_WEEK;
        return week;
    }

    private class WeekPagerAdapter extends RecyclerView.Adapter<WeekViewHolder> {
        private final List<Course> data;

        WeekPagerAdapter(List<Course> data) {
            this.data = new ArrayList<>(data);
        }

        @Override
        public WeekViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int week = parent.getChildCount() < 0 ? 1 : 1; // filled in create
            // 在 createWeekPageView 中创建
            LinearLayout page = new LinearLayout(MainActivity.this);
            page.setOrientation(LinearLayout.VERTICAL);

            // ViewPager2 的 RecyclerView 会给 page 配好宽高
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            page.setLayoutParams(lp);
            return new WeekViewHolder(page);
        }

        @Override
        public void onBindViewHolder(WeekViewHolder holder, int position) {
            int week = position + 1;
            ViewGroup container = (ViewGroup) holder.itemView;
            container.removeAllViews();
            List<Course> weekCourses = filterByWeek(data, week);
            ScrollView scroll = new ScrollView(MainActivity.this);
            scroll.setFillViewport(true);
            scroll.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout content = new LinearLayout(MainActivity.this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (weekCourses.isEmpty()) {
                content.setGravity(Gravity.CENTER);
                content.setPadding(0, dp(40), 0, 0);
                TextView empty = new TextView(MainActivity.this);
                empty.setText("第" + week + "周暂无课程");
                empty.setTextSize(14);
                empty.setTextColor(0xFF6B7280);
                content.addView(empty);
            } else {
                content.addView(createWeekPageContent(weekCourses, week));
            }
            scroll.addView(content);
            container.addView(scroll);
        }

        @Override
        public int getItemCount() {
            return MAX_WEEK;
        }
    }

    private static class WeekViewHolder extends RecyclerView.ViewHolder {
        WeekViewHolder(View itemView) {
            super(itemView);
        }
    }

    private void applyBackground() {
        View root = findViewById(R.id.main_root);
        if (root == null) {
            return;
        }
        if (BackgroundManager.isImageMode(this)) {
            String path = BackgroundManager.getImagePath(this);
            if (path != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                if (bitmap != null) {
                    root.setBackground(new BitmapDrawable(getResources(), bitmap));
                    return;
                }
            }
        }
        root.setBackgroundResource(BackgroundManager.backgroundRes(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyBackground();
        reload();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void showAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = 24;
        layout.setPadding(pad, pad, pad, pad);

        EditText name = new EditText(this);
        name.setHint("课程名");
        EditText teacher = new EditText(this);
        teacher.setHint("教师");
        EditText location = new EditText(this);
        location.setHint("教室");
        EditText weeks = new EditText(this);
        weeks.setHint("周次，如 1-16");
        weeks.setInputType(InputType.TYPE_CLASS_TEXT);
        Spinner day = new Spinner(this);
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"});
        day.setAdapter(dayAdapter);
        EditText startSec = new EditText(this);
        startSec.setHint("起始节次");
        startSec.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText endSec = new EditText(this);
        endSec.setHint("结束节次");
        endSec.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText[] editTexts = {name, teacher, location, weeks, startSec, endSec};
        for (EditText et : editTexts) {
            et.setBackgroundResource(R.drawable.bg_search_box);
            et.setPadding(28, 16, 28, 16);
            et.setTextSize(15);
            et.setHintTextColor(Color.parseColor("#9AA3B5"));
            et.setTextColor(Color.parseColor("#2D3142"));
            et.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        layout.addView(name);
        layout.addView(teacher);
        layout.addView(location);
        layout.addView(weeks);
        layout.addView(day);
        layout.addView(startSec);
        layout.addView(endSec);

        new AlertDialog.Builder(this)
                .setTitle("添加课程")
                .setView(layout)
                .setPositiveButton("保存", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) { Toast.makeText(this, "课程名不能为空", Toast.LENGTH_SHORT).show(); return; }
                    int start = 0, end = 0;
                    try { start = Integer.parseInt(startSec.getText().toString()); end = Integer.parseInt(endSec.getText().toString()); } catch (Exception e) {}
                    if (start <= 0 || end < start) { Toast.makeText(this, "节次不对", Toast.LENGTH_SHORT).show(); return; }
                    Course c = new Course(n, teacher.getText().toString().trim(), location.getText().toString().trim(),
                            day.getSelectedItemPosition() + 1, start, end,
                            weeks.getText().toString().trim().isEmpty() ? "1-16" : weeks.getText().toString().trim());
                    db.add(c);
                    reload();
                    Toast.makeText(this, "已添加 " + n, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
