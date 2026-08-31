package com.hoshi.qingkebiao;

public class Course {
    public long id;
    public String name;
    public String teacher;
    public String location;
    public int day; // 1-7
    public int start;
    public int end;
    public String weeks;
    public int color = -1;

    public Course() {}
    public Course(String name, String teacher, String location, int day, int start, int end, String weeks) {
        this.name = name;
        this.teacher = teacher;
        this.location = location;
        this.day = day;
        this.start = start;
        this.end = end;
        this.weeks = weeks;
    }

    public Course(String name, String teacher, String location, int day, int start, int end, String weeks, int color) {
        this(name, teacher, location, day, start, end, weeks);
        this.color = color;
    }

    public String sectionText() {
        return "第" + start + "-" + end + "节";
    }

    public boolean isInWeek(int week) {
        if (weeks == null || weeks.trim().isEmpty()) {
            return true;
        }
        String[] parts = weeks.split("[,，、]");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            if (p.matches("\\d+")) {
                if (Integer.parseInt(p) == week) return true;
            } else if (p.matches("\\d+\\s*-\\s*\\d+")) {
                String[] range = p.split("\\s*-\\s*");
                int startW = Integer.parseInt(range[0]);
                int endW = Integer.parseInt(range[1]);
                if (week >= startW && week <= endW) return true;
            }
        }
        return false;
    }
}
