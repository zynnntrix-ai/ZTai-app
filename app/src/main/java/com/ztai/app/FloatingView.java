package com.ztai.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingView {

    private final Context ctx;
    private final WindowManager wm;
    private final WindowManager.LayoutParams params;
    private final LinearLayout rootLayout;
    private LinearLayout menuContent;
    private TextView titleView;
    private boolean isExpanded = false;

    public FloatingView(Context context) {
        this.ctx = context;
        this.wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 100;

        rootLayout = new LinearLayout(ctx);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(0xEE0a0e1a); // полупрозрачный тёмный

        // Заголовок
        titleView = new TextView(ctx);
        titleView.setText(" ZTai ");
        titleView.setTextColor(0xFFa855f7);
        titleView.setTextSize(18);
        titleView.setPadding(20, 15, 20, 15);
        titleView.setOnClickListener(v -> toggleExpand());
        
        // Добавляем обработчик перемещения (drag)
        titleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false; // чтобы сработал клик
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        wm.updateViewLayout(rootLayout, params);
                        return true;
                }
                return false;
            }
        });

        rootLayout.addView(titleView);

        // Контент меню (скрыт по умолчанию)
        menuContent = new LinearLayout(ctx);
        menuContent.setOrientation(LinearLayout.VERTICAL);
        menuContent.setPadding(20, 0, 20, 20);
        menuContent.setVisibility(View.GONE);
        
        setupMenu(menuContent);
        
        rootLayout.addView(menuContent);
    }

    private void setupMenu(LinearLayout layout) {
        // 1. Выбор бойца
        TextView tvBrawler = new TextView(ctx);
        tvBrawler.setText("Боец:");
        tvBrawler.setTextColor(Color.WHITE);
        layout.addView(tvBrawler);

        Spinner spinner = new Spinner(ctx);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(ctx,
                android.R.drawable.ic_menu_search, android.R.layout.simple_spinner_item);
        // Хардкод бойцов для теста (потом заменим на нормальный список)
        String[] brawlers = {"Шелли", "Эль Примо", "Кольт", "Нита", "Джесси"};
        ArrayAdapter<String> brawlerAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, brawlers);
        spinner.setAdapter(brawlerAdapter);
        layout.addView(spinner);

        // 2. Пуш кубков
        TextView tvPush = new TextView(ctx);
        tvPush.setText("Пуш кубков:");
        tvPush.setTextColor(Color.WHITE);
        tvPush.setPadding(0, 20, 0, 0);
        layout.addView(tvPush);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);

        EditText etCurrent = new EditText(ctx);
        etCurrent.setHint("Сейчас");
        etCurrent.setTextSize(14);
        etCurrent.setWidth(150);

        EditText etTarget = new EditText(ctx);
        etTarget.setHint("Цель");
        etTarget.setTextSize(14);
        etTarget.setWidth(150);

        row.addView(etCurrent);
        row.addView(etTarget);
        layout.addView(row);

        // 3. Кнопки Старт/Стоп
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 20, 0, 0);

        Button btnStart = new Button(ctx);
        btnStart.setText("СТАРТ");
        btnStart.setBackgroundColor(0xFF00ff41);
        btnStart.setTextColor(Color.BLACK);
        btnStart.setOnClickListener(v -> {
            Toast.makeText(ctx, "Бот запущен", Toast.LENGTH_SHORT).show();
            // ЗДЕСЬ БУДЕТ ЛОГИКА ЗАПУСКА БОТА
            // int current = Integer.parseInt(etCurrent.getText().toString());
            // int target = Integer.parseInt(etTarget.getText().toString());
            // String brawler = spinner.getSelectedItem().toString();
            // BotEngine.start(brawler, current, target);
        });

        Button btnStop = new Button(ctx);
        btnStop.setText("СТОП");
        btnStop.setBackgroundColor(0xFFff4444);
        btnStop.setTextColor(Color.WHITE);
        btnStop.setOnClickListener(v -> {
            Toast.makeText(ctx, "Бот остановлен", Toast.LENGTH_SHORT).show();
            // ЗДЕСЬ БУДЕТ ЛОГИКА ОСТАНОВКИ
        });

        btnRow.addView(btnStart);
        btnRow.addView(btnStop);
        layout.addView(btnRow);

        // Статус
        TextView status = new TextView(ctx);
        status.setText("Статус: Ожидание");
        status.setTextColor(0xFF00d4ff);
        status.setPadding(0, 15, 0, 0);
        layout.addView(status);
    }

    private void toggleExpand() {
        isExpanded = !isExpanded;
        if (isExpanded) {
            menuContent.setVisibility(View.VISIBLE);
            titleView.setText(" ZTai (Свернуть) ");
            // Меняем фокус, чтобы можно было вводить текст
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        } else {
            menuContent.setVisibility(View.GONE);
            titleView.setText(" ZTai (Открыть) ");
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        try {
            wm.updateViewLayout(rootLayout, params);
        } catch (Exception ignored) {}
    }

    public void show() {
        try {
            if (rootLayout.getParent() == null) {
                wm.addView(rootLayout, params);
            }
        } catch (Exception ignored) {}
    }

    public void hide() {
        try {
            if (rootLayout.getParent() != null) {
                wm.removeView(rootLayout);
            }
        } catch (Exception ignored) {}
    }
}
