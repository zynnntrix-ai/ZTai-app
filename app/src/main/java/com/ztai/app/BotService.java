package com.ztai.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class BotService extends Service {

    private Handler handler;
    private boolean isRunning = false;
    private Runnable botLoop;
    
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth, screenHeight, screenDpi;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDpi = metrics.densityDpi;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getStringExtra("action") != null) {
            String action = intent.getStringExtra("action");
            if (action.equals("start")) {
                startBot();
            } else if (action.equals("stop")) {
                stopBot();
            } else if (action.equals("set_projection")) {
                int resultCode = intent.getIntExtra("result_code", 0);
                Intent data = intent.getParcelableExtra("data");
                setupMediaProjection(resultCode, data);
            }
        }
        return START_STICKY;
    }

    private void setupMediaProjection(int resultCode, Intent data) {
        if (data == null) return;
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(resultCode, data);
        
        if (mediaProjection != null) {
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            virtualDisplay = mediaProjection.createVirtualDisplay("ZTaiScreen",
                    screenWidth, screenHeight, screenDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, handler);
            System.out.println("ZTai: MediaProjection готов, экран захвачен.");
        }
    }

    private void startBot() {
        if (isRunning) return;
        isRunning = true;

        botLoop = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                // 1. Делаем скриншот (чтобы бот "видел")
                captureScreen();

                // 2. ТЕСТ: Тап по центру экрана (в будущем — координаты кнопок Brawl Stars)
                TapService.tap(screenWidth / 2f, screenHeight / 2f);

                // Повторяем каждые 2 секунды
                handler.postDelayed(this, 2000);
            }
        };
        
        handler.post(botLoop);
    }

    private void captureScreen() {
        if (imageReader == null) return;
        Image image = imageReader.acquireLatestImage();
        if (image == null) return;

        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;

            Bitmap bitmap = Bitmap.createBitmap(screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            
            // ЗДЕСЬ В БУДУЩЕМ БУДЕТ АНАЛИЗ КАРТИНКИ (поиск врагов, HP и т.д.)
            System.out.println("ZTai: Скриншот сделан! Размер: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            image.close();
        }
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
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
