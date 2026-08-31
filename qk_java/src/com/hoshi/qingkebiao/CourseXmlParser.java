package com.hoshi.qingkebiao;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 兼容常见教务/课表导出 XML 的解析器。
 * 支持：
 * 1. <course> / <row> / <item> 节点，子节点或属性带课程字段
 * 2. 常见中英文标签：kcmc/jsxm/cdmc/xqj/jc/zcd 等
 * 3. 如果文件是多个 <course> 一个接一个，也能识别
 */
public class CourseXmlParser {

    private static final String[] NAME_TAGS = {
            "kcmc", "kcm", "course_name", "coursename", "name", "课程名称", "课程名"
    };
    private static final String[] TEACHER_TAGS = {
            "jsxm", "teacher", "teacher_name", "teachername", "教师", "任课教师"
    };
    private static final String[] LOCATION_TAGS = {
            "cdmc", "room", "classroom", "location", "place", "教室", "地点"
    };
    private static final String[] DAY_TAGS = {
            "xqj", "day", "weekday", "星期", "周几", "周数"
    };
    private static final String[] START_TAGS = {
            "jc", "start_section", "start", "上节", "节次", "开始节", "起始节"
    };
    private static final String[] END_TAGS = {
            "end_section", "end", "下节", "结束节", "终止节"
    };
    private static final String[] WEEKS_TAGS = {
            "zcd", "weeks", "week", "周次", "周", "上课周"
    };

    private CourseXmlParser() {}

    public static List<Course> parse(InputStream is) throws Exception {
        List<Course> list = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(is, null);

        // 先做一遍轻量扫描：找 course/row/item 节点。
        // 如果找不到，就用“把每个有课程名的元素当一个课程”的兜底。
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                String lower = tag.toLowerCase();
                if (lower.equals("course") || lower.equals("row") || lower.equals("item")) {
                    Course c = parseOneElement(parser, tag);
                    if (c != null && c.name != null && !c.name.trim().isEmpty()) {
                        list.add(c);
                    }
                }
            }
        }

        // 如果标准节点没导入到，尝试把所有带 name 属性的节点当课程
        //（防止某些导出用自定义节点名）
        if (list.isEmpty()) {
            XmlPullParser p2 = Xml.newPullParser();
            p2.setInput(is, null);
            while ((event = p2.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && hasAnyAttribute(p2, NAME_TAGS)) {
                    Course c = parseOneElementWithAttributesOnly(p2);
                    if (c != null && c.name != null && !c.name.trim().isEmpty()) {
                        list.add(c);
                    }
                }
            }
        }
        return list;
    }

    private static Course parseOneElement(XmlPullParser parser, String tag) throws Exception {
        Course c = new Course();
        int depth = parser.getDepth();
        String currentField = null;

        // 元素属性
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String key = parser.getAttributeName(i).toLowerCase();
            String value = parser.getAttributeValue(i).trim();
            assignField(c, key, value);
        }

        while (true) {
            int event = parser.next();
            if (event == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase(tag)
                    && parser.getDepth() == depth) {
                break;
            }
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName().toLowerCase();
                currentField = isFieldTag(name) ? name : null;
            } else if (event == XmlPullParser.TEXT) {
                if (currentField != null) {
                    String text = parser.getText();
                    if (text != null) {
                        assignField(c, currentField, text.trim());
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName().toLowerCase();
                if (isFieldTag(name)) {
                    currentField = null;
                }
            }
        }

        if (c.name == null || c.name.trim().isEmpty()) return null;
        if (c.day < 1 || c.day > 7) c.day = 1;
        if (c.start <= 0) c.start = 1;
        if (c.end < c.start) c.end = c.start;
        if (c.weeks == null || c.weeks.trim().isEmpty()) c.weeks = "1-16";
        return c;
    }

    private static Course parseOneElementWithAttributesOnly(XmlPullParser parser) {
        Course c = new Course();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            assignField(c, parser.getAttributeName(i).toLowerCase(), parser.getAttributeValue(i).trim());
        }
        if (c.name == null || c.name.trim().isEmpty()) return null;
        if (c.day < 1 || c.day > 7) c.day = 1;
        if (c.start <= 0) c.start = 1;
        if (c.end < c.start) c.end = c.start;
        if (c.weeks == null || c.weeks.trim().isEmpty()) c.weeks = "1-16";
        return c;
    }

    private static boolean hasAnyAttribute(XmlPullParser parser, String[] tags) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeName(i).toLowerCase();
            for (String t : tags) if (attr.equals(t)) return true;
        }
        return false;
    }

    private static boolean isFieldTag(String tag) {
        return contains(NAME_TAGS, tag) || contains(TEACHER_TAGS, tag)
                || contains(LOCATION_TAGS, tag) || contains(DAY_TAGS, tag)
                || contains(START_TAGS, tag) || contains(END_TAGS, tag)
                || contains(WEEKS_TAGS, tag);
    }

    private static boolean contains(String[] arr, String value) {
        for (String s : arr) if (s.equals(value)) return true;
        return false;
    }

    private static void assignField(Course c, String key, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        if (contains(NAME_TAGS, key)) c.name = v;
        else if (contains(TEACHER_TAGS, key)) c.teacher = v;
        else if (contains(LOCATION_TAGS, key)) c.location = v;
        else if (contains(DAY_TAGS, key)) c.day = parseInt(v, -1);
        else if (contains(START_TAGS, key)) c.start = parseInt(v, -1);
        else if (contains(END_TAGS, key)) c.end = parseInt(v, -1);
        else if (contains(WEEKS_TAGS, key)) c.weeks = v;
    }

    private static int parseInt(String s, int def) {
        try {
            int n = Integer.parseInt(s.replaceAll("\\D+", ""));
            return n;
        } catch (Exception e) {
            return def;
        }
    }
}
