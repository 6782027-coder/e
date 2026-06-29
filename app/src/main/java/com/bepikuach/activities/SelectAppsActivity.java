package com.bepikuach.activities;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.bepikuach.R;
import com.bepikuach.admin.DeviceAdminReceiver;
import com.bepikuach.utils.AppInfo;
import com.bepikuach.utils.LockTaskManager;
import com.bepikuach.utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SelectAppsActivity extends AppCompatActivity {

    private PrefManager prefManager;
    private LockTaskManager lockTaskManager;
    private DevicePolicyManager dpm;
    private ComponentName admin;

    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filteredApps = new ArrayList<>();
    private String searchQuery = "";

    private TextView tabApps, tabBlocked, tabWhitelist;
    private View panelApps, panelBlocked, panelWhitelist;
    private GridLayout appsGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_apps);

        prefManager = new PrefManager(this);
        lockTaskManager = new LockTaskManager(this);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, DeviceAdminReceiver.class);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ניהול אפליקציות");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        allApps = getAllInstalledApps();
        filteredApps = new ArrayList<>(allApps);

        setupTabs();
        setupSearch();
        buildAppsGrid(filteredApps);
        setupBlockedLog();
        setupWhitelistPanel();
    }

    private void setupTabs() {
        tabApps = findViewById(R.id.tabApps);
        tabBlocked = findViewById(R.id.tabBlocked);
        tabWhitelist = findViewById(R.id.tabWhitelist);
        panelApps = findViewById(R.id.panelApps);
        panelBlocked = findViewById(R.id.panelBlocked);
        panelWhitelist = findViewById(R.id.panelWhitelist);

        tabApps.setOnClickListener(v -> switchTab(0));
        tabBlocked.setOnClickListener(v -> switchTab(1));
        tabWhitelist.setOnClickListener(v -> switchTab(2));
        switchTab(0);
    }

    private void switchTab(int tab) {
        panelApps.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        panelBlocked.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        panelWhitelist.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        // הטאב הפעיל — רקע כהה, טקסט לבן
        // הטאבים הלא פעילים — רקע בהיר, טקסט כחול
        int activeBg = 0xFF3F51B5, inactiveBg = 0xFFE8EAF6;
        int activeText = 0xFFFFFFFF, inactiveText = 0xFF3F51B5;

        tabApps.setBackgroundColor(tab == 0 ? activeBg : inactiveBg);
        tabApps.setTextColor(tab == 0 ? activeText : inactiveText);
        tabBlocked.setBackgroundColor(tab == 1 ? activeBg : inactiveBg);
        tabBlocked.setTextColor(tab == 1 ? activeText : inactiveText);
        tabWhitelist.setBackgroundColor(tab == 2 ? activeBg : inactiveBg);
        tabWhitelist.setTextColor(tab == 2 ? activeText : inactiveText);

        if (tab == 1) refreshBlockedLog();
        if (tab == 2) refreshWhitelistPanel();
    }

    private void setupSearch() {
        SearchView search = findViewById(R.id.searchApps);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) {
                searchQuery = q;
                filterAndRebuild();
                return true;
            }
        });
    }

    private void filterAndRebuild() {
        filteredApps = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (searchQuery.isEmpty() ||
                app.name.toLowerCase().contains(searchQuery.toLowerCase()) ||
                app.packageName.toLowerCase().contains(searchQuery.toLowerCase())) {
                filteredApps.add(app);
            }
        }
        buildAppsGrid(filteredApps);
    }

    private void buildAppsGrid(List<AppInfo> apps) {
        appsGrid = findViewById(R.id.appsGrid);
        appsGrid.removeAllViews();

        int columns = 3;
        appsGrid.setColumnCount(columns);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cellWidth = screenWidth / columns;
        int cellHeight = (int) (cellWidth * 1.4f);

        for (AppInfo app : apps) {
            boolean isHidden = prefManager.isAppHidden(app.packageName);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            cell.setPadding(6, 8, 6, 8);
            cell.setBackgroundColor(app.isApproved ? 0x1A3F51B5 : 0x00000000);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellWidth;
            params.height = cellHeight;
            cell.setLayoutParams(params);

            // אייקון
            ImageView icon = new ImageView(this);
            int iconSize = (int) (cellWidth * 0.45f);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(iconParams);
            icon.setImageDrawable(app.icon);
            if (isHidden) icon.setAlpha(0.4f);

            // שם
            TextView name = new TextView(this);
            name.setText(app.name);
            name.setTextSize(9);
            name.setTextColor(isHidden ? 0xFF9E9E9E : 0xFF1A1A2E);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = 4;
            name.setLayoutParams(nameParams);

            // שורת צ'קבוקסים
            LinearLayout checks = new LinearLayout(this);
            checks.setOrientation(LinearLayout.HORIZONTAL);
            checks.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams checksParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            checksParams.topMargin = 4;
            checks.setLayoutParams(checksParams);

            CheckBox cbApproved = new CheckBox(this);
            cbApproved.setText("מותר");
            cbApproved.setTextSize(9);
            cbApproved.setChecked(app.isApproved);

            CheckBox cbHidden = new CheckBox(this);
            cbHidden.setText("מוסתר");
            cbHidden.setTextSize(9);
            cbHidden.setChecked(isHidden);

            checks.addView(cbApproved);
            checks.addView(cbHidden);

            cell.addView(icon);
            cell.addView(name);
            cell.addView(checks);

            // מאזינים
            cbApproved.setOnCheckedChangeListener((btn, checked) -> {
                app.isApproved = checked;
                for (AppInfo a : allApps) {
                    if (a.packageName.equals(app.packageName)) { a.isApproved = checked; break; }
                }
                cell.setBackgroundColor(checked ? 0x1A3F51B5 : 0x00000000);
                saveApproved();
            });

            cbHidden.setOnCheckedChangeListener((btn, checked) -> {
                Set<String> hidden = prefManager.getHiddenApps();
                if (checked) {
                    hidden.add(app.packageName);
                    try { dpm.setApplicationHidden(admin, app.packageName, true); } catch (Exception ignored) {}
                    icon.setAlpha(0.4f);
                    name.setTextColor(0xFF9E9E9E);
                } else {
                    hidden.remove(app.packageName);
                    try { dpm.setApplicationHidden(admin, app.packageName, false); } catch (Exception ignored) {}
                    icon.setAlpha(1.0f);
                    name.setTextColor(0xFF1A1A2E);
                }
                prefManager.setHiddenApps(hidden);
            });

            appsGrid.addView(cell);
        }

        Button saveBtn = findViewById(R.id.btnSave);
        saveBtn.setOnClickListener(v -> {
            saveApproved();
            Toast.makeText(this, "נשמר ✓", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveApproved() {
        Set<String> approved = new HashSet<>();
        for (AppInfo app : allApps) {
            if (app.isApproved) approved.add(app.packageName);
        }
        prefManager.setApprovedApps(approved);
        lockTaskManager.updateApprovedPackages(approved);
    }

    // ---- חסימות אחרונות ----
    private void setupBlockedLog() {
        Button clearBtn = findViewById(R.id.btnClearLog);
        clearBtn.setOnClickListener(v -> {
            prefManager.clearBlockedLog();
            refreshBlockedLog();
            Toast.makeText(this, "הלוג נוקה ✓", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshBlockedLog() {
        LinearLayout logContainer = findViewById(R.id.blockedLogContainer);
        TextView emptyMsg = findViewById(R.id.blockedLogEmpty);
        logContainer.removeAllViews();

        List<String> log = prefManager.getBlockedLog();
        if (log.isEmpty()) { emptyMsg.setVisibility(View.VISIBLE); return; }
        emptyMsg.setVisibility(View.GONE);
        Collections.sort(log, Collections.reverseOrder());

        PackageManager pm = getPackageManager();
        Set<String> approved = prefManager.getApprovedApps();

        for (String entry : log) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length < 2) continue;
            String pkg = parts[1];
            long timestamp = 0;
            try { timestamp = Long.parseLong(parts[0]); } catch (Exception ignored) {}

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(0xFFFFFFFF);

            try {
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(pm.getApplicationIcon(pkg));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(72, 72);
                lp.setMarginEnd(12);
                icon.setLayoutParams(lp);
                row.addView(icon);
            } catch (Exception ignored) {}

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            String appName = pkg;
            try { appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); } catch (Exception ignored) {}

            TextView nameView = new TextView(this);
            nameView.setText(appName);
            nameView.setTextColor(0xFF1A1A2E);
            nameView.setTextSize(14);

            TextView timeView = new TextView(this);
            if (timestamp > 0) timeView.setText(new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(timestamp)));
            timeView.setTextColor(0xFF9E9E9E);
            timeView.setTextSize(11);

            info.addView(nameView);
            info.addView(timeView);

            boolean alreadyApproved = approved.contains(pkg);
            Button allowBtn = new Button(this);
            allowBtn.setText(alreadyApproved ? "מותר ✓" : "התר");
            allowBtn.setTextSize(12);
            allowBtn.setEnabled(!alreadyApproved);
            allowBtn.setBackgroundColor(alreadyApproved ? 0xFF9E9E9E : 0xFF43A047);
            allowBtn.setTextColor(0xFFFFFFFF);

            final String fp = pkg, fn = appName;
            allowBtn.setOnClickListener(v -> {
                Set<String> newApproved = prefManager.getApprovedApps();
                newApproved.add(fp);
                prefManager.setApprovedApps(newApproved);
                lockTaskManager.updateApprovedPackages(newApproved);
                for (AppInfo a : allApps) { if (a.packageName.equals(fp)) { a.isApproved = true; break; } }
                allowBtn.setText("מותר ✓"); allowBtn.setEnabled(false); allowBtn.setBackgroundColor(0xFF9E9E9E);
                Toast.makeText(this, fn + " הותר ✓", Toast.LENGTH_SHORT).show();
            });

            row.addView(info); row.addView(allowBtn);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFC5CAE9);

            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(row); wrapper.addView(divider);
            logContainer.addView(wrapper);
        }
    }

    // ---- רשימה לבנה ----
    private void setupWhitelistPanel() {
        Button addBtn = findViewById(R.id.btnAddManual);
        EditText inputPkg = findViewById(R.id.inputManualPkg);
        addBtn.setOnClickListener(v -> {
            String pkg = inputPkg.getText() != null ? inputPkg.getText().toString().trim() : "";
            if (pkg.isEmpty()) { Toast.makeText(this, "הכנס שם חבילה", Toast.LENGTH_SHORT).show(); return; }
            Set<String> approved = prefManager.getApprovedApps();
            approved.add(pkg);
            prefManager.setApprovedApps(approved);
            lockTaskManager.updateApprovedPackages(approved);
            for (AppInfo a : allApps) { if (a.packageName.equals(pkg)) { a.isApproved = true; break; } }
            inputPkg.setText("");
            Toast.makeText(this, pkg + " הוסף ✓", Toast.LENGTH_SHORT).show();
            refreshWhitelistPanel();
        });
        refreshWhitelistPanel();
    }

    private void refreshWhitelistPanel() {
        LinearLayout container = findViewById(R.id.whitelistContainer);
        TextView emptyMsg = findViewById(R.id.whitelistEmpty);
        container.removeAllViews();

        Set<String> approved = prefManager.getApprovedApps();
        if (approved.isEmpty()) { emptyMsg.setVisibility(View.VISIBLE); return; }
        emptyMsg.setVisibility(View.GONE);

        List<String> sorted = new ArrayList<>(approved);
        Collections.sort(sorted);
        PackageManager pm = getPackageManager();

        for (String pkg : sorted) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(0xFFFFFFFF);

            try {
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(pm.getApplicationIcon(pkg));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(72, 72);
                lp.setMarginEnd(12);
                icon.setLayoutParams(lp);
                row.addView(icon);
            } catch (Exception ignored) {}

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            String appName = pkg;
            try { appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); } catch (Exception ignored) {}

            TextView nameView = new TextView(this);
            nameView.setText(appName);
            nameView.setTextColor(0xFF1A1A2E);
            nameView.setTextSize(14);

            TextView pkgView = new TextView(this);
            pkgView.setText(pkg);
            pkgView.setTextColor(0xFF9E9E9E);
            pkgView.setTextSize(10);

            info.addView(nameView); info.addView(pkgView);

            Button removeBtn = new Button(this);
            removeBtn.setText("הסר");
            removeBtn.setTextSize(12);
            removeBtn.setBackgroundColor(0xFFD32F2F);
            removeBtn.setTextColor(0xFFFFFFFF);

            final String fp = pkg, fn = appName;
            removeBtn.setOnClickListener(v -> {
                Set<String> newApproved = prefManager.getApprovedApps();
                newApproved.remove(fp);
                prefManager.setApprovedApps(newApproved);
                lockTaskManager.updateApprovedPackages(newApproved);
                for (AppInfo a : allApps) { if (a.packageName.equals(fp)) { a.isApproved = false; break; } }
                Toast.makeText(this, fn + " הוסר", Toast.LENGTH_SHORT).show();
                refreshWhitelistPanel();
            });

            row.addView(info); row.addView(removeBtn);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFC5CAE9);

            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(row); wrapper.addView(divider);
            container.addView(wrapper);
        }
    }

    private List<AppInfo> getAllInstalledApps() {
        List<AppInfo> result = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Set<String> approved = prefManager.getApprovedApps();
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo info : installed) {
            if (info.packageName.equals("com.bepikuach")) continue;
            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setPackage(info.packageName);
            if (pm.queryIntentActivities(launchIntent, 0).isEmpty()) continue;

            String name;
            try { name = pm.getApplicationLabel(info).toString(); } catch (Exception e) { name = info.packageName; }

            Drawable icon;
            try { icon = pm.getApplicationIcon(info.packageName); } catch (Exception e) { icon = pm.getDefaultActivityIcon(); }

            result.add(new AppInfo(name, info.packageName, icon, approved.contains(info.packageName)));
        }
        result.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
