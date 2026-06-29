package com.bepikuach.activities;

import android.app.usage.UsageStats;
import android.graphics.drawable.Drawable;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bepikuach.R;
import com.bepikuach.utils.AppInfo;
import com.bepikuach.utils.PrefManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RecentAppsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);

        PrefManager prefManager = new PrefManager(this);
        PackageManager pm = getPackageManager();
        Set<String> approved = prefManager.getApprovedApps();

        List<AppInfo> apps = getRecentApproved(approved, pm);

        RecyclerView list = findViewById(R.id.recentList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new RecentAdapter(apps));

        findViewById(R.id.bgOverlay).setOnClickListener(v -> finish());
    }

    private List<AppInfo> getRecentApproved(Set<String> approved, PackageManager pm) {
        List<AppInfo> result = new ArrayList<>();
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            if (usm != null) {
                long now = System.currentTimeMillis();
                Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(
                        now - 1000L * 60 * 60 * 24 * 7, now);

                TreeMap<Long, String> sorted = new TreeMap<>(Collections.reverseOrder());
                for (Map.Entry<String, UsageStats> e : stats.entrySet()) {
                    String pkg = e.getKey();
                    if (!approved.contains(pkg) || pkg.equals("com.bepikuach")) continue;
                    long last = e.getValue().getLastTimeUsed();
                    if (last > 0) sorted.put(last, pkg);
                }

                for (String pkg : sorted.values()) {
                    try {
                        android.content.pm.ApplicationInfo info = pm.getApplicationInfo(
                            pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES
                                  | PackageManager.MATCH_DISABLED_COMPONENTS);
                        String name = pm.getApplicationLabel(info).toString();
                        android.graphics.drawable.Drawable icon;
                        try { icon = pm.getApplicationIcon(pkg); }
                        catch (Exception e2) { icon = pm.getApplicationIcon(info); }
                        result.add(new AppInfo(name, pkg, icon, true));
                    } catch (Exception ignored) {}
                    if (result.size() >= 8) break;
                }
            }
        } catch (Exception ignored) {}

        // fallback — הצג כל המאושרות, כולל אפליקציות מוסתרות
        if (result.isEmpty()) {
            for (String pkg : approved) {
                if (pkg.equals("com.bepikuach")) continue;
                try {
                    android.content.pm.ApplicationInfo info = pm.getApplicationInfo(
                        pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES
                              | PackageManager.MATCH_DISABLED_COMPONENTS);
                    String name = pm.getApplicationLabel(info).toString();
                    android.graphics.drawable.Drawable icon;
                    try { icon = pm.getApplicationIcon(pkg); }
                    catch (Exception e2) { icon = pm.getApplicationIcon(info); }
                    result.add(new AppInfo(name, pkg, icon, true));
                } catch (Exception ignored) {}
            }
            result.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        }
        return result;
    }

    class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.VH> {
        List<AppInfo> apps;
        RecentAdapter(List<AppInfo> a) { this.apps = a; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_app, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppInfo app = apps.get(pos);
            h.icon.setImageDrawable(app.icon);
            h.name.setText(app.name);
            h.itemView.setOnClickListener(v -> {
                Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                }
                finish();
            });
        }

        @Override public int getItemCount() { return apps.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView icon; TextView name;
            VH(View v) { super(v); icon = v.findViewById(R.id.appIcon); name = v.findViewById(R.id.appName); }
        }
    }

    @Override public void onBackPressed() { finish(); }
}
