package com.hoshi.qingkebiao;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebImportActivity extends Activity {
    private static final String PREF_SCHOOL_URL = "school_url";
    private static final String URL_XAUT_WEBVPN = "https://webvpn.xaut.edu.cn/login";
    private static final String URL_XAUT_DIRECT = "http://jwgl.xaut.edu.cn/jsxsd/sso.jsp";

    private static final String IMPORT_JS =
            "(function() {" +
            "  try {" +
            "    function blockText(html) {" +
            "      var div = document.createElement('div');" +
            "      div.innerHTML = html;" +
            "      var t = div.innerText || div.textContent || '';" +
            "      return t.replace(/\\s+/g, ' ').trim();" +
            "    }" +
            "    var tables = document.querySelectorAll('table');" +
            "    var best = null;" +
            "    for (var i = 0; i < tables.length; i++) {" +
            "      var t = tables[i].innerText || '';" +
            "      if (t.indexOf('节次') >= 0 || t.indexOf('星期') >= 0 || t.indexOf('课程') >= 0) {" +
            "        best = tables[i]; break;" +
            "      }" +
            "    }" +
            "    if (!best && tables.length > 0) best = tables[0];" +
            "    if (!best) return JSON.stringify({ok:false,msg:'未找到课表表格'});" +
            "    var result = [];" +
            "    var rows = best.rows;" +
            "    for (var r = 1; r < rows.length; r++) {" +
            "      var cells = rows[r].cells;" +
            "      if (!cells || cells.length < 2) continue;" +
            "      var secText = cells[0].innerText.replace(/\\s+/g, ' ').trim();" +
            "      var secMatch = secText.match(/(\\d+)\\s*[-~—]\\s*(\\d+)/);" +
            "      var start = 1, end = 1;" +
            "      if (secMatch) { start = parseInt(secMatch[1]); end = parseInt(secMatch[2]); }" +
            "      else {" +
            "        var nums = secText.split(/[^0-9]+/).filter(function(x){ return x; });" +
            "        if (nums.length >= 2) { start = parseInt(nums[0]); end = parseInt(nums[1]); }" +
            "        else if (nums.length === 1) { start = end = parseInt(nums[0]); }" +
            "      }" +
            "      for (var c = 1; c < cells.length && c <= 7; c++) {" +
            "        var blocks = cells[c].innerHTML.split(/<br[^>]*>/i);" +
            "        for (var b = 0; b < blocks.length; b++) {" +
            "          var text = blockText(blocks[b]);" +
            "          if (!text) continue;" +
            "          if (/^(?:第)?\\d+\\s*[-~—]?\\s*\\d*节?$/.test(text)) continue;" +
            "          result.push({day:c, start:start, end:end, text:text});" +
            "        }" +
            "      }" +
            "    }" +
            "    return JSON.stringify({ok:true, courses:result});" +
            "  } catch (e) {" +
            "    return JSON.stringify({ok:false, msg:String(e)});" +
            "  }" +
            "})();";

    private WebView web;
    private CourseDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_import);
        db = new CourseDatabase(this);
        web = findViewById(R.id.webview);
        final View controlsPanel = findViewById(R.id.controls_panel);
        final EditText etUrl = findViewById(R.id.et_school_url);
        ((Button) findViewById(R.id.btn_reset_zoom)).setOnClickListener(v -> {
            web.setInitialScale(0);
            web.reload();
            Toast.makeText(this, "已重置页面缩放", Toast.LENGTH_SHORT).show();
        });
        ((Button) findViewById(R.id.btn_toggle_controls)).setOnClickListener(v -> {
            if (controlsPanel.getVisibility() == View.GONE) {
                controlsPanel.setVisibility(View.VISIBLE);
            } else {
                controlsPanel.setVisibility(View.GONE);
            }
        });
        SharedPreferences sp = getSharedPreferences("qingkebiao", MODE_PRIVATE);
        String savedUrl = sp.getString(PREF_SCHOOL_URL, "");
        etUrl.setText(savedUrl);

        Spinner schoolSpinner = findViewById(R.id.sp_school_preset);
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"自定义网址", "西安理工大学（WebVPN）", "西安理工大学（教务直连）"});
        schoolSpinner.setAdapter(presetAdapter);
        int presetPos = 0;
        if (savedUrl != null && savedUrl.contains("webvpn")) {
            presetPos = 1;
        } else if (savedUrl != null && savedUrl.contains("jwgl")) {
            presetPos = 2;
        }
        schoolSpinner.setSelection(presetPos);
        schoolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    etUrl.setText(URL_XAUT_WEBVPN);
                } else if (position == 2) {
                    etUrl.setText(URL_XAUT_DIRECT);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        // 模拟电脑浏览器，让教务系统返回桌面版页面
        // 模拟 Edge/Chrome 电脑版：完整桌面布局 + 横向滚动，右半部分可通过滑动查看
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        web.setWebViewClient(new WebViewClient());
        if (savedUrl != null && !savedUrl.trim().isEmpty()) {
            web.loadUrl(savedUrl.trim());
        }

        // 拦截“导出课表”产生的下载，并把下载文件自动尝试识别为 XML 导入
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Toast.makeText(this, "检测到下载，正在处理…", Toast.LENGTH_SHORT).show();
            downloadAndImport(url, userAgent, contentDisposition, mimeType);
        });

        ((Button) findViewById(R.id.btn_open_schedule)).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "请先输入学校教务系统网址", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
                etUrl.setText(url);
            }
            sp.edit().putString(PREF_SCHOOL_URL, url).apply();
            web.setInitialScale(0);
            web.loadUrl(url);
            controlsPanel.setVisibility(View.GONE);
        });
        ((Button) findViewById(R.id.btn_import_schedule)).setOnClickListener(v -> importCurrentPage());
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void importCurrentPage() {
        web.evaluateJavascript(IMPORT_JS, value -> {
            try {
                String raw = value;
                if (raw == null || raw.equals("null")) {
                    runOnUiThread(() -> Toast.makeText(this, "当前页面没有可解析的课表", Toast.LENGTH_LONG).show());
                    return;
                }
                if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                    raw = raw.substring(1, raw.length() - 1)
                            .replace("\\/", "/")
                            .replace("\\\"", "\"");
                }
                JSONObject obj = new JSONObject(raw);
                if (!obj.optBoolean("ok", false)) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "未找到课表：" + obj.optString("msg", "请先打开课表页"), Toast.LENGTH_LONG).show());
                    return;
                }
                JSONArray courses = obj.optJSONArray("courses");
                int count = 0;
                if (courses != null) {
                    for (int i = 0; i < courses.length(); i++) {
                        JSONObject item = courses.getJSONObject(i);
                        String text = item.optString("text", "");
                        if (text.isEmpty()) continue;
                        Course c = parseCourse(item.optInt("day"), item.optInt("start"),
                                item.optInt("end"), text);
                        if (c != null) {
                            db.add(c);
                            count++;
                        }
                    }
                }
                final int finalCount = count;
                runOnUiThread(() -> {
                    if (finalCount > 0) {
                        Toast.makeText(this, "已导入 " + finalCount + " 门课程", Toast.LENGTH_LONG).show();
                        goHome();
                    } else {
                        Toast.makeText(this, "没有解析到课程，请确认当前是课表页面", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "解析失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void downloadAndImport(final String url, final String userAgent,
                                   final String contentDisposition, final String mimeType) {
        Thread t = new Thread(() -> {
            File file = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", userAgent != null
                        ? userAgent : web.getSettings().getUserAgentString());
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) {
                    conn.setRequestProperty("Cookie", cookie);
                }

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    final int cc = code;
                    runOnUiThread(() -> Toast.makeText(this, "下载失败 HTTP " + cc, Toast.LENGTH_LONG).show());
                    return;
                }

                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                if (!fileName.toLowerCase().endsWith(".xml")
                        && !fileName.toLowerCase().endsWith(".csv")) {
                    // 如果文件名分辨不出，仍按内容解析，先保存原文件名
                }
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) dir = getFilesDir();
                file = new File(dir, fileName);

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }

                File finalFile = file;
                runOnUiThread(() -> importDownloadedFile(finalFile));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "下载失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        t.start();
    }

    private void importDownloadedFile(File file) {
        try {
            byte[] bytes;
            try (FileInputStream fis = new FileInputStream(file)) {
                bytes = readAll(fis);
            }
            String head = new String(bytes, StandardCharsets.UTF_8).trim();

            boolean xls = bytes.length >= 4
                    && (bytes[0] & 0xFF) == 0xD0
                    && (bytes[1] & 0xFF) == 0xCF
                    && (bytes[2] & 0xFF) == 0x11
                    && (bytes[3] & 0xFF) == 0xE0;
            if (xls) {
                List<Course> list = CourseXlsParser.parse(new ByteArrayInputStream(bytes));
                int count = 0;
                for (Course c : list) {
                    db.add(c);
                    count++;
                }
                if (count > 0) {
                    Toast.makeText(this, "已从课表 XLS 导入 " + count + " 门课程", Toast.LENGTH_LONG).show();
                    goHome();
                } else {
                    Toast.makeText(this, "已下载，但没有解析到课程", Toast.LENGTH_LONG).show();
                }
            } else if (head.startsWith("<") || head.startsWith("<?xml")) {
                List<Course> list = CourseXmlParser.parse(new ByteArrayInputStream(bytes));
                int count = 0;
                for (Course c : list) {
                    db.add(c);
                    count++;
                }
                if (count > 0) {
                    Toast.makeText(this, "已从 XML 导入 " + count + " 门课程", Toast.LENGTH_LONG).show();
                    goHome();
                } else {
                    Toast.makeText(this, "已下载，但没有解析到课程 XML", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "已下载到：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "解析下载文件失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

    private Course parseCourse(int day, int start, int end, String text) {
        try {
            String normalized = text.replaceAll("\\s+", " ").trim();
            String weeks = "";
            Matcher m = Pattern.compile("(\\d+(?:-\\d+)?(?:,\\d+)*)\\s*周").matcher(normalized);
            if (m.find()) {
                weeks = m.group(1);
            }
            normalized = normalized.replaceAll("(\\d+(?:-\\d+)?(?:,\\d+)*)\\s*周", " ").trim();

            String location = "";
            String[] locHints = {"楼", "室", "实验", "教", "馆", "机房", "校区"};
            String[] parts = normalized.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i];
                if (p.isEmpty()) continue;
                if (p.length() > 1 && p.matches(".*[0-9].*")) {
                    for (String hint : locHints) {
                        if (p.contains(hint) || p.matches(".*[A-Za-z]\\d+.*")) {
                            location = p;
                            parts[i] = "";
                            break;
                        }
                    }
                    if (location.isEmpty()) continue;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(p);
                }
            }
            String remain = sb.toString().trim();
            String[] remainParts = remain.split("\\s+");
            if (remainParts.length == 0) return null;
            String name = remainParts[0];
            String teacher = "";
            if (remainParts.length > 1) {
                teacher = remainParts[1];
            }
            if (weeks.isEmpty()) weeks = "1-16";
            return new Course(name, teacher, location, day, start, end, weeks);
        } catch (Exception e) {
            return null;
        }
    }
}
