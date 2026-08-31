package com.hoshi.qingkebiao;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ImportActivity extends Activity {
    private CourseDatabase db;
    private static final int REQ_FILE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        db = new CourseDatabase(this);
        ((Button) findViewById(R.id.btn_web)).setOnClickListener(v -> startActivity(new Intent(this, WebImportActivity.class)));
        ((Button) findViewById(R.id.btn_file)).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQ_FILE);
        });
    }

    private static boolean isXls(byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && (bytes[0] & 0xFF) == 0xD0
                && (bytes[1] & 0xFF) == 0xCF
                && (bytes[2] & 0xFF) == 0x11
                && (bytes[3] & 0xFF) == 0xE0;
    }

    private static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                byte[] bytes = readAll(is);
                String head = new String(bytes, StandardCharsets.UTF_8);
                int count = 0;

                if (isXls(bytes)) {
                    List<Course> list = CourseXlsParser.parse(new ByteArrayInputStream(bytes));
                    for (Course c : list) {
                        db.add(c);
                        count++;
                    }
                } else if (head.trim().startsWith("<") || head.trim().startsWith("<?xml")) {
                    List<Course> list = CourseXmlParser.parse(new ByteArrayInputStream(bytes));
                    for (Course c : list) {
                        db.add(c);
                        count++;
                    }
                } else {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("课程名") || line.trim().isEmpty()) continue;
                        String[] p = line.split(",");
                        if (p.length >= 7) {
                            Course c = new Course(p[0].trim(), p[1].trim(), p[2].trim(),
                                    Integer.parseInt(p[3].trim()), Integer.parseInt(p[4].trim()),
                                    Integer.parseInt(p[5].trim()), p[6].trim());
                            db.add(c);
                            count++;
                        }
                    }
                }
                Toast.makeText(this, "导入 " + count + " 门课程", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
