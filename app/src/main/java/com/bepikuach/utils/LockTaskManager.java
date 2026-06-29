package com.bepikuach.utils;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

import com.bepikuach.admin.DeviceAdminReceiver;

import java.util.Set;

public class LockTaskManager {

    private final Context context;
    private final DevicePolicyManager dpm;
    private final ComponentName admin;
    public final boolean isDeviceOwner;

    public LockTaskManager(Context context) {
        this.context = context;
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.admin = new ComponentName(context, DeviceAdminReceiver.class);
        this.isDeviceOwner = dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    public void enableFRP() {
        if (!isDeviceOwner || dpm == null) return;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.app.admin.FactoryResetProtectionPolicy policy =
                    new android.app.admin.FactoryResetProtectionPolicy.Builder()
                        .setFactoryResetProtectionEnabled(true)
                        .setFactoryResetProtectionAccounts(new java.util.ArrayList<>())
                        .build();
                dpm.setFactoryResetProtectionPolicy(admin, policy);
            }
        } catch (Exception ignored) {}
    }

    public void applyAll(PrefManager prefs) {
        if (!isDeviceOwner || dpm == null) return;
        enableFRP();
        updateLockTaskFeatures(prefs);
        updateStatusBar(prefs.isBlockStatusBar());
        // הערה: לא קוראים ל-setApplicationHidden בכלל — החסימה מתבצעת ע"י AccessibilityService
    }

    public void updateLockTaskFeatures(PrefManager prefs) {
        if (!isDeviceOwner || dpm == null) return;
        try {
            if (prefs.isBlockStatusBar()) {
                int features = DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS |
                               DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD |
                               DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW;
                dpm.setLockTaskFeatures(admin, features);
            } else {
                dpm.setLockTaskFeatures(admin,
                        DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS |
                        DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD |
                        DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW |
                        DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS |
                        DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO |
                        DevicePolicyManager.LOCK_TASK_FEATURE_HOME);
            }
        } catch (Exception ignored) {}
    }

    public void updateStatusBar(boolean blocked) {
        if (!isDeviceOwner || dpm == null) return;
        try {
            dpm.setStatusBarDisabled(admin, blocked);
        } catch (Exception ignored) {}
    }

    public void updateApprovedPackages(Set<String> approved) {
        if (!isDeviceOwner || dpm == null) return;
        try {
            Set<String> pkgs = new java.util.HashSet<>(approved);
            pkgs.add(context.getPackageName());
            dpm.setLockTaskPackages(admin, pkgs.toArray(new String[0]));
            // אין setApplicationHidden — אפליקציות תמיד גלויות
        } catch (Exception ignored) {}
    }

    // נשאר לצורך תאימות — לא עושה כלום בגישה החדשה
    public void showAllAppsTemporarily() {}
    public void restoreHiddenApps(Set<String> approved) { updateApprovedPackages(approved); }
}
