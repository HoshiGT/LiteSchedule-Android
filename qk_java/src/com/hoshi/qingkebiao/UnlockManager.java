package com.hoshi.qingkebiao;

import android.content.Context;
import android.provider.Settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 离线赞赏解锁。
 *
 * 流程：
 * 1. 用户赞赏后将 App 显示的“设备码”发给作者；
 * 2. 作者用同样算法根据设备码算出“激活码”发给用户；
 * 3. 用户在 App 内输入激活码完成本地解锁。
 *
 * 这个方案不依赖服务器/证书，适合当前 1 元赞赏场景。
 */
public class UnlockManager {
    // 作者侧和 App 内持有同一 secret，后续有服务端后可换成服务器验证。
    private static final String SECRET = "qkb-2026-hoshi-donate-1";

    private UnlockManager() {}

    public static String deviceCode(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null) {
            androidId = "unknown";
        }
        return sha256("qkb-device-" + androidId).substring(0, 8).toUpperCase(Locale.CHINA);
    }

    public static String activationCode(Context context) {
        return sha256(SECRET + "-" + deviceCode(context)).substring(0, 8).toUpperCase(Locale.CHINA);
    }

    public static boolean activate(Context context, String input) {
        if (input == null) {
            return false;
        }
        String code = input.trim().toUpperCase(Locale.CHINA);
        return !code.isEmpty() && code.equals(activationCode(context));
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
