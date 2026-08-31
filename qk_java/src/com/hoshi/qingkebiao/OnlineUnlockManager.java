package com.hoshi.qingkebiao;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 联机解锁客户端。
 *
 * 约定 API（后续可换成真实接口）：
 * GET  /api/qingkebiao/unlock?device=DEVICE_CODE
 *      -> {"unlocked": true|false}
 * POST /api/qingkebiao/unlock
 *      body: {"device":"DEVICE_CODE", "proof":"订单号/昵称/激活码"}
 *      -> {"unlocked": true|false}
 *
 * 服务器地址在 res/values/strings.xml 的 server_unlock_url 里配置。
 */
public class OnlineUnlockManager {
    private static final int TIMEOUT_MS = 8000;

    private OnlineUnlockManager() {}

    public static boolean checkUnlocked(Context context) {
        String base = baseUrl(context);
        if (base == null || base.trim().isEmpty()) {
            return false;
        }
        try {
            String urlStr = base + "/api/qingkebiao/unlock?device="
                    + URLEncoder.encode(UnlockManager.deviceCode(context), "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            int code = conn.getResponseCode();
            if (code == 200) {
                String body = readStream(conn.getInputStream());
                JSONObject obj = new JSONObject(body);
                return obj.optBoolean("unlocked", false);
            }
        } catch (Exception ignored) {
            // 网络不可用或服务端未就绪时静默失败，客户端继续用离线激活码
        }
        return false;
    }

    public static boolean activateOnline(Context context, String proof) {
        String base = baseUrl(context);
        if (base == null || base.trim().isEmpty()) {
            return false;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("device", UnlockManager.deviceCode(context));
            body.put("proof", proof == null ? "" : proof);
            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);

            HttpURLConnection conn = (HttpURLConnection) new URL(base + "/api/qingkebiao/unlock").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));

            try (OutputStream out = conn.getOutputStream()) {
                out.write(data);
            }
            int code = conn.getResponseCode();
            if (code == 200) {
                String res = readStream(conn.getInputStream());
                JSONObject obj = new JSONObject(res);
                return obj.optBoolean("unlocked", false);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String baseUrl(Context context) {
        return context.getString(R.string.server_unlock_url);
    }

    private static String readStream(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
