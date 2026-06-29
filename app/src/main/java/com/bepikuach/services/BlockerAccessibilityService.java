package com.bepikuach.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import com.bepikuach.activities.BlockedActivity;
import com.bepikuach.utils.PrefManager;

public class BlockerAccessibilityService extends AccessibilityService {

    private PrefManager prefManager;
    private String lastBlockedPackage = "";
    private long lastBlockedTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        prefManager = new PrefManager(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (pkg.isEmpty()) return;

        // חסימת שורת התראות
        if (prefManager.isBlockStatusBar()) {
            if (pkg.equals("com.android.systemui")) {
                String cls = event.getClassName() != null ? event.getClassName().toString() : "";
                if (cls.contains("StatusBar") || cls.contains("NotificationShade") ||
                    cls.contains("QuickSettings") || cls.contains("NotificationPanel") ||
                    cls.contains("QSPanel") || cls.contains("ExpandedView")) {
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    return;
                }
            }
        }

        // דלג על חבילות מערכת ועל בפיקוח עצמה
        if (pkg.equals(getPackageName())) return;
        if (isSystemPackage(pkg)) return;

        // בדוק אם מאושר
        if (!prefManager.isAppApproved(pkg)) {
            // מניעת ספאם — אותה אפליקציה לא תחסם שוב תוך 800ms
            long now = System.currentTimeMillis();
            if (pkg.equals(lastBlockedPackage) && (now - lastBlockedTime) < 800) return;
            lastBlockedPackage = pkg;
            lastBlockedTime = now;

            prefManager.addToBlockedLog(pkg);

            Intent blocked = new Intent(this, BlockedActivity.class);
            blocked.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(blocked);
        }
    }

    private boolean isSystemPackage(String pkg) {
        return pkg.startsWith("com.android.") ||
               pkg.startsWith("android.") ||
               pkg.startsWith("com.google.android.gms") ||
               pkg.startsWith("com.google.android.gsf") ||
               pkg.equals("com.google.android.inputmethod.latin") ||
               pkg.equals("com.samsung.android.honeyboard") ||
               pkg.equals("com.android.launcher") ||
               pkg.equals("com.android.systemui");
    }

    @Override
    public void onInterrupt() {}
}
