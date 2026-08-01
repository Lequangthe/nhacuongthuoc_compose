package com.quangthe.nhacnho_uongthuoc.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.quangthe.nhacnho_uongthuoc.AppDatabase
import com.quangthe.nhacnho_uongthuoc.Pill
import com.quangthe.nhacnho_uongthuoc.SharedPrefs
import com.quangthe.nhacnho_uongthuoc.Simpill
import com.quangthe.nhacnho_uongthuoc.Toasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SimpillSettings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { SharedPrefs(context) }

    var selectedTheme by remember { mutableStateOf(sharedPrefs.themesPref) }
    var is24HourFormat by remember { mutableStateOf(sharedPrefs.get24HourFormatPref()) }
    var soundOn by remember { mutableStateOf(sharedPrefs.pillSoundPref) }
    var stickyNotifications by remember { mutableStateOf(sharedPrefs.stickyNotificationsPref) }
    var darkDialogs by remember { mutableStateOf(sharedPrefs.darkDialogsPref) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        if (uri != null) {
            exportDatabase(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importDatabase(context, uri)
        }
    }

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionTitle("Giao diện")
            ThemeSelectionRow(
                selectedTheme = selectedTheme,
                onThemeSelected = { theme ->
                    selectedTheme = theme
                    sharedPrefs.setThemesPref(theme)
                }
            )
            SettingsSwitchItem(
                title = "Chế độ tối cho hộp thoại",
                checked = darkDialogs,
                onCheckedChange = {
                    darkDialogs = it
                    sharedPrefs.setDarkDialogsPref(it)
                }
            )

            Divider()

            SettingsSectionTitle("Thời gian & Thông báo")
            SettingsSwitchItem(
                title = "Sử dụng định dạng 24 giờ",
                checked = is24HourFormat,
                onCheckedChange = {
                    is24HourFormat = it
                    sharedPrefs.set24HourTimeFormatPref(it)
                }
            )
            SettingsSwitchItem(
                title = "Âm báo",
                checked = soundOn,
                onCheckedChange = {
                    soundOn = it
                    sharedPrefs.setPillSoundPref(it)
                }
            )
            SettingsSwitchItem(
                title = "Thông báo cố định",
                checked = stickyNotifications,
                onCheckedChange = {
                    stickyNotifications = it
                    sharedPrefs.setStickyNotificationsPref(it)
                }
            )

            Divider()

            SettingsSectionTitle("Dữ liệu")
            SettingsActionItem(
                title = "Xuất dữ liệu (sao lưu)",
                onClick = {
                    val timestamp = SimpleDateFormat("ddMMyyyy-HHmm", Locale.getDefault()).format(Date())
                    exportLauncher.launch("PillList_Backup_$timestamp.db")
                }
            )
            SettingsActionItem(
                title = "Nhập dữ liệu (khôi phục)",
                onClick = { importLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream", "*/*")) }
            )
            SettingsActionItem(
                title = "Xóa tất cả dữ liệu",
                onClick = { showDeleteAllDialog = true }
            )
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Xóa tất cả dữ liệu") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ dữ liệu? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.getDatabase(context).clearAllTables()
                        withContext(Dispatchers.Main) {
                            Toasts(context).showCustomToast("Đã xóa tất cả dữ liệu")
                            showDeleteAllDialog = false
                        }
                    }
                }) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Hủy") }
            }
        )
    }
}

private fun exportDatabase(context: Context, uri: Uri) {
    Log.d(TAG, "Starting export to $uri")
    val dbFile = context.getDatabasePath("PillList.db")
    if (!dbFile.exists()) {
        Log.e(TAG, "Database file not found!")
        return
    }
    
    try {
        // Force checkpoint and CLOSE the database to ensure main .db file is up-to-date
        val db = AppDatabase.getDatabase(context)
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { c ->
            if (c.moveToFirst()) {
                Log.d(TAG, "Checkpoint result: ${c.getInt(0)}, ${c.getInt(1)}, ${c.getInt(2)}")
            }
        }
        AppDatabase.closeDatabase()
        Log.d(TAG, "Database closed for export")

        context.contentResolver.openOutputStream(uri)?.use { output ->
            FileInputStream(dbFile).use { input ->
                val size = input.copyTo(output)
                Log.d(TAG, "Exported $size bytes")
            }
        }
        Toasts(context).showCustomToast("Đã xuất dữ liệu thành công!")
    } catch (e: Exception) {
        Log.e(TAG, "Export failed", e)
        Toasts(context).showCustomToast("Xuất dữ liệu thất bại!")
    }
}

private fun importDatabase(context: Context, uri: Uri) {
    Log.d(TAG, "Starting import from $uri")
    val importFile = context.getDatabasePath("PillList_import.db")
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1. Clear previous temp files
            importFile.delete()
            File(importFile.path + "-wal").delete()
            File(importFile.path + "-shm").delete()

            // 2. Copy selected file to internal temp location
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(importFile).use { output ->
                    val size = input.copyTo(output)
                    Log.d(TAG, "Copied $size bytes to temp import file")
                }
            } ?: throw IOException("Không đọc được file từ URI")

            // 3. Open temp database using Room
            val importedPills: List<Pill> = try {
                val importDb = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "PillList_import.db"
                ).build()
                
                Log.d(TAG, "Temp DB opened, querying pills...")
                val allPills = importDb.pillDao().getAllPillsSync()
                val trashPills = importDb.pillDao().getTrashPillsSync()
                val combined = allPills + trashPills
                Log.d(TAG, "Found ${allPills.size} active and ${trashPills.size} trash pills in backup")
                
                importDb.close()
                combined
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read imported database", e)
                importFile.delete()
                throw IOException("File backup không hợp lệ hoặc sai cấu trúc", e)
            }

            if (importedPills.isEmpty()) {
                Log.w(TAG, "No pills found in the imported database!")
                withContext(Dispatchers.Main) {
                    Toasts(context).showCustomToast("File backup không chứa dữ liệu thuốc nào!")
                }
                importFile.delete()
                return@launch
            }

            // 4. Merge imported pills into live database
            val liveDao = AppDatabase.getDatabase(context).pillDao()
            var restoredCount = 0
            for (pill in importedPills) {
                Log.d(TAG, "Importing pill: ${pill.name}")
                // Create a clean copy for the new database
                val newPill = pill.copy(
                    primaryKey = 0, // Let Room auto-generate
                    isDeleted = pill.isDeleted,
                    alarmsSet = 0,
                    taken = 0,
                    timeTaken = Pill.NULL_DB_ENTRY_STRING
                )
                val newId = liveDao.insertPill(newPill)
                
                // If not deleted, re-schedule alarms
                if (newPill.isDeleted == 0) {
                    val p = liveDao.getPillSync(newId.toInt())
                    if (p != null) {
                        p.setAlarmRequestCodes()
                        p.setAlarm(context)
                        p.setStockupAlarm(context)
                        p.alarmsSet = 1
                        liveDao.updatePillSync(p)
                    }
                }
                restoredCount++
            }

            Log.d(TAG, "Successfully merged $restoredCount pills")
            importFile.delete()
            
            withContext(Dispatchers.Main) {
                Toasts(context).showCustomToast("Đã nhập thành công $restoredCount thuốc!")
                (context.findActivity())?.recreate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            importFile.delete()
            withContext(Dispatchers.Main) {
                Toasts(context).showCustomToast("Lỗi nhập dữ liệu: ${e.localizedMessage}")
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionRow(
    selectedTheme: Int,
    onThemeSelected: (Int) -> Unit
) {
    val themes = listOf(
        Simpill.BLUE_THEME to "Xanh",
        Simpill.BLACK_THEME to "Đen",
        Simpill.GREY_THEME to "Xám",
        Simpill.PURPLE_THEME to "Tím"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Chủ đề ứng dụng", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.forEach { (themeId, themeName) ->
                FilterChip(
                    selected = selectedTheme == themeId,
                    onClick = { onThemeSelected(themeId) },
                    label = { Text(themeName) }
                )
            }
        }
    }
}
