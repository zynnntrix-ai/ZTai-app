package com.ztai.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.os.Build;
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

    private static final String CHANNEL_ID = "ZTaiChannel";

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
        
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "ZTai Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getStringExtra("action") != null) {
            String action = intent.getStringExtra("action");
            if (action.equals("start")) {
                startBot();
            } else if (action.equals("stop")) {
                stopBot();
                stopForeground(true);
                stopSelf();
            } else if (action.equals("set_projection")) {
                int resultCode = intent.getIntExtra("result_code", 0);
                Intent data = intent.getParcelableExtra("data");
                setupMediaProjection(resultCode, data);
                startForegroundWithNotification(); // Запускаем foreground ПОСЛЕ получения разрешения
            }
        }
        return START_STICKY;
    }

    private void startForegroundWithNotification() {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ZTai работает")
                .setContentText("Бот активен и готов к игре")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();
        startForeground(1, notification);
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
            System.out.println("ZTai: MediaProjection готов.");
        }
    }

    private void startBot() {
        if (isRunning) return;
        isRunning = true;

        botLoop = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                captureScreen();
                // TapService.tap(screenWidth / 2f, screenHeight / 2f); // Временно отключено для теста
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
            System.out.println("ZTai: Скриншот сделан!");
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
