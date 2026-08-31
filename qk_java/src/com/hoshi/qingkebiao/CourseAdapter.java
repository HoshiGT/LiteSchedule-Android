package com.hoshi.qingkebiao;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class CourseAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private List<Course> courses = new ArrayList<>();

    public CourseAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setCourses(List<Course> list) {
        this.courses = list != null ? list : new ArrayList<Course>();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return courses.size();
    }

    @Override
    public Course getItem(int position) {
        return courses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return courses.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_course, parent, false);
        }
        Course c = getItem(position);
        TextView name = convertView.findViewById(R.id.tv_course_name);
        TextView detail = convertView.findViewById(R.id.tv_course_detail);

        String dayText = "周" + "一二三四五六日".charAt(c.day - 1);
        name.setText(dayText + " " + c.name + "  " + c.sectionText());
        StringBuilder sb = new StringBuilder();
        if (c.location != null && !c.location.isEmpty()) {
            sb.append(c.location);
        }
        if (c.teacher != null && !c.teacher.isEmpty()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(c.teacher);
        }
        if (c.weeks != null && !c.weeks.isEmpty()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append("周次 ").append(c.weeks);
        }
        detail.setText(sb.length() == 0 ? "暂无详情" : sb.toString());
        return convertView;
    }
}
