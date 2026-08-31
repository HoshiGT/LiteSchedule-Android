package com.hoshi.qingkebiao;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class BackgroundActivity extends Activity {
    private static final int REQ_IMAGE = 101;

    private TextView tvLockStatus;
    private TextView tvDeviceCode;
    private TextView tvChoose;
    private ImageView imgQr;
    private ImageView imgCustomPreview;
    private Button btnUnlock;
    private Button btnCopyDevice;
    private Button btnChooseImage;
    private EditText etActivation;
    private LinearLayout lockPanel;
    private LinearLayout unlockedPanel;
    private LinearLayout bgOptions;
    private LinearLayout root;
    private View[] optionViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background);

        tvLockStatus = findViewById(R.id.tv_lock_status);
        tvDeviceCode = findViewById(R.id.tv_device_code);
        tvChoose = findViewById(R.id.tv_choose);

        imgQr = findViewById(R.id.img_qr);
        imgCustomPreview = findViewById(R.id.img_custom_preview);
        btnUnlock = findViewById(R.id.btn_unlock);
        btnCopyDevice = findViewById(R.id.btn_copy_device);
        btnChooseImage = findViewById(R.id.btn_choose_image);
        etActivation = findViewById(R.id.et_activation);
        lockPanel = findViewById(R.id.lock_panel);
        unlockedPanel = findViewById(R.id.unlocked_panel);
        bgOptions = findViewById(R.id.bg_options);
        root = findViewById(R.id.bg_root);

        tvDeviceCode.setText(UnlockManager.deviceCode(this));

        btnCopyDevice.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("设备码", tvDeviceCode.getText().toString()));
            Toast.makeText(this, "设备码已复制", Toast.LENGTH_SHORT).show();
        });

        btnUnlock.setOnClickListener(v -> {
            String input = etActivation.getText().toString().trim();
            if (OnlineUnlockManager.activateOnline(this, input)) {
                BackgroundManager.setUnlocked(this, true);
                Toast.makeText(this, "服务端验证成功，感谢支持！", Toast.LENGTH_SHORT).show();
                updateUi();
            } else if (UnlockManager.activate(this, input)) {
                BackgroundManager.setUnlocked(this, true);
                Toast.makeText(this, "激活码验证成功，感谢支持！", Toast.LENGTH_SHORT).show();
                updateUi();
            } else {
                Toast.makeText(this, "验证失败：请检查凭证/激活码，或确认服务端已标记解锁", Toast.LENGTH_LONG).show();
            }
        });

        btnChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_IMAGE);
        });

        buildOptions();
        updateUi();
        checkOnlineUnlockAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOnlineUnlockAsync();
    }

    private void checkOnlineUnlockAsync() {
        Thread t = new Thread(() -> {
            final boolean unlocked = OnlineUnlockManager.checkUnlocked(this);
            if (unlocked) {
                runOnUiThread(() -> {
                    if (!BackgroundManager.isUnlocked(this)) {
                        BackgroundManager.setUnlocked(this, true);
                        Toast.makeText(this, "服务端已标记解锁", Toast.LENGTH_SHORT).show();
                        updateUi();
                    }
                });
            }
        });
        t.start();
    }

    private void buildOptions() {
        String[] names = BackgroundManager.NAMES;
        int[] res = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            res[i] = BackgroundManager.backgroundRes(i);
        }
        optionViews = new View[names.length];
        for (int i = 0; i < names.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundResource(R.drawable.bg_card);
            row.setPadding(16, 14, 16, 14);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 8, 0, 8);
            row.setLayoutParams(rowLp);

            View colorView = new View(this);
            colorView.setBackgroundResource(res[i]);
            LinearLayout.LayoutParams colorLp = new LinearLayout.LayoutParams(44, 44);
            colorView.setLayoutParams(colorLp);

            TextView text = new TextView(this);
            text.setText(names[i]);
            text.setTextSize(16);
            text.setTextColor(0xFF2D3142);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textLp.setMargins(16, 0, 0, 0);
            text.setLayoutParams(textLp);

            row.addView(colorView);
            row.addView(text);

            final int index = i;
            row.setOnClickListener(v -> {
                if (!BackgroundManager.isUnlocked(this)) {
                    return;
                }
                BackgroundManager.setImageMode(this, false);
                BackgroundManager.setIndex(this, index);
                updateUi();
                Toast.makeText(this, "已应用：" + BackgroundManager.NAMES[index], Toast.LENGTH_SHORT).show();
            });

            bgOptions.addView(row);
            optionViews[i] = row;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            File dest = new File(getFilesDir(), "background.jpg");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                BackgroundManager.setImagePath(this, dest.getAbsolutePath());
                BackgroundManager.setImageMode(this, true);
                updateUi();
                Toast.makeText(this, "背景图片已导入", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateUi() {
        boolean unlocked = BackgroundManager.isUnlocked(this);
        if (unlocked) {
            tvLockStatus.setText("已解锁，可切换预设背景或导入本地图片");
            lockPanel.setVisibility(View.GONE);
            unlockedPanel.setVisibility(View.VISIBLE);
            tvChoose.setText(BackgroundManager.isImageMode(this) ? "当前使用导入图片，也可切换预设背景" : "选择背景");
            root.setBackgroundResource(BackgroundManager.backgroundRes(this));
            showImagePreviewIfNeeded();
        } else {
            tvLockStatus.setText("赞赏 1 元后，把下方设备码发给作者，兑换激活码解锁");
            lockPanel.setVisibility(View.VISIBLE);
            unlockedPanel.setVisibility(View.GONE);
            imgCustomPreview.setVisibility(View.GONE);
            root.setBackgroundResource(R.drawable.bg_home_default);
        }
        int selected = BackgroundManager.getIndex(this);
        if (optionViews != null) {
            for (int i = 0; i < optionViews.length; i++) {
                if (optionViews[i] != null) {
                    optionViews[i].setAlpha(i == selected && !BackgroundManager.isImageMode(this) ? 1f : 0.55f);
                }
            }
        }
    }

    private void showImagePreviewIfNeeded() {
        String path = BackgroundManager.getImagePath(this);
        if (BackgroundManager.isImageMode(this) && path != null && new File(path).exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(path);
            if (bmp != null) {
                imgCustomPreview.setImageBitmap(bmp);
                imgCustomPreview.setVisibility(View.VISIBLE);
                return;
            }
        }
        imgCustomPreview.setVisibility(View.GONE);
    }
}
