package com.ztai.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    private static final String SERVER_URL = "https://ztai-production.up.railway.app";
    private EditText etKey;
    private TextView tvStatus;
    private Button btnActivate;
    private String key;
    private final OkHttpClient client = new OkHttpClient();
    private LinearLayout layout;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#0a0e1a"));
        layout.setPadding(50, 50, 50, 50);

        // Проверяем, активирован ли уже ключ
        String savedKey = prefs.getString("key", null);
        if (savedKey != null) {
            showStatusScreen();
        } else {
            showLoginScreen();
        }
        
        setContentView(layout);
    }

    private void showLoginScreen() {
        layout.removeAllViews();

        TextView tvTitle = new TextView(this);
        tvTitle.setText("ZTai");
        tvTitle.setTextSize(32);
        tvTitle.setTextColor(Color.parseColor("#a855f7"));
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        etKey = new EditText(this);
        etKey.setHint("Введите ключ активации");
        etKey.setTextColor(Color.WHITE);
        etKey.setHintTextColor(Color.GRAY);
        layout.addView(etKey);

        tvStatus = new TextView(this);
        tvStatus.setText("Ожидание...");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setPadding(0, 20, 0, 20);
        layout.addView(tvStatus);

        btnActivate = new Button(this);
        btnActivate.setText("Активировать");
        btnActivate.setBackgroundColor(Color.parseColor("#a855f7"));
        btnActivate.setTextColor(Color.WHITE);
        layout.addView(btnActivate);

        btnActivate.setOnClickListener(v -> {
            key = etKey.getText().toString().trim();
            if (key.isEmpty()) {
                tvStatus.setText("Введите ключ");
                return;
            }
            activateKey();
        });
    }

    private void showStatusScreen() {
        layout.removeAllViews();
        String savedKey = prefs.getString("key", "N/A");
        long expires = prefs.getLong("expires", 0);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✅ Ключ активирован");
        tvTitle.setTextSize(24);
        tvTitle.setTextColor(Color.GREEN);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 30);
        layout.addView(tvTitle);

        TextView tvKeyInfo = new TextView(this);
        tvKeyInfo.setText("Ваш ключ: " + savedKey);
        tvKeyInfo.setTextColor(Color.WHITE);
        tvKeyInfo.setGravity(Gravity.CENTER);
        layout.addView(tvKeyInfo);

        TextView tvExpires = new TextView(this);
        if (expires > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(expires));
            tvExpires.setText("Действует до: " + dateStr);
        } else {
            tvExpires.setText("Срок действия: неограничен");
        }
        tvExpires.setTextColor(Color.LTGRAY);
        tvExpires.setGravity(Gravity.CENTER);
        tvExpires.setPadding(0, 10, 0, 30);
        layout.addView(tvExpires);

        Button btnOpenMenu = new Button(this);
        btnOpenMenu.setText("Открыть меню (Оверлей)");
        btnOpenMenu.setBackgroundColor(Color.parseColor("#a855f7"));
        btnOpenMenu.setTextColor(Color.WHITE);
        layout.addView(btnOpenMenu);
        btnOpenMenu.setOnClickListener(v -> checkAndLaunchUI());

        Button btnBuy = new Button(this);
        btnBuy.setText("Купить новый ключ в Telegram");
        btnBuy.setBackgroundColor(Color.TRANSPARENT);
        btnBuy.setTextColor(Color.parseColor("#00d4ff"));
        layout.addView(btnBuy);
        btnBuy.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/zynvpn_originalbot"));
            startActivity(intent);
        });

        Button btnReset = new Button(this);
        btnReset.setText("Сбросить ключ");
        btnReset.setTextColor(Color.RED);
        btnReset.setBackgroundColor(Color.TRANSPARENT);
        layout.addView(btnReset);
        btnReset.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            showLoginScreen();
        });
    }

    private void activateKey() {
        tvStatus.setText("Проверка...");
        tvStatus.setTextColor(Color.YELLOW);

        JSONObject json = new JSONObject();
        try {
            json.put("key", key);
            json.put("device_id", fetchMyDeviceId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(SERVER_URL + "/api/check_key")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setTextColor(Color.RED);
                    tvStatus.setText("Ошибка сети: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(respStr);
                        if (json.optBoolean("ok", false)) {
                            prefs.edit()
                                    .putString("key", key)
                                    .putLong("expires", json.optLong("expires", 0))
                                    .apply();
                            
                            showStatusScreen();
                        } else {
                            String error = json.optString("error", "unknown");
                            String msg;
                            if (error.equals("invalid_key")) msg = "❌ Неверный ключ";
                            else if (error.equals("expired")) msg = "❌ Ключ истёк";
                            else msg = "❌ Ошибка: " + error;
                            tvStatus.setTextColor(Color.RED);
                            tvStatus.setText(msg);
                        }
                    } catch (Exception e) {
                        tvStatus.setTextColor(Color.RED);
                        tvStatus.setText("Ошибка ответа сервера");
                    }
                });
            }
        });
    }

    private void checkAndLaunchUI() {
        if (Settings.canDrawOverlays(this)) {
            launchBotUI();
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 101);
        }
    }

    private void launchBotUI() {
        startService(new Intent(this, BotService.class));
        startService(new Intent(this, FloatingService.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (Settings.canDrawOverlays(this)) {
                launchBotUI();
            } else {
                tvStatus.setText("Нужно разрешение на оверлей");
                tvStatus.setTextColor(Color.RED);
            }
        }
    }

    private String fetchMyDeviceId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
