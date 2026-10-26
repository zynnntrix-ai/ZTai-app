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
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingView {

    private final Context context;
    private final WindowManager windowManager;
    private final LinearLayout rootLayout;
    private final WindowManager.LayoutParams params;
    private boolean isMenuOpen = false;
    private final LinearLayout menuLayout;
    private final TextView buttonView;

    public FloatingView(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // Главный контейнер (кнопка)
        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);

        // Круглая кнопка
        buttonView = new TextView(context);
        buttonView.setText("Z");
        buttonView.setTextSize(24);
        buttonView.setTextColor(Color.WHITE);
        buttonView.setGravity(Gravity.CENTER);
        
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#A855F7"));
        circle.setStroke(3, Color.parseColor("#00D4FF"));
        buttonView.setBackground(circle);

        int size = (int) (60 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(size, size);
        buttonView.setLayoutParams(btnParams);
        rootLayout.addView(buttonView);

        // Выпадающее меню (скрыто по умолчанию)
        menuLayout = new LinearLayout(context);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setVisibility(View.GONE);
        menuLayout.setBackgroundColor(Color.parseColor("#CC0A0E1A"));
        menuLayout.setPadding(20, 20, 20, 20);

        Button btnStart = new Button(context);
        btnStart.setText("▶ СТАРТ");
        btnStart.setTextColor(Color.WHITE);
        btnStart.setBackgroundColor(Color.parseColor("#00FF41"));
        menuLayout.addView(btnStart);

        Button btnStop = new Button(context);
        btnStop.setText("⏹ СТОП");
        btnStop.setTextColor(Color.WHITE);
        btnStop.setBackgroundColor(Color.parseColor("#FF0000"));
        menuLayout.addView(btnStop);

        rootLayout.addView(menuLayout);

        // Настройки окна
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        // Логика перетаскивания и клика
        buttonView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Если не двигали — открываем/закрываем меню
                        if (Math.abs(event.getRawX() - initialTouchX) < 10 && Math.abs(event.getRawY() - initialTouchY) < 10) {
                            toggleMenu();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(rootLayout, params);
                        return true;
                }
                return false;
            }
        });

        // Кнопки Старт и Стоп
        btnStart.setOnClickListener(v -> {
            // Логика запуска бота (пока просто текст)
            buttonView.setText("ON");
            buttonView.setBackgroundColor(Color.parseColor("#00FF41"));
            toggleMenu();
        });

        btnStop.setOnClickListener(v -> {
            // Логика остановки бота
            buttonView.setText("Z");
            GradientDrawable circle2 = new GradientDrawable();
            circle2.setShape(GradientDrawable.OVAL);
            circle2.setColor(Color.parseColor("#A855F7"));
            circle2.setStroke(3, Color.parseColor("#00D4FF"));
            buttonView.setBackground(circle2);
            toggleMenu();
        });
    }

    private void toggleMenu() {
        if (isMenuOpen) {
            menuLayout.setVisibility(View.GONE);
            isMenuOpen = false;
        } else {
            menuLayout.setVisibility(View.VISIBLE);
            isMenuOpen = true;
        }
    }

    public void show() {
        if (rootLayout.getParent() == null) {
            windowManager.addView(rootLayout, params);
        }
    }

    public void hide() {
        if (rootLayout.getParent() != null) {
            windowManager.removeView(rootLayout);
        }
    }
}
