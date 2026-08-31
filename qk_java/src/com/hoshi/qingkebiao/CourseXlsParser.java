package com.hoshi.qingkebiao;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析教务系统导出的 .xls 学生个人课表（JXL 生成格式）。
 * 表格结构：
 * 第 1 行：标题
 * 第 2 行：学年学期/班级信息
 * 第 3 行：星期一到星期日
 * 从第 4 行起：第一列是大节/节次，第 2~8 列是对应星期课程。
 * 单元格内多个课程用空行分隔。
 */
public class CourseXlsParser {

    private CourseXlsParser() {}

    public static List<Course> parse(InputStream is) throws Exception {
        List<Course> result = new ArrayList<>();
        Workbook workbook = Workbook.getWorkbook(is);
        try {
            Sheet sheet = workbook.getSheet(0);
            int headerRow = -1;
            for (int r = 0; r < sheet.getRows(); r++) {
                Cell c = sheet.getCell(0, r);
                if (c.getContents().contains("星期") || c.getContents().contains("周一")) {
                    headerRow = r;
                    break;
                }
            }
            if (headerRow < 0) headerRow = 2;

            for (int r = headerRow + 1; r < sheet.getRows(); r++) {
                String sectionText = sheet.getCell(0, r).getContents().trim();
                int[] sec = parseSection(sectionText);
                if (sec == null) continue;
                int start = sec[0];
                int end = sec[1];

                for (int c = 1; c <= 7; c++) {
                    String content = sheet.getCell(c, r).getContents();
                    if (content == null || content.trim().isEmpty()) continue;
                    for (String block : content.split("\\n\\s*\\n")) {
                        Course course = parseBlock(block, c, start, end);
                        if (course != null) {
                            result.add(course);
                        }
                    }
                }
            }
        } finally {
            workbook.close();
        }
        return result;
    }

    private static int[] parseSection(String sectionText) {
        Matcher m = Pattern.compile("\\((\\d+)\\s*,\\s*(\\d+)").matcher(sectionText);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        Matcher m2 = Pattern.compile("(\\d+)\\s*[-~—]\\s*(\\d+)").matcher(sectionText);
        if (m2.find()) {
            return new int[]{Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2))};
        }
        return null;
    }

    private static Course parseBlock(String block, int day, int defaultStart, int defaultEnd) {
        String[] lines = block.split("\\n");
        List<String> nonEmpty = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty()) nonEmpty.add(t);
        }
        if (nonEmpty.isEmpty()) return null;

        String name = nonEmpty.get(0);
        if (name.length() <= 1 && name.matches("[0-9]+")) return null;

        String teacher = "";
        String location = "";
        String weeks = "";

        for (int i = 1; i < nonEmpty.size(); i++) {
            String line = nonEmpty.get(i);
            Matcher m = Pattern.compile("(\\d+(?:-\\d+)?(?:,\\d+)*)\\s*(\\(\\[周\\]\\)|周)").matcher(line);
            if (m.find()) {
                weeks = m.group(1);
                continue;
            }
            if (looksLikeLocation(line)) {
                location = line;
                continue;
            }
            if (teacher.isEmpty()) {
                teacher = line;
            }
        }

        if (weeks.isEmpty()) {
            Matcher m = Pattern.compile("(\\d+(?:-\\d+)?(?:,\\d+)*)\\s*([周|周)])").matcher(block);
            if (m.find()) weeks = m.group(1);
        }
        if (weeks.isEmpty()) weeks = "1-16";

        return new Course(name, teacher, location, day, defaultStart, defaultEnd, weeks);
    }

    private static boolean looksLikeLocation(String line) {
        if (line.matches(".*(楼|室|馆|机房|校区|实验).*")) return true;
        if (line.matches(".*[A-Za-z]?\\d+.*") && line.length() > 2) return true;
        return false;
    }
}
