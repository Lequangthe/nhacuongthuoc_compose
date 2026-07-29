/* (C) 2022 */
package com.quangthe.nhacnho_uongthuoc;

import com.quangthe.nhacnho_uongthuoc.R;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Settings extends AppCompatActivity implements Dialogs.SettingsDialogListener {

    final Toasts toasts = new Toasts(this);

    boolean settingsChanged = false;

    Button backButton;
    SwitchCompat clockIs24HrSwitch,
            darkDialogsSwitch,
            appSoundsSwitch;
    Button themesBtn, exportBtn, importBtn, deleteAllBtn;
    
    private static final int EXPORT_REQUEST_CODE = 1001;
    private static final int IMPORT_REQUEST_CODE = 1002;

    private final SharedPrefs sharedPrefs = new SharedPrefs(this);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().hasExtra("theme_changed_bool")
                && getIntent().getBooleanExtra("theme_changed_bool", false)) {
            settingsChanged = true;
        }
        setContentViewBasedOnThemeSetting();
        initWidgets();
        createOnClickListeners();

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (settingsChanged) {
                                    Process.killProcess(Process.myPid());
                                } else {
                                    finish();
                                }
                            }
                        });
    }

    private void createOnClickListeners() {
        Dialogs getDialogs = new Dialogs(this);

        deleteAllBtn.setOnClickListener(view -> {
            Dialog dialog = getDialogs.getDatabaseDeletionDialog();
            dialog.show();
            settingsChanged = true;
        });
        backButton.setOnClickListener(
                v -> {
                    if (settingsChanged) {
                        Process.killProcess(Process.myPid());
                    } else {
                        finish();
                    }
                });
        themesBtn.setOnClickListener(view -> getDialogs.getChooseThemeDialog().show());
        exportBtn.setOnClickListener(view -> exportDatabase());
        importBtn.setOnClickListener(view -> importDatabase());
        darkDialogsSwitch.setOnClickListener(
                view -> {
                    sharedPrefs.setDarkDialogsPref(darkDialogsSwitch.isChecked());
                    toasts.showCustomToast(
                            sharedPrefs.getDarkDialogsPref()
                                    ? getString(R.string.dark_dialogs_toast)
                                    : getString(R.string.light_dialogs_toast));
                    settingsChanged = true;
                });
        clockIs24HrSwitch.setOnClickListener(
                view -> {
                    sharedPrefs.set24HourTimeFormatPref(clockIs24HrSwitch.isChecked());
                    toasts.showCustomToast(
                            sharedPrefs.get24HourFormatPref()
                                    ? getString(R.string.time_format_24hr_toast)
                                    : getString(R.string.time_format_12hr_toast));
                    settingsChanged = true;
                });
        appSoundsSwitch.setOnClickListener(
                view -> {
                    sharedPrefs.setPillSoundPref(appSoundsSwitch.isChecked());
                    toasts.showCustomToast(
                            sharedPrefs.getPillSoundPref()
                                    ? getString(R.string.app_sounds_enabled)
                                    : getString(R.string.app_sounds_disabled));
                    settingsChanged = true;
                });
    }

    private void setContentViewBasedOnThemeSetting() {
        int theme = sharedPrefs.getThemesPref();

        if (theme == Simpill.BLUE_THEME) {
            setTheme(R.style.SimpillAppTheme_BlueBackground);
        } else if (theme == Simpill.GREY_THEME) {
            setTheme(R.style.SimpillAppTheme_GreyBackground);
        } else if (theme == Simpill.BLACK_THEME) {
            setTheme(R.style.SimpillAppTheme_BlackBackground);
        } else {
            setTheme(R.style.SimpillAppTheme_PurpleBackground);
        }

        setContentView(R.layout.app_settings);
    }

    private void initWidgets() {
        backButton = findViewById(R.id.back_button);
        themesBtn = findViewById(R.id.theme_select_btn);
        exportBtn = findViewById(R.id.export_db_btn);
        importBtn = findViewById(R.id.import_db_btn);
        clockIs24HrSwitch = findViewById(R.id.clock_24hr_switch);
        darkDialogsSwitch = findViewById(R.id.dark_dialogs_switch);
        deleteAllBtn = findViewById(R.id.delete_db_btn);
        appSoundsSwitch = findViewById(R.id.soundSwitch);

        clockIs24HrSwitch.setChecked(sharedPrefs.get24HourFormatPref());
        darkDialogsSwitch.setChecked(sharedPrefs.getDarkDialogsPref());
        appSoundsSwitch.setChecked(sharedPrefs.getPillSoundPref());
    }

    private void exportDatabase() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/x-sqlite3");
        intent.putExtra(Intent.EXTRA_TITLE, "PillList_Backup.db");
        startActivityForResult(intent, EXPORT_REQUEST_CODE);
    }

    private void importDatabase() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, IMPORT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (requestCode == EXPORT_REQUEST_CODE) {
                doExport(uri);
            } else if (requestCode == IMPORT_REQUEST_CODE) {
                doImport(uri);
            }
        }
    }

    private void doExport(Uri uri) {
        File dbFile = getDatabasePath("PillList.db");
        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = getContentResolver().openOutputStream(uri)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            toasts.showCustomToast(getString(R.string.export_success));
        } catch (IOException e) {
            e.printStackTrace();
            toasts.showCustomToast(getString(R.string.export_fail));
        }
    }

    private void doImport(Uri uri) {
        File dbFile = getDatabasePath("PillList.db");
        // Close database before importing
        new DatabaseHelper(this).close();

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dbFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            // Lập lịch lại báo thức TRƯỚC khi khởi động lại
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            Pill[] pills = dbHelper.getAllPills();
            for (Pill p : pills) {
                p.setAlarm(this);
                p.setStockupAlarm(this);
            }
            dbHelper.close();

            toasts.showCustomToast(getString(R.string.import_success));
            settingsChanged = true; // Refresh UI
            Process.killProcess(Process.myPid()); // Restart to apply new DB
        } catch (IOException e) {
            e.printStackTrace();
            toasts.showCustomToast(getString(R.string.import_fail));
        }
    }

    @Override
    public void recreateScreen() {
        finish();

        Intent intent = new Intent(this, Settings.class);
        intent.putExtra("theme_changed_bool", true);
        startActivity(intent);
    }
}
