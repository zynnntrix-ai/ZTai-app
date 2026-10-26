package com.ztai.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
    private final OkHttpClient client = new OkHttpClient();
    private SharedPreferences prefs;
    private LinearLayout contentLayout;
    private LinearLayout tabBar;
    private TextView[] tabViews = new TextView[4];
    private String[] tabNames = {"ГЛАВНАЯ", "ИНФО", "СТАТИСТИКА", "НАСТРОЙКИ"};
    
    private MediaProjectionManager projectionManager;
    private static final int REQUEST_MEDIA_PROJECTION = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0A0E1A"));

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        contentLayout.setPadding(30, 30, 30, 30);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        mainLayout.addView(contentLayout);

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Color.parseColor("#120A1A"));
        tabBar.setPadding(0, 15, 0, 15);
        tabBar.setVisibility(View.GONE);

        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            TextView tvTab = new TextView(this);
            tvTab.setText(tabNames[i]);
            tvTab.setTextSize(11);
            tvTab.setGravity(Gravity.CENTER);
            tvTab.setPadding(0, 10, 0, 10);
            tvTab.setTextColor(Color.parseColor("#888888"));
            tvTab.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tvTab.setLayoutParams(params);
            tabBar.addView(tvTab);
            tabViews[i] = tvTab;
            tvTab.setOnClickListener(v -> selectTab(index));
        }
        mainLayout.addView(tabBar);

        setContentView(mainLayout);

        if (prefs.getString("key", null) != null) {
            showMainTabs();
        } else {
            showLoginScreen();
        }
    }

    private void selectTab(int index) {
        for (int i = 0; i < tabViews.length; i++) {
            tabViews[i].setTextColor(i == index ? Color.parseColor("#00D4FF") : Color.parseColor("#888888"));
        }
        if (index == 0) showHomeTab();
        else if (index == 1) showInfoTab();
        else if (index == 2) showStatsTab();
        else showSettingsTab();
    }

    private void showLoginScreen() {
        contentLayout.removeAllViews();
        tabBar.setVisibility(View.GONE);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("ZTAI");
        tvTitle.setTextSize(48);
        tvTitle.setTextColor(Color.parseColor("#A855F7"));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 100, 0, 50);
        tvTitle.setTypeface(null, Typeface.BOLD);
        contentLayout.addView(tvTitle);

        EditText etKey = new EditText(this);
        etKey.setHint("Введите ключ активации");
        etKey.setTextColor(Color.WHITE);
        etKey.setHintTextColor(Color.GRAY);
        etKey.setBackgroundColor(Color.parseColor("#1A1A2E"));
        etKey.setPadding(30, 30, 30, 30);
        contentLayout.addView(etKey);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Ожидание...");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setPadding(0, 20, 0, 20);
        contentLayout.addView(tvStatus);

        Button btnActivate = new Button(this);
        btnActivate.setText("АКТИВИРОВАТЬ");
        btnActivate.setTextColor(Color.WHITE);
        btnActivate.setBackgroundResource(R.drawable.bg_btn_primary);
        contentLayout.addView(btnActivate);

        btnActivate.setOnClickListener(v -> {
            String key = etKey.getText().toString().trim();
            if (key.isEmpty()) { tvStatus.setText("Введите ключ"); return; }
            tvStatus.setText("Проверка...");
            tvStatus.setTextColor(Color.YELLOW);
            
            JSONObject json = new JSONObject();
            try {
                json.put("key", key);
                json.put("device_id", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
            } catch (Exception e) { e.printStackTrace(); }

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder().url(SERVER_URL + "/api/check_key").post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> { tvStatus.setTextColor(Color.RED); tvStatus.setText("Ошибка сети"); });
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    String resp = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject j = new JSONObject(resp);
                            if (j.optBoolean("ok", false)) {
                                prefs.edit().putString("key", key).putLong("expires", j.optLong("expires", 0)).apply();
                                showMainTabs();
                            } else {
                                tvStatus.setTextColor(Color.RED);
                                tvStatus.setText("❌ " + j.optString("error", "unknown"));
                            }
                        } catch (Exception e) { tvStatus.setText("Ошибка ответа"); }
                    });
                }
            });
        });
    }

    private void showMainTabs() {
        tabBar.setVisibility(View.VISIBLE);
        selectTab(0);
    }

    private void showHomeTab() {
        contentLayout.removeAllViews();

        TextView tvLogo = new TextView(this);
        tvLogo.setText("ZTai");
        tvLogo.setTextSize(32);
        tvLogo.setTextColor(Color.parseColor("#A855F7"));
        tvLogo.setGravity(Gravity.CENTER);
        tvLogo.setTypeface(null, Typeface.BOLD);
        contentLayout.addView(tvLogo);

        TextView tvSub = new TextView(this);
        tvSub.setText("AI-бот для Brawl Stars");
        tvSub.setTextColor(Color.parseColor("#00D4FF"));
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, 0, 0, 30);
        contentLayout.addView(tvSub);

        LinearLayout brawlerCard = new LinearLayout(this);
        brawlerCard.setOrientation(LinearLayout.VERTICAL);
        brawlerCard.setGravity(Gravity.CENTER);
        brawlerCard.setBackgroundResource(R.drawable.bg_card_glass);
        brawlerCard.setPadding(40, 40, 40, 40);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 30);
        brawlerCard.setLayoutParams(cardParams);

        TextView avatar = new TextView(this);
        avatar.setText("S");
        avatar.setTextSize(40);
        avatar.setTextColor(Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_avatar_circle);
        int size = (int) getResources().getDisplayMetrics().density * 80;
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(size, size);
        avatar.setLayoutParams(avatarParams);
        brawlerCard.addView(avatar);

        TextView brawlerName = new TextView(this);
        brawlerName.setText("Shelly");
        brawlerName.setTextSize(20);
        brawlerName.setTextColor(Color.WHITE);
        brawlerName.setTypeface(null, Typeface.BOLD);
        brawlerName.setPadding(0, 15, 0, 0);
        brawlerCard.addView(brawlerName);

        TextView brawlerRarity = new TextView(this);
        brawlerRarity.setText("НАЧАЛЬНЫЙ");
        brawlerRarity.setTextColor(Color.CYAN);
        brawlerCard.addView(brawlerRarity);

        contentLayout.addView(brawlerCard);

        Button btnStart = new Button(this);
        btnStart.setText("▶ ЗАПУСТИТЬ");
        btnStart.setTextColor(Color.WHITE);
        btnStart.setBackgroundResource(R.drawable.bg_btn_primary);
        btnStart.setPadding(0, 20, 0, 20);
        contentLayout.addView(btnStart);
        btnStart.setOnClickListener(v -> tryStartBot());

        TextView tvExpires = new TextView(this);
        long expires = prefs.getLong("expires", 0);
        if (expires > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            tvExpires.setText("Ключ активен до: " + sdf.format(new Date(expires)));
        } else {
            tvExpires.setText("Ключ активен: бессрочно");
        }
        tvExpires.setTextColor(Color.LTGRAY);
        tvExpires.setGravity(Gravity.CENTER);
        tvExpires.setPadding(0, 30, 0, 0);
        contentLayout.addView(tvExpires);
    }

    private void tryStartBot() {
        boolean hasOverlay = Settings.canDrawOverlays(this);
        boolean hasAccessibility = isAccessibilityServiceEnabled(this, TapService.class);

        if (!hasOverlay || !hasAccessibility) {
            Toast.makeText(this, "Разрешите все в настройках для запуска!", Toast.LENGTH_LONG).show();
            selectTab(3); 
            return;
        }

        launchBotUI();
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {
        String enabledServicesSetting = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) return false;
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            if (componentNameString.equalsIgnoreCase(context.getPackageName() + "/" + serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    private void showInfoTab() {
        contentLayout.removeAllViews();
        TextView title = new TextView(this);
        title.setText("ИНФОРМАЦИЯ");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 20);
        contentLayout.addView(title);
        TextView tvInfo = new TextView(this);
        tvInfo.setText("ZTai — умный бот для Brawl Stars.\n\n✅ Умная логика\n✅ Работает в фоне\n✅ Простой интерфейс\n\nКак играть:\n1. Включите разрешения.\n2. Нажмите ЗАПУСТИТЬ.\n3. Разрешите захват экрана.\n4. Откройте игру.");
        tvInfo.setTextColor(Color.LTGRAY);
        tvInfo.setTextSize(14);
        contentLayout.addView(tvInfo);
    }

    private void showStatsTab() {
        contentLayout.removeAllViews();
        TextView title = new TextView(this);
        title.setText("СТАТИСТИКА");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 20);
        contentLayout.addView(title);
        addStatCard("Время сессии", "00:00:00");
        addStatCard("Матчей сыграно", "0");
        addStatCard("Побед", "0");
        addStatCard("Винрейт", "0%");
    }

    private void addStatCard(String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.bg_card_glass);
        card.setPadding(30, 20, 30, 20);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 15);
        card.setLayoutParams(params);
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.LTGRAY);
        tvLabel.setTextSize(16);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(tvLabel);
        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(Color.parseColor("#00D4FF"));
        tvValue.setTextSize(18);
        tvValue.setTypeface(null, Typeface.BOLD);
        card.addView(tvValue);
        contentLayout.addView(card);
    }

    private void showSettingsTab() {
        contentLayout.removeAllViews();
        TextView title = new TextView(this);
        title.setText("НАСТРОЙКИ");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 20);
        contentLayout.addView(title);

        boolean hasOverlay = Settings.canDrawOverlays(this);
        boolean hasAccessibility = isAccessibilityServiceEnabled(this, TapService.class);

        addClickableSettingItem("Показ поверх окон", hasOverlay ? "Включено ✅" : "Выключено ❌", 
                hasOverlay ? Color.GREEN : Color.RED, 
                () -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));

        addClickableSettingItem("Служба доступности", hasAccessibility ? "Включено ✅" : "Выключено ❌", 
                hasAccessibility ? Color.GREEN : Color.RED, 
                () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        addClickableSettingItem("Оптимизация батареи", "Отключить для стабильности", Color.LTGRAY, 
                () -> startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));

        Button btnReset = new Button(this);
        btnReset.setText("Сбросить ключ");
        btnReset.setTextColor(Color.RED);
        btnReset.setBackgroundColor(Color.TRANSPARENT);
        btnReset.setPadding(0, 30, 0, 0);
        contentLayout.addView(btnReset);
        btnReset.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            showLoginScreen();
        });
    }

    private void addClickableSettingItem(String title, String subtitle, int subtitleColor, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(30, 20, 30, 20);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_glass);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 15);
        row.setLayoutParams(params);
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16);
        textLayout.addView(tvTitle);
        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(subtitleColor);
        tvSub.setTextSize(12);
        textLayout.addView(tvSub);
        row.addView(textLayout);
        row.setOnClickListener(v -> onClick.run());
        contentLayout.addView(row);
    }

    private void launchBotUI() {
        startService(new Intent(this, FloatingService.class));
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (Settings.canDrawOverlays(this)) {
                launchBotUI();
            }
        } else if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(this, BotService.class);
                serviceIntent.putExtra("action", "set_projection");
                serviceIntent.putExtra("result_code", resultCode);
                serviceIntent.putExtra("data", data);
                startService(serviceIntent);
            } else {
                Toast.makeText(this, "Без разрешения на экран бот не сможет работать", Toast.LENGTH_LONG).show();
            }
        }
    }
}
