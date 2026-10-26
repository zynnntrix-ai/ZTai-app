package com.ztai.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
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
    private LinearLayout mainLayout;
    private LinearLayout contentLayout;
    private SharedPreferences prefs;
    private LinearLayout tabBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0a0e1a"));

        // Контейнер для контента (меняется при переключении вкладок)
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        mainLayout.addView(contentLayout);

        // Нижняя панель вкладок
        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Color.parseColor("#1a1a2e"));
        tabBar.setPadding(0, 10, 0, 10);
        tabBar.setVisibility(View.GONE); // Покажем только после активации
        mainLayout.addView(tabBar);

        setContentView(mainLayout);

        String savedKey = prefs.getString("key", null);
        if (savedKey != null) {
            showMainTabs();
        } else {
            showLoginScreen();
        }
    }

    private void showLoginScreen() {
        contentLayout.removeAllViews();
        tabBar.setVisibility(View.GONE);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("ZTai");
        tvTitle.setTextSize(32);
        tvTitle.setTextColor(Color.parseColor("#a855f7"));
        tvTitle.setGravity(Gravity.CENTER);
        contentLayout.addView(tvTitle);

        etKey = new EditText(this);
        etKey.setHint("Введите ключ активации");
        etKey.setTextColor(Color.WHITE);
        etKey.setHintTextColor(Color.GRAY);
        contentLayout.addView(etKey);

        tvStatus = new TextView(this);
        tvStatus.setText("Ожидание...");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setPadding(0, 20, 0, 20);
        contentLayout.addView(tvStatus);

        btnActivate = new Button(this);
        btnActivate.setText("Активировать");
        btnActivate.setBackgroundColor(Color.parseColor("#a855f7"));
        btnActivate.setTextColor(Color.WHITE);
        contentLayout.addView(btnActivate);

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
                            showMainTabs();
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

    private void showMainTabs() {
        tabBar.setVisibility(View.VISIBLE);
        tabBar.removeAllViews();

        String[] tabs = {"ГЛАВНАЯ", "БОТ", "СТАТИСТИКА", "НАСТРОЙКИ"};
        for (String tabName : tabs) {
            TextView tvTab = new TextView(this);
            tvTab.setText(tabName);
            tvTab.setTextColor(Color.WHITE);
            tvTab.setPadding(20, 10, 20, 10);
            tvTab.setTextSize(12);
            tvTab.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tvTab.setLayoutParams(params);
            tabBar.addView(tvTab);

            tvTab.setOnClickListener(v -> {
                if (tabName.equals("ГЛАВНАЯ")) showHomeTab();
                else if (tabName.equals("БОТ")) showBotTab();
                else if (tabName.equals("СТАТИСТИКА")) showStatsTab();
                else if (tabName.equals("НАСТРОЙКИ")) showSettingsTab();
            });
        }

        showHomeTab(); // По умолчанию открываем главную
    }

    private void showHomeTab() {
        contentLayout.removeAllViews();

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Mecha Premium");
        tvTitle.setTextSize(24);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 20, 0, 20);
        contentLayout.addView(tvTitle);

        TextView tvBrawler = new TextView(this);
        tvBrawler.setText("БОЕЦ\nShelly\nНАЧАЛЬНЫЙ");
        tvBrawler.setTextSize(18);
        tvBrawler.setTextColor(Color.CYAN);
        tvBrawler.setGravity(Gravity.CENTER);
        tvBrawler.setPadding(0, 20, 0, 20);
        contentLayout.addView(tvBrawler);

        Button btnStart = new Button(this);
        btnStart.setText("▶ ЗАПУСТИТЬ");
        btnStart.setBackgroundColor(Color.parseColor("#8B0000"));
        btnStart.setTextColor(Color.WHITE);
        contentLayout.addView(btnStart);
        btnStart.setOnClickListener(v -> checkAndLaunchUI());

        TextView tvExpires = new TextView(this);
        long expires = prefs.getLong("expires", 0);
        if (expires > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            tvExpires.setText("ключ активен еще: " + sdf.format(new Date(expires)));
        } else {
            tvExpires.setText("ключ активен: бессрочно");
        }
        tvExpires.setTextColor(Color.LTGRAY);
        tvExpires.setPadding(0, 20, 0, 20);
        contentLayout.addView(tvExpires);
    }

    private void showBotTab() {
        contentLayout.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("Экран бота\n(Логи/статус)");
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        contentLayout.addView(tv);
    }

    private void showStatsTab() {
        contentLayout.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("Статистика\nМатчи: 1152\nПобеды: 545");
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        contentLayout.addView(tv);
    }

    private void showSettingsTab() {
        contentLayout.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("Настройки\n(Визуальный режим, ADB и т.д.)");
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        contentLayout.addView(tv);

        Button btnBuy = new Button(this);
        btnBuy.setText("Купить новый ключ");
        btnBuy.setBackgroundColor(Color.TRANSPARENT);
        btnBuy.setTextColor(Color.parseColor("#00d4ff"));
        contentLayout.addView(btnBuy);
        btnBuy.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/zynvpn_originalbot"));
            startActivity(intent);
        });

        Button btnReset = new Button(this);
        btnReset.setText("Сбросить ключ");
        btnReset.setTextColor(Color.RED);
        btnReset.setBackgroundColor(Color.TRANSPARENT);
        contentLayout.addView(btnReset);
        btnReset.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            showLoginScreen();
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
            }
        }
    }

    private String fetchMyDeviceId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
