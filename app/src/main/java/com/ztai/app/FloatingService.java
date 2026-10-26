package com.ztai.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FloatingService extends Service {

    private FloatingView floatingView;

    @Override
    public void onCreate() {
        super.onCreate();
        floatingView = new FloatingView(this);
        floatingView.show();
    }

    @Override
    public void onDestroy() {
        if (floatingView != null) {
            floatingView.hide();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
