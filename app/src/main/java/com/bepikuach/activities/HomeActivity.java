package com.bepikuach.activities;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bepikuach.R;
import com.bepikuach.services.AppMonitorService;
import com.bepikuach.utils.AppInfo;
import com.bepikuach.utils.HomeAppsAdapter;
import com.bepikuach.utils.LockTaskManager;
import com.bepikuach.utils.PrefManager;
import com.bepikuach.utils.RestrictionsManager;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private PrefManager prefs;
    private LockTaskManager ltm;
    private HomeAppsAdapter adapter;
    private Handler clockHandler;
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setContentView(R.layout.activity_home);
        prefs = new PrefManager(this);
        ltm = new LockTaskManager(this);

        // זהה דף בית והוסף לרשימה הלבנה אוטומטית
        autoRegisterHomeApp();

        // החל הגדרות לפני startLockTask
        ltm.applyAll(prefs);
        new RestrictionsManager(this).applyAll(prefs);

        // הפעל LockTask — אחרי setLockTaskPackages, בלי dialog
        startLockTaskIfNeeded();

        // שירות ניטור
        startForegroundService(new Intent(this, AppMonitorService.class));

        setupStatusBar();
        setupWallpaper();
        setupClock();
        setupAppsGrid();
        setupButtons();
    }

    private void autoRegisterHomeApp() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo ri = getPackageManager()
                    .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null) {
                String homePkg = ri.activityInfo.packageName;
                if (!homePkg.equals(getPackageName())) {
                    prefs.updateHomeApp(homePkg);
                }
            }
        } catch (Exception ignored) {}
    }

    private void startLockTaskIfNeeded() {
        if (!ltm.isDeviceOwner) return;
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null && !am.isInLockTaskMode()) {
                startLockTask();
            }
        } catch (Exception ignored) {}
    }

    private void setupStatusBar() {
        if (prefs.isBlockStatusBar()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void setupWallpaper() {
        ImageView wallpaper = findViewById(R.id.wallpaperImage);
        String path = prefs.getWallpaperPath();
        if (path != null) {
            try {
                // נסה כנתיב קובץ
                if (path.startsWith("content://")) {
                    Uri uri = Uri.parse(path);
                    InputStream is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        wallpaper.setImageBitmap(BitmapFactory.decodeStream(is));
                        is.close();
                        return;
                    }
                } else {
                    Bitmap bmp = BitmapFactory.decodeFile(path);
                    if (bmp != null) {
                        wallpaper.setImageBitmap(bmp);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }
        wallpaper.setBackgroundColor(0xFF3F51B5);
    }

    private void setupClock() {
        TextView clockText = findViewById(R.id.clockText);
        TextView dateText = findViewById(R.id.dateText);
        clockHandler = new Handler(Looper.getMainLooper());
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                clockText.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
                dateText.setText(new SimpleDateFormat("EEEE, d בMMMM", new Locale("iw")).format(now));
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void setupAppsGrid() {
        RecyclerView grid = findViewById(R.id.appsGrid);
        int iconSize = prefs.getIconSize();
        int columns = iconSize == 0 ? 5 : iconSize == 1 ? 4 : 3;
        grid.setLayoutManager(new GridLayoutManager(this, columns));
        grid.setHasFixedSize(false);
        adapter = new HomeAppsAdapter(getApprovedApps(), app -> {
            Intent i = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        });
        adapter.setIconSize(iconSize);
        grid.setAdapter(adapter);
    }

    private List<AppInfo> getApprovedApps() {
        List<AppInfo> result = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (String pkg : prefs.getApprovedApps()) {
            try {
                String name = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                result.add(new AppInfo(name, pkg, pm.getApplicationIcon(pkg), true));
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return result;
    }

    private void setupButtons() {
        findViewById(R.id.settingsBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, PasswordActivity.class);
            intent.putExtra("target", "admin");
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoRegisterHomeApp();
        ltm.applyAll(prefs);
        new RestrictionsManager(this).applyAll(prefs);
        setupAppsGrid();
        setupStatusBar();
    }

    @Override public void onBackPressed() {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null) clockHandler.removeCallbacks(clockRunnable);
    }
}
