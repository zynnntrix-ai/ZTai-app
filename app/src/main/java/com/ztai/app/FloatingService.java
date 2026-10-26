package com.ztai.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FloatingService extends Service {

    private FloatingView view;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (view == null) {
            view = new FloatingView(this);
            view.show();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (view != null) {
            view.hide();
            view = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
