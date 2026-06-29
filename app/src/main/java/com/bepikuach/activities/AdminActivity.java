package com.bepikuach.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bepikuach.R;
import com.bepikuach.admin.DeviceAdminReceiver;
import com.bepikuach.services.AppMonitorService;
import com.bepikuach.services.FloatingButtonService;
import com.bepikuach.utils.LockTaskManager;
import com.bepikuach.utils.PrefManager;
import com.bepikuach.utils.RestrictionsManager;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;
import java.util.Set;

public class AdminActivity extends AppCompatActivity {

    private static final int REQUEST_WALLPAPER = 101;
    private static final int REQUEST_DEVICE_ADMIN = 102;
    private static final int REQUEST_HOME = 103;

    private PrefManager prefs;
    private LockTaskManager ltm;
    private RestrictionsManager rm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        prefs = new PrefManager(this);
        ltm = new LockTaskManager(this);
        rm = new RestrictionsManager(this);

        setupToolbar();
        setupMenuButtons();
        setupRestrictionRows();
        setupIconSize();
        setupWallpaper();
        setupHomeMode();
        setupBlockedColor();
        setupPasswordChange();
        setupRemoveDeviceOwner();
        setupSaveButton();
        setupFloatButton();
        ensureDeviceAdmin();
        ensureAccessibility();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupMenuButtons() {
        findViewById(R.id.btnSelectApps).setOnClickListener(v ->
                startActivity(new Intent(this, SelectAppsActivity.class)));
        findViewById(R.id.btnAdbSetup).setOnClickListener(v ->
                startActivity(new Intent(this, AdbSetupActivity.class)));
    }

    private void ensureAccessibility() {
        if (!isAccessibilityEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle("נדרשת הרשאת נגישות")
                .setMessage("שירות הנגישות של בפיקוח נדרש לחסימת אפליקציות.\n\nלחץ אישור כדי לעבור להגדרות.")
                .setPositiveButton("עבור להגדרות", (d, w) ->
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton("אחר כך", null)
                .show();
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            int enabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled != 1) return false;
            String services = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return services != null && services.contains(getPackageName());
        } catch (Exception e) { return false; }
    }

