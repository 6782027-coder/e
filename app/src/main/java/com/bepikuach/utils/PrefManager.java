package com.bepikuach.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrefManager {

    private static final String PREF_NAME = "BePikuachPrefs";
    private static final String KEY_PASSWORD = "admin_password";
    private static final String KEY_APPROVED_APPS = "approved_apps";
    private static final String KEY_WALLPAPER_PATH = "wallpaper_path";
    private static final String KEY_ICON_SIZE = "icon_size";
    private static final String KEY_BLOCKED_LOG = "blocked_log";
    private static final String KEY_BLOCK_NETWORK = "block_network";
    private static final String KEY_WM_ENABLED = "wm_enabled";
    private static final String KEY_WM_ALPHA   = "wm_alpha";
    private static final String KEY_WM_SIZE_DP = "wm_size_dp";
    private static final String KEY_WM_GRAVITY = "wm_gravity";
    private static final String KEY_FLOAT_BTN_GRAVITY = "float_btn_gravity";
    private static final String KEY_FLOAT_BTN_ENABLED = "float_btn_enabled";

    // כל חסימה: 0=כבוי, 1=פעיל (ניתן לשינוי), 2=קבוע (לא ניתן לשינוי)
    private static final String KEY_BLOCK_STATUS_BAR = "block_status_bar";
    private static final String KEY_BLOCK_INSTALL = "block_install";
    private static final String KEY_BLOCK_WIFI = "block_wifi";
    private static final String KEY_BLOCK_HOTSPOT = "block_hotspot";
    private static final String KEY_BLOCK_USB = "block_usb";
    private static final String KEY_BLOCK_FACTORY_RESET = "block_factory_reset";
    private static final String KEY_BLOCK_UNINSTALL = "block_uninstall";
    private static final String KEY_BLOCK_DEV_OPTIONS = "block_dev_options";

    private static final int MAX_LOG = 30;
    private static final String DEFAULT_PASSWORD = "1234";

    private final SharedPreferences prefs;

    public PrefManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // סיסמה
    public String getPassword() { return prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD); }
    public void setPassword(String p) { prefs.edit().putString(KEY_PASSWORD, p).apply(); }
    public boolean checkPassword(String input) { return getPassword().equals(input); }

    // אפליקציות מאושרות
    public Set<String> getApprovedApps() {
        return new HashSet<>(prefs.getStringSet(KEY_APPROVED_APPS, new HashSet<>()));
    }
    public void setApprovedApps(Set<String> packages) {
        prefs.edit().putStringSet(KEY_APPROVED_APPS, packages).apply();
    }
    public boolean isAppApproved(String pkg) {
        if (pkg.equals("com.bepikuach")) return true;
        return getApprovedApps().contains(pkg);
    }

    // מראה
    public String getWallpaperPath() { return prefs.getString(KEY_WALLPAPER_PATH, null); }
    public void setWallpaperPath(String p) { prefs.edit().putString(KEY_WALLPAPER_PATH, p).apply(); }
    public int getIconSize() { return prefs.getInt(KEY_ICON_SIZE, 1); }
    public void setIconSize(int size) { prefs.edit().putInt(KEY_ICON_SIZE, size).apply(); }

    // --- חסימות עם 3 מצבים ---
    // 0 = כבוי, 1 = פעיל, 2 = קבוע

    public int getBlockStatusBar() { return prefs.getInt(KEY_BLOCK_STATUS_BAR, 0); }
    public void setBlockStatusBar(int v) { if (getBlockStatusBar() < 2) prefs.edit().putInt(KEY_BLOCK_STATUS_BAR, v).apply(); }
    public boolean isBlockStatusBar() { return getBlockStatusBar() > 0; }

    public int getBlockInstall() { return prefs.getInt(KEY_BLOCK_INSTALL, 0); }
    public void setBlockInstall(int v) { if (getBlockInstall() < 2) prefs.edit().putInt(KEY_BLOCK_INSTALL, v).apply(); }
    public boolean isBlockInstall() { return getBlockInstall() > 0; }

    public int getBlockWifi() { return prefs.getInt(KEY_BLOCK_WIFI, 0); }
    public void setBlockWifi(int v) { if (getBlockWifi() < 2) prefs.edit().putInt(KEY_BLOCK_WIFI, v).apply(); }
    public boolean isBlockWifi() { return getBlockWifi() > 0; }

    public int getBlockHotspot() { return prefs.getInt(KEY_BLOCK_HOTSPOT, 0); }
    public void setBlockHotspot(int v) { if (getBlockHotspot() < 2) prefs.edit().putInt(KEY_BLOCK_HOTSPOT, v).apply(); }
    public boolean isBlockHotspot() { return getBlockHotspot() > 0; }

    public int getBlockUsb() { return prefs.getInt(KEY_BLOCK_USB, 0); }
    public void setBlockUsb(int v) { if (getBlockUsb() < 2) prefs.edit().putInt(KEY_BLOCK_USB, v).apply(); }
    public boolean isBlockUsb() { return getBlockUsb() > 0; }

    public int getBlockFactoryReset() { return prefs.getInt(KEY_BLOCK_FACTORY_RESET, 0); }
    public void setBlockFactoryReset(int v) { if (getBlockFactoryReset() < 2) prefs.edit().putInt(KEY_BLOCK_FACTORY_RESET, v).apply(); }
    public boolean isBlockFactoryReset() { return getBlockFactoryReset() > 0; }

    public int getBlockUninstall() { return prefs.getInt(KEY_BLOCK_UNINSTALL, 0); }
    public void setBlockUninstall(int v) { if (getBlockUninstall() < 2) prefs.edit().putInt(KEY_BLOCK_UNINSTALL, v).apply(); }
    public boolean isBlockUninstall() { return getBlockUninstall() > 0; }

    public int getBlockDevOptions() { return prefs.getInt(KEY_BLOCK_DEV_OPTIONS, 0); }
    public void setBlockDevOptions(int v) { if (getBlockDevOptions() < 2) prefs.edit().putInt(KEY_BLOCK_DEV_OPTIONS, v).apply(); }
    public boolean isBlockDevOptions() { return getBlockDevOptions() > 0; }

    public boolean isBlockNetwork() { return prefs.getBoolean(KEY_BLOCK_NETWORK, false); }
    public void setBlockNetwork(boolean b) { prefs.edit().putBoolean(KEY_BLOCK_NETWORK, b).apply(); }


    private static final String KEY_LOCK_REMOVE_OWNER = "lock_remove_owner";

    // נעילת כפתור הסרת Device Owner: 0=פתוח, 2=נעול לצמיתות
    public int getLockRemoveOwner() { return prefs.getInt(KEY_LOCK_REMOVE_OWNER, 0); }
    public void setLockRemoveOwner(int v) { if (getLockRemoveOwner() < 2) prefs.edit().putInt(KEY_LOCK_REMOVE_OWNER, v).apply(); }

    // לוג חסימות
    public List<String> getBlockedLog() {
        Set<String> set = prefs.getStringSet(KEY_BLOCKED_LOG, new HashSet<>());
        return new ArrayList<>(set);
    }
    public void addToBlockedLog(String pkg) {
        Set<String> current = new HashSet<>(prefs.getStringSet(KEY_BLOCKED_LOG, new HashSet<>()));
        current.add(System.currentTimeMillis() + "|" + pkg);
        if (current.size() > MAX_LOG) {
            List<String> sorted = new ArrayList<>(current);
            java.util.Collections.sort(sorted);
            while (sorted.size() > MAX_LOG) sorted.remove(0);
            current = new HashSet<>(sorted);
        }
        prefs.edit().putStringSet(KEY_BLOCKED_LOG, current).apply();
    }
    public void clearBlockedLog() {
        prefs.edit().putStringSet(KEY_BLOCKED_LOG, new HashSet<>()).apply();
    }
    // ווטרמארק
    public boolean isWatermarkEnabled() { return prefs.getBoolean(KEY_WM_ENABLED, false); }
    public void setWatermarkEnabled(boolean v) { prefs.edit().putBoolean(KEY_WM_ENABLED, v).apply(); }
    public int getWatermarkAlpha() { return prefs.getInt(KEY_WM_ALPHA, 12); }
    public void setWatermarkAlpha(int v) { prefs.edit().putInt(KEY_WM_ALPHA, v).apply(); }
    public int getWatermarkSizeDp() { return prefs.getInt(KEY_WM_SIZE_DP, 120); }
    public void setWatermarkSizeDp(int v) { prefs.edit().putInt(KEY_WM_SIZE_DP, v).apply(); }
    public int getWatermarkGravity() { return prefs.getInt(KEY_WM_GRAVITY, 0); }
    public void setWatermarkGravity(int v) { prefs.edit().putInt(KEY_WM_GRAVITY, v).apply(); }

    // כפתור צף
    public int getFloatBtnGravity() { return prefs.getInt(KEY_FLOAT_BTN_GRAVITY, 0); }
    public void setFloatBtnGravity(int v) { prefs.edit().putInt(KEY_FLOAT_BTN_GRAVITY, v).apply(); }

    public boolean isFloatBtnEnabled() { return prefs.getBoolean(KEY_FLOAT_BTN_ENABLED, true); }
    public void setFloatBtnEnabled(boolean v) { prefs.edit().putBoolean(KEY_FLOAT_BTN_ENABLED, v).apply(); }

    // מצב רקע — חסימה ללא דף בית
    public boolean isBackgroundModeEnabled() { return prefs.getBoolean("background_mode", false); }
    public void setBackgroundModeEnabled(boolean v) { prefs.edit().putBoolean("background_mode", v).apply(); }


    // אפליקציות מוסתרות (נפרד מחסימה)
    public Set<String> getHiddenApps() {
        return new HashSet<>(prefs.getStringSet("hidden_apps", new HashSet<>()));
    }
    public void setHiddenApps(Set<String> packages) {
        prefs.edit().putStringSet("hidden_apps", packages).apply();
    }
    public boolean isAppHidden(String pkg) {
        return getHiddenApps().contains(pkg);
    }


    // דף בית אוטומטי
    public String getAutoHomePkg() { return prefs.getString("auto_home_pkg", null); }
    public void setAutoHomePkg(String pkg) { prefs.edit().putString("auto_home_pkg", pkg).apply(); }

    // הוסף דף בית לרשימה לבנה אוטומטית
    public void updateHomeApp(String newHomePkg) {
        String oldHome = getAutoHomePkg();
        Set<String> approved = getApprovedApps();
        // הסר את הישן אם לא הוסף ידנית
        if (oldHome != null && !oldHome.equals(newHomePkg)) {
            approved.remove(oldHome);
        }
        // הוסף את החדש
        if (newHomePkg != null && !newHomePkg.isEmpty()) {
            approved.add(newHomePkg);
            setAutoHomePkg(newHomePkg);
        }
        setApprovedApps(approved);
    }


    // צבע מסך חסימה
    public int getBlockedScreenColor() { return prefs.getInt("blocked_color", 0xFFC62828); }
    public void setBlockedScreenColor(int color) { prefs.edit().putInt("blocked_color", color).apply(); }

}
