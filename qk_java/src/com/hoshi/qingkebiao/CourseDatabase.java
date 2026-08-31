package com.hoshi.qingkebiao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class CourseDatabase extends SQLiteOpenHelper {
    public static final String DB_NAME = "qingkebiao.db";
    public static final int DB_VERSION = 2;

    public CourseDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE courses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "teacher TEXT," +
                "location TEXT," +
                "day INTEGER," +
                "start INTEGER," +
                "end INTEGER," +
                "weeks TEXT," +
                "color INTEGER DEFAULT -1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS courses");
        onCreate(db);
    }

    public long add(Course c) {
        ContentValues v = new ContentValues();
        v.put("name", c.name);
        v.put("teacher", c.teacher);
        v.put("location", c.location);
        v.put("day", c.day);
        v.put("start", c.start);
        v.put("end", c.end);
        v.put("weeks", c.weeks);
        v.put("color", c.color);
        return getWritableDatabase().insert("courses", null, v);
    }

    public void delete(long id) {
        getWritableDatabase().delete("courses", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Course> getAll() {
        List<Course> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("courses", null, null, null, null, null, "day,start");
        while (c.moveToNext()) {
            Course x = new Course();
            x.id = c.getLong(c.getColumnIndexOrThrow("id"));
            x.name = c.getString(c.getColumnIndexOrThrow("name"));
            x.teacher = c.getString(c.getColumnIndexOrThrow("teacher"));
            x.location = c.getString(c.getColumnIndexOrThrow("location"));
            x.day = c.getInt(c.getColumnIndexOrThrow("day"));
            x.start = c.getInt(c.getColumnIndexOrThrow("start"));
            x.end = c.getInt(c.getColumnIndexOrThrow("end"));
            x.weeks = c.getString(c.getColumnIndexOrThrow("weeks"));
            list.add(x);
        }
        c.close();
        return list;
    }


    public Course getById(long id) {
        Cursor c = getReadableDatabase().query("courses", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        Course x = null;
        if (c.moveToFirst()) {
            x = new Course();
            x.id = c.getLong(c.getColumnIndexOrThrow("id"));
            x.name = c.getString(c.getColumnIndexOrThrow("name"));
            x.teacher = c.getString(c.getColumnIndexOrThrow("teacher"));
            x.location = c.getString(c.getColumnIndexOrThrow("location"));
            x.day = c.getInt(c.getColumnIndexOrThrow("day"));
            x.start = c.getInt(c.getColumnIndexOrThrow("start"));
            x.end = c.getInt(c.getColumnIndexOrThrow("end"));
            x.weeks = c.getString(c.getColumnIndexOrThrow("weeks"));
            x.color = c.getInt(c.getColumnIndexOrThrow("color"));
        }
        c.close();
        return x;
    }

    public List<Course> getByDay(int day) {
        List<Course> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("courses", null, "day=?", new String[]{String.valueOf(day)}, null, null, "start");
        while (c.moveToNext()) {
            Course x = new Course();
            x.id = c.getLong(c.getColumnIndexOrThrow("id"));
            x.name = c.getString(c.getColumnIndexOrThrow("name"));
            x.teacher = c.getString(c.getColumnIndexOrThrow("teacher"));
            x.location = c.getString(c.getColumnIndexOrThrow("location"));
            x.day = c.getInt(c.getColumnIndexOrThrow("day"));
            x.start = c.getInt(c.getColumnIndexOrThrow("start"));
            x.end = c.getInt(c.getColumnIndexOrThrow("end"));
            x.weeks = c.getString(c.getColumnIndexOrThrow("weeks"));
            x.color = c.getInt(c.getColumnIndexOrThrow("color"));
            list.add(x);
        }
        c.close();
        return list;
    }

    public long clear() {
        return getWritableDatabase().delete("courses", null, null);
    }
}
