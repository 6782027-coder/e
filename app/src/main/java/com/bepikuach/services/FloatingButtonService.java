package com.bepikuach.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bepikuach.activities.HomeActivity;
import com.bepikuach.activities.RecentAppsActivity;
import com.bepikuach.utils.PrefManager;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private LinearLayout floatPanel;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // הסר קיים
        if (floatPanel != null) {
            try { windowManager.removeView(floatPanel); } catch (Exception ignored) {}
            floatPanel = null;
        }

        PrefManager prefs = new PrefManager(this);

        // אם כבוי — לא מציג
        if (!prefs.isFloatBtnEnabled()) return START_STICKY;

        int btnSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        int marginPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

        // פאנל אופקי
        floatPanel = new LinearLayout(this);
        floatPanel.setOrientation(LinearLayout.HORIZONTAL);

        // כפתור מחליף אפליקציות
        TextView btnRecent = new TextView(this);
        btnRecent.setText("⧉");
        btnRecent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnRecent.setTextColor(Color.WHITE);
        btnRecent.setBackgroundColor(0xDD1A237E);
        btnRecent.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(btnSizePx, btnSizePx);
        lp1.rightMargin = marginPx;
        btnRecent.setLayoutParams(lp1);
        btnRecent.setOnClickListener(v -> {
            Intent i = new Intent(this, RecentAppsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        // כפתור מסך הבית
        TextView btnHome = new TextView(this);
        btnHome.setText("⌂");
        btnHome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnHome.setTextColor(Color.WHITE);
        btnHome.setBackgroundColor(0xDD1A237E);
        btnHome.setGravity(Gravity.CENTER);
        btnHome.setLayoutParams(new LinearLayout.LayoutParams(btnSizePx, btnSizePx));
        btnHome.setOnClickListener(v -> {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        });

        floatPanel.addView(btnRecent);
        floatPanel.addView(btnHome);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = gravityFromPref(prefs.getFloatBtnGravity());
        params.x = marginPx;
        params.y = marginPx;

        try { windowManager.addView(floatPanel, params); }
        catch (Exception ignored) {}

        return START_STICKY;
    }

    private int gravityFromPref(int g) {
        switch (g) {
            case 1: return Gravity.TOP    | Gravity.START;
            case 2: return Gravity.TOP    | Gravity.END;
            case 3: return Gravity.BOTTOM | Gravity.START;
            case 4: return Gravity.BOTTOM | Gravity.END;
            default: return Gravity.TOP   | Gravity.END;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatPanel != null) {
            try { windowManager.removeView(floatPanel); } catch (Exception ignored) {}
            floatPanel = null;
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
