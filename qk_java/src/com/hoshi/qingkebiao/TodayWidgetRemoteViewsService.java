package com.hoshi.qingkebiao;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TodayWidgetRemoteViewsService extends RemoteViewsService {

    private static final int[] COURSE_COLORS = {
            0xFFFFCDD2, 0xFFFFE0B2, 0xFFFFF9C4,
            0xFFC8E6C9, 0xFFB2DFDB, 0xFFBBDEFB,
            0xFFD1C4E9, 0xFFF8BBD0, 0xFFD7CCC8,
            0xFFCFD8DC
    };
    private static final int[] BUBBLE_BACKGROUNDS = {
            R.drawable.widget_bubble_0, R.drawable.widget_bubble_1,
            R.drawable.widget_bubble_2, R.drawable.widget_bubble_3,
            R.drawable.widget_bubble_4, R.drawable.widget_bubble_5,
            R.drawable.widget_bubble_6, R.drawable.widget_bubble_7,
            R.drawable.widget_bubble_8, R.drawable.widget_bubble_9
    };

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CoursesFactory(getApplicationContext());
    }

    private static class CoursesFactory implements RemoteViewsService.RemoteViewsFactory {
        private final Context context;
        private final List<Course> courses = new ArrayList<>();

        CoursesFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            courses.clear();
            CourseDatabase db = new CourseDatabase(context);
            int offset = context.getSharedPreferences("qingkebiao", Context.MODE_PRIVATE)
                    .getInt("widget_day_offset", 0);
            if (offset > 1) offset = 1;
            if (offset < 0) offset = 0;
            Calendar selected = Calendar.getInstance();
            selected.add(Calendar.DAY_OF_YEAR, offset);
            int day = selected.get(Calendar.DAY_OF_WEEK);
            int weekDay = (day == Calendar.SUNDAY) ? 7 : day - 1;
            courses.addAll(db.getByDay(weekDay));
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public int getCount() {
            return courses.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Course c = courses.get(position);
            RemoteViews item = new RemoteViews(context.getPackageName(), R.layout.widget_course_item);
            item.setTextViewText(R.id.widget_course_name, c.name);
            String info = c.sectionText();
            if (c.location != null && !c.location.isEmpty()) {
                info = info + " · " + c.location;
            }
            item.setTextViewText(R.id.widget_course_info, info);
            int idx = Math.abs(c.name.hashCode()) % BUBBLE_BACKGROUNDS.length;
            item.setInt(R.id.widget_course_item, "setBackgroundResource", BUBBLE_BACKGROUNDS[idx]);
            return item;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return courses.get(position).id;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
