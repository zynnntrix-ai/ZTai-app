package com.ztai.app;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class BotService extends Service {

    private Handler handler;
    private boolean isRunning = false;
    private Runnable botLoop;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getStringExtra("action") != null) {
            String action = intent.getStringExtra("action");
            if (action.equals("start")) {
                startBot();
            } else if (action.equals("stop")) {
                stopBot();
            }
        }
        return START_STICKY;
    }

    private void startBot() {
        if (isRunning) return;
        isRunning = true;

        botLoop = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                // ТЕСТОВАЯ ЛОГИКА: Просто свайп вправо и тап по центру
                // В будущем здесь будет OCR и анализ экрана
                
                // 1. Свайп (движение)
                TapService.swipe(500, 1500, 800, 1500, 200); 
                
                // 2. Тап (атака) через 1 секунду
                handler.postDelayed(() -> {
                    TapService.tap(1000, 1500); // Пример: тап по кнопке атаки
                }, 1000);

                // Повторяем цикл каждые 2 секунды
                handler.postDelayed(this, 2000);
            }
        };
        
        handler.post(botLoop);
    }

    private void stopBot() {
        isRunning = false;
        if (handler != null && botLoop != null) {
            handler.removeCallbacks(botLoop);
        }
    }

    @Override
    public void onDestroy() {
        stopBot();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
