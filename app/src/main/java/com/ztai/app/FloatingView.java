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
    private boolean isBotRunning = false;
    private final LinearLayout menuLayout;
    private final TextView buttonView;

    public FloatingView(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);

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

        menuLayout = new LinearLayout(context);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setVisibility(View.GONE);
        
        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setShape(GradientDrawable.RECTANGLE);
        menuBg.setColor(Color.parseColor("#E60A0E1A"));
        menuBg.setCornerRadius(20);
        menuBg.setStroke(2, Color.parseColor("#00D4FF"));
        menuLayout.setBackground(menuBg);
        menuLayout.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                (int)(120 * context.getResources().getDisplayMetrics().density), 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        menuParams.topMargin = 15;
        menuLayout.setLayoutParams(menuParams);

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

        btnStart.setOnClickListener(v -> {
            isBotRunning = true;
            buttonView.setText("ON");
            
            GradientDrawable circleOn = new GradientDrawable();
            circleOn.setShape(GradientDrawable.OVAL);
            circleOn.setColor(Color.parseColor("#00FF41"));
            circleOn.setStroke(3, Color.WHITE);
            buttonView.setBackground(circleOn);
            
            toggleMenu();
        });

        btnStop.setOnClickListener(v -> {
            isBotRunning = false;
            buttonView.setText("Z");
            
            GradientDrawable circleOff = new GradientDrawable();
            circleOff.setShape(GradientDrawable.OVAL);
            circleOff.setColor(Color.parseColor("#A855F7"));
            circleOff.setStroke(3, Color.parseColor("#00D4FF"));
            buttonView.setBackground(circleOff);
            
            toggleMenu();
        });
    }

    private void toggleMenu() {
        if (isBotRunning) return;
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
