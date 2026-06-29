package com.bepikuach.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bepikuach.R;
import com.bepikuach.utils.PrefManager;

public class BlockedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_blocked);

        // צבע רקע לפי הגדרת המשתמש
        PrefManager prefs = new PrefManager(this);
        int color = prefs.getBlockedScreenColor();
        findViewById(R.id.blockedRoot).setBackgroundColor(color);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finish();
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        }, 1500);
    }

    @Override public void onBackPressed() {}
}