    // כפתור אחד: דף בית או מצב רקע
    private void setupHomeMode() {
        Button btnSetHome = findViewById(R.id.btnSetAsHome);
        Button btnBgMode = findViewById(R.id.btnBackgroundMode);
        updateHomeModeButtons(btnSetHome, btnBgMode);

        btnSetHome.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager rm = getSystemService(RoleManager.class);
                if (rm != null) {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME);
                }
            } else {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            }
        });

        btnBgMode.setOnClickListener(v -> {
            boolean current = prefs.isBackgroundModeEnabled();
            if (!current) {
                addHomeAppToWhitelist();
                prefs.setBackgroundModeEnabled(true);
                startForegroundService(new Intent(this, AppMonitorService.class));
                Toast.makeText(this, "מצב רקע הופעל ✓", Toast.LENGTH_SHORT).show();
            } else {
                prefs.setBackgroundModeEnabled(false);
                stopService(new Intent(this, AppMonitorService.class));
                Toast.makeText(this, "מצב רקע כובה", Toast.LENGTH_SHORT).show();
            }
            updateHomeModeButtons(btnSetHome, btnBgMode);
        });
    }

    private void updateHomeModeButtons(Button btnHome, Button btnBg) {
        boolean bgMode = prefs.isBackgroundModeEnabled();
        btnHome.setBackgroundColor(0xFF3F51B5);
        btnHome.setTextColor(0xFFFFFFFF);
        btnBg.setBackgroundColor(bgMode ? 0xFF43A047 : 0xFF757575);
        btnBg.setTextColor(0xFFFFFFFF);
        btnBg.setText(bgMode ? "🟢 מצב רקע פעיל — לחץ לכיבוי" : "⚫ הפעל חסימה במצב רקע");
    }

    private void addHomeAppToWhitelist() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null) {
                String homePkg = ri.activityInfo.packageName;
                if (!homePkg.equals(getPackageName())) {
                    prefs.updateHomeApp(homePkg);
                }
            }
        } catch (Exception ignored) {}
    }

    private void setupBlockedColor() {
        Button btnColor = findViewById(R.id.btnBlockedColor);
        updateBlockedColorBtn(btnColor);
        btnColor.setOnClickListener(v -> {
            String[] names = {"אדום כהה", "כחול כהה", "שחור", "כהה-ירוק", "סגול"};
            int[] colors = {0xFFC62828, 0xFF1A237E, 0xFF212121, 0xFF1B5E20, 0xFF4A148C};
            new AlertDialog.Builder(this)
                .setTitle("צבע מסך חסימה")
                .setItems(names, (d, which) -> {
                    prefs.setBlockedScreenColor(colors[which]);
                    updateBlockedColorBtn(btnColor);
                })
                .show();
        });
    }

    private void updateBlockedColorBtn(Button btn) {
        int color = prefs.getBlockedScreenColor();
        btn.setBackgroundColor(color);
        btn.setTextColor(0xFFFFFFFF);
    }

    interface RestrictionSetter { void set(int value); }

    private void setupRestrictionRow(int statusId, int btnOffId, int btnOnId, int btnPermId,
                                      String name, int currentValue, RestrictionSetter setter) {
        updateRowStatus(statusId, currentValue);
        Button btnOff = findViewById(btnOffId);
        Button btnOn = findViewById(btnOnId);
        Button btnPerm = findViewById(btnPermId);
        highlightRow(currentValue, btnOff, btnOn, btnPerm);

        if (currentValue == 2) {
            btnOff.setEnabled(false); btnOn.setEnabled(false); btnPerm.setEnabled(false);
            return;
        }
        btnOff.setOnClickListener(v -> { setter.set(0); updateRowStatus(statusId, 0); highlightRow(0, btnOff, btnOn, btnPerm); applyAll(); });
        btnOn.setOnClickListener(v -> { setter.set(1); updateRowStatus(statusId, 1); highlightRow(1, btnOff, btnOn, btnPerm); applyAll(); });
        btnPerm.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("נעילה קבועה: " + name)
                .setMessage("לא ניתן לשנות גם מתפריט הניהול!\n\nהאם אתה בטוח?")
                .setPositiveButton("כן, נעל", (d, w) -> {
                    setter.set(2); updateRowStatus(statusId, 2); highlightRow(2, btnOff, btnOn, btnPerm);
                    btnOff.setEnabled(false); btnOn.setEnabled(false); btnPerm.setEnabled(false);
                    applyAll();
                })
                .setNegativeButton("ביטול", null).show()
        );
    }

    private void updateRowStatus(int statusId, int value) {
        TextView tv = findViewById(statusId);
        if (tv == null) return;
        switch (value) {
            case 0: tv.setText("כבוי"); tv.setTextColor(0xFF9E9E9E); break;
            case 1: tv.setText("פעיל"); tv.setTextColor(0xFF43A047); break;
            case 2: tv.setText("קבוע 🔐"); tv.setTextColor(0xFFD32F2F); break;
        }
    }

    private void highlightRow(int value, Button off, Button on, Button perm) {
        off.setBackgroundColor(value == 0 ? 0xFF757575 : 0xFFE8EAF6); off.setTextColor(value == 0 ? 0xFFFFFFFF : 0xFF3F51B5);
        on.setBackgroundColor(value == 1 ? 0xFF43A047 : 0xFFE8EAF6); on.setTextColor(value == 1 ? 0xFFFFFFFF : 0xFF3F51B5);
        perm.setBackgroundColor(value == 2 ? 0xFFD32F2F : 0xFFE8EAF6); perm.setTextColor(value == 2 ? 0xFFFFFFFF : 0xFF3F51B5);
    }

    private void applyAll() {
        ltm.updateLockTaskFeatures(prefs);
        ltm.updateStatusBar(prefs.isBlockStatusBar());
        rm.applyAll(prefs);
    }

    private void setupRestrictionRows() {
        if (!rm.isDeviceOwner) findViewById(R.id.deviceOwnerNotice).setVisibility(View.VISIBLE);
        setupRestrictionRow(R.id.statusStatusBar, R.id.btnStatusBarOff, R.id.btnStatusBarOn, R.id.btnStatusBarPerm, "חסימת שורת התראות", prefs.getBlockStatusBar(), prefs::setBlockStatusBar);
        setupRestrictionRow(R.id.statusInstall, R.id.btnInstallOff, R.id.btnInstallOn, R.id.btnInstallPerm, "חסימת התקנות", prefs.getBlockInstall(), prefs::setBlockInstall);
        setupRestrictionRow(R.id.statusWifi, R.id.btnWifiOff, R.id.btnWifiOn, R.id.btnWifiPerm, "חסימת WiFi", prefs.getBlockWifi(), prefs::setBlockWifi);
        setupRestrictionRow(R.id.statusHotspot, R.id.btnHotspotOff, R.id.btnHotspotOn, R.id.btnHotspotPerm, "חסימת נקודה חמה", prefs.getBlockHotspot(), prefs::setBlockHotspot);
        setupRestrictionRow(R.id.statusUsb, R.id.btnUsbOff, R.id.btnUsbOn, R.id.btnUsbPerm, "חסימת USB", prefs.getBlockUsb(), prefs::setBlockUsb);
        setupRestrictionRow(R.id.statusFactoryReset, R.id.btnFactoryResetOff, R.id.btnFactoryResetOn, R.id.btnFactoryResetPerm, "חסימת איפוס מכשיר", prefs.getBlockFactoryReset(), prefs::setBlockFactoryReset);
        setupRestrictionRow(R.id.statusUninstall, R.id.btnUninstallOff, R.id.btnUninstallOn, R.id.btnUninstallPerm, "חסימת הסרת אפליקציות", prefs.getBlockUninstall(), prefs::setBlockUninstall);
        setupRestrictionRow(R.id.statusDevOptions, R.id.btnDevOptionsOff, R.id.btnDevOptionsOn, R.id.btnDevOptionsPerm, "חסימת אפשרויות מפתחים", prefs.getBlockDevOptions(), prefs::setBlockDevOptions);
    }

    private void setupIconSize() {
        Button s = findViewById(R.id.btnIconSmall), m = findViewById(R.id.btnIconMedium), l = findViewById(R.id.btnIconLarge);
        highlightIconSize(prefs.getIconSize(), s, m, l);
        s.setOnClickListener(v -> { prefs.setIconSize(0); highlightIconSize(0, s, m, l); });
        m.setOnClickListener(v -> { prefs.setIconSize(1); highlightIconSize(1, s, m, l); });
        l.setOnClickListener(v -> { prefs.setIconSize(2); highlightIconSize(2, s, m, l); });
    }

    private void highlightIconSize(int size, Button s, Button m, Button l) {
        int a = 0xFF3F51B5, i = 0xFFE8EAF6;
        s.setBackgroundColor(size==0?a:i); s.setTextColor(size==0?0xFFFFFFFF:0xFF3F51B5);
        m.setBackgroundColor(size==1?a:i); m.setTextColor(size==1?0xFFFFFFFF:0xFF3F51B5);
        l.setBackgroundColor(size==2?a:i); l.setTextColor(size==2?0xFFFFFFFF:0xFF3F51B5);
    }

    private void setupWallpaper() {
        findViewById(R.id.btnWallpaper).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_WALLPAPER);
        });
    }

    private void setupPasswordChange() {
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            TextInputEditText p1 = findViewById(R.id.newPassword);
            TextInputEditText p2 = findViewById(R.id.confirmPassword);
            String pass1 = p1.getText() != null ? p1.getText().toString() : "";
            String pass2 = p2.getText() != null ? p2.getText().toString() : "";
            if (pass1.isEmpty()) { Toast.makeText(this, "הכנס סיסמה", Toast.LENGTH_SHORT).show(); return; }
            if (!pass1.equals(pass2)) { Toast.makeText(this, "הסיסמאות אינן תואמות", Toast.LENGTH_SHORT).show(); return; }
            prefs.setPassword(pass1);
            p1.setText(""); p2.setText("");
            Toast.makeText(this, "הסיסמה עודכנה ✓", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRemoveDeviceOwner() {
        Button btnRemove = findViewById(R.id.btnRemoveDeviceOwner);
        Button btnLockRemove = findViewById(R.id.btnLockRemoveOwner);
        TextView statusLockRemove = findViewById(R.id.statusLockRemoveOwner);

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        boolean isOwner = dpm != null && dpm.isDeviceOwnerApp(getPackageName());

        if (!isOwner) {
            btnRemove.setText("Device Owner לא מוגדר"); btnRemove.setEnabled(false);
            btnLockRemove.setVisibility(View.GONE); statusLockRemove.setVisibility(View.GONE);
            return;
        }

        int lockLevel = prefs.getLockRemoveOwner();
        if (lockLevel == 2) {
            btnRemove.setVisibility(View.GONE); btnLockRemove.setEnabled(false);
            statusLockRemove.setText("כפתור הסרה נעול לצמיתות 🔐");
            statusLockRemove.setTextColor(0xFFD32F2F); return;
        }

        btnRemove.setOnClickListener(v -> showPasswordThenRemove(dpm));
        btnLockRemove.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("נעל כפתור הסרה לצמיתות")
                .setMessage("לאחר אישור כפתור הסרת Device Owner יוסתר לצמיתות.\n\nלא ניתן לבטל!")
                .setPositiveButton("כן, נעל", (d, w) -> {
                    prefs.setLockRemoveOwner(2);
                    btnRemove.setVisibility(View.GONE); btnLockRemove.setEnabled(false);
                    statusLockRemove.setText("כפתור הסרה נעול לצמיתות 🔐");
                    statusLockRemove.setTextColor(0xFFD32F2F);
                    Toast.makeText(this, "נעול לצמיתות ✓", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null).show()
        );
    }

    private void showPasswordThenRemove(DevicePolicyManager dpm) {
        View view = LayoutInflater.from(this).inflate(R.layout.activity_password, null);
        TextInputEditText input = view.findViewById(R.id.passwordInput);
        new AlertDialog.Builder(this)
            .setTitle("הסר Device Owner")
            .setMessage("הכנס סיסמת מנהל לאישור:")
            .setView(view)
            .setPositiveButton("אשר והסר", (d, w) -> {
                String entered = input.getText() != null ? input.getText().toString() : "";
                if (prefs.checkPassword(entered)) {
                    try {
                        dpm.clearDeviceOwnerApp(getPackageName());
                        Toast.makeText(this, "Device Owner הוסר ✓", Toast.LENGTH_LONG).show();
                        ((Button)findViewById(R.id.btnRemoveDeviceOwner)).setText("הוסר ✓");
                        ((Button)findViewById(R.id.btnRemoveDeviceOwner)).setEnabled(false);
                    } catch (Exception e) {
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else { Toast.makeText(this, "סיסמה שגויה", Toast.LENGTH_SHORT).show(); }
            })
            .setNegativeButton("ביטול", null).show();
    }

    private void ensureDeviceAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, DeviceAdminReceiver.class);
        if (dpm != null && !dpm.isAdminActive(admin)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "נדרש להפעלת הגנת בפיקוח");
            startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
        }
    }

    private void setupSaveButton() {
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            Toast.makeText(this, "ההגדרות נשמרו ✓", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HOME) {
            // בדוק אם בפיקוח הוגדרה כדף הבית
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager rm = getSystemService(RoleManager.class);
                if (rm != null && rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    Toast.makeText(this, "בפיקוח הוגדרה כדף הבית ✓", Toast.LENGTH_SHORT).show();
                } else {
                    // לא הוגדרה — אפשר להגדיר אחר
                    Toast.makeText(this, "לא הוגדר — ניתן לבחור אפליקציה אחרת", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == REQUEST_WALLPAPER && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    // שמור הרשאה קבועה
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    prefs.setWallpaperPath(uri.toString());
                    Toast.makeText(this, "הטפט עודכן ✓", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    // fallback — העתק לקובץ
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        java.io.File dest = new java.io.File(getFilesDir(), "wallpaper.jpg");
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
                        byte[] buf = new byte[4096]; int len;
                        while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                        is.close(); fos.close();
                        prefs.setWallpaperPath(dest.getAbsolutePath());
                        Toast.makeText(this, "הטפט עודכן ✓", Toast.LENGTH_SHORT).show();
                    } catch (Exception ex) {
                        Toast.makeText(this, "שגיאה בטעינת הטפט", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
    @Override public void onBackPressed() { finish(); }

    private void setupFloatButton() {
        android.widget.Switch swFloat = findViewById(R.id.switchFloatBtn);
        Button btnTL = findViewById(R.id.btnFloatTopLeft), btnTR = findViewById(R.id.btnFloatTopRight);
        Button btnBL = findViewById(R.id.btnFloatBottomLeft), btnBR = findViewById(R.id.btnFloatBottomRight);
        swFloat.setChecked(prefs.isFloatBtnEnabled());
        highlightFloatBtn(prefs.getFloatBtnGravity(), btnTL, btnTR, btnBL, btnBR);
        swFloat.setOnCheckedChangeListener((b, checked) -> {
            prefs.setFloatBtnEnabled(checked);
            stopService(new Intent(this, FloatingButtonService.class));
            if (checked) startService(new Intent(this, FloatingButtonService.class));
        });
        View.OnClickListener cl = v -> {
            int g = 0;
            if (v.getId() == R.id.btnFloatTopLeft) g = 1;
            else if (v.getId() == R.id.btnFloatTopRight) g = 2;
            else if (v.getId() == R.id.btnFloatBottomLeft) g = 3;
            else if (v.getId() == R.id.btnFloatBottomRight) g = 4;
            prefs.setFloatBtnGravity(g);
            highlightFloatBtn(g, btnTL, btnTR, btnBL, btnBR);
            stopService(new Intent(this, FloatingButtonService.class));
            startService(new Intent(this, FloatingButtonService.class));
        };
        btnTL.setOnClickListener(cl); btnTR.setOnClickListener(cl);
        btnBL.setOnClickListener(cl); btnBR.setOnClickListener(cl);
    }

    private void highlightFloatBtn(int g, Button tl, Button tr, Button bl, Button br) {
        int a = 0xFF3F51B5, i = 0xFFE8EAF6;
        tl.setBackgroundColor(g==1?a:i); tl.setTextColor(g==1?0xFFFFFFFF:0xFF3F51B5);
        tr.setBackgroundColor(g==2?a:i); tr.setTextColor(g==2?0xFFFFFFFF:0xFF3F51B5);
        bl.setBackgroundColor(g==3?a:i); bl.setTextColor(g==3?0xFFFFFFFF:0xFF3F51B5);
        br.setBackgroundColor(g==4?a:i); br.setTextColor(g==4?0xFFFFFFFF:0xFF3F51B5);
    }
}
