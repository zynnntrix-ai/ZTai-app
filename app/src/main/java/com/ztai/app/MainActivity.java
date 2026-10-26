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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем интерфейс программно, чтобы не зависеть от XML
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#0a0e1a"));
        layout.setPadding(50, 50, 50, 50);

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

        setContentView(layout);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String savedKey = prefs.getString("key", null);
        
        if (savedKey != null) {
            key = savedKey;
            etKey.setText(key);
            tvStatus.setText("Ключ уже активирован");
            tvStatus.setTextColor(Color.GREEN);
            btnActivate.setText("Открыть меню");
            btnActivate.setOnClickListener(v -> checkAndLaunchUI());
            return;
        }

        btnActivate.setOnClickListener(v -> {
            key = etKey.getText().toString().trim();
            if (key.isEmpty()) {
                tvStatus.setText("Введите ключ");
                return;
            }
            activateKey();
        });
    }

    private void activateKey() {
        tvStatus.setText("Проверка...");
        tvStatus.setTextColor(Color.YELLOW);

        JSONObject json = new JSONObject();
        try {
            json.put("key", key);
            json.put("device_id", getDeviceId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(SERVER_URL + "/api/activate")
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
                            getSharedPreferences("prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("key", key)
                                    .putLong("expires", json.optLong("expires", 0))
                                    .apply();

                            tvStatus.setTextColor(Color.GREEN);
                            tvStatus.setText("✅ Ключ активирован!");
                            
                            btnActivate.setText("Открыть меню");
                            btnActivate.setOnClickListener(v -> checkAndLaunchUI());

                            startService(new Intent(MainActivity.this, BotService.class));
                            checkAndLaunchUI();

                        } else {
                            String error = json.optString("error", "unknown");
                            String msg = "";
                            if (error.equals("invalid_key")) msg = "❌ Неверный ключ";
                            else if (error.equals("expired")) msg = "❌ Ключ истёк";
                            else msg = "❌ Ошибка: " + error;
                            tvStatus.setTextColor(Color.RED);
                            tvStatus.setText(msg);
                        }
                    } catch (Exception e) {
                        tvStatus.setTextColor(Color.RED);
                        tvStatus.setText("Ошибка парсинга ответа");
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

    // Переименовали метод, чтобы не конфликтовать с системным
    private String fetchMyDeviceId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}

