package com.quangthe.nhacnho_uongthuoc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quangthe.nhacnho_uongthuoc.ui.screens.AddEditPillScreen
import com.quangthe.nhacnho_uongthuoc.ui.screens.MainScreen
import com.quangthe.nhacnho_uongthuoc.ui.screens.SettingsScreen
import com.quangthe.nhacnho_uongthuoc.ui.theme.SimpillTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val sharedPrefs by lazy { SharedPrefs(this) }
    private val viewModel: PillViewModel by viewModels {
        PillViewModelFactory(PillRepository(applicationContext, AppDatabase.getDatabase(this).pillDao()))
    }
    private val dialogs by lazy { Dialogs(this) }
    private val toasts by lazy { Toasts(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            toasts.showCustomToast("Đã cấp quyền thông báo")
        } else {
            toasts.showCustomToast("Cần bật quyền thông báo để dùng tính năng Test")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)
        showPendingCrashReport()
        checkOpenCount()
        checkAndRequestNotificationPermission()

        // Sync alarms for pills that don't have them set yet (migration logic from MainActivity.java)
        viewModel.allPills.observe(this) { pills ->
            pills.forEach { pill ->
                if (pill.alarmsSet == 0) {
                    pill.setAlarm(this)
                    pill.setStockupAlarm(this)
                    pill.alarmsSet = 1
                    viewModel.update(pill)
                }
            }
        }

        setContent {
            val themeType = sharedPrefs.themesPref
            SimpillTheme(themeType = themeType) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            viewModel = viewModel,
                            onAddPillClick = {
                                navController.navigate("add_edit_pill/-1")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            onPillClick = { pill ->
                                navController.navigate("add_edit_pill/${pill.primaryKey}")
                            }
                        )
                    }
                    composable(
                        route = "add_edit_pill/{pillPk}",
                        arguments = listOf(navArgument("pillPk") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val pillPk = backStackEntry.arguments?.getInt("pillPk") ?: -1
                        val pills by viewModel.allPills.observeAsState(initial = emptyList())
                        val pill = pills.find { it.primaryKey == pillPk }

                        AddEditPillScreen(
                            pill = pill,
                            onSave = { newPill ->
                                if (pillPk == -1) {
                                    viewModel.insert(newPill)
                                } else {
                                    viewModel.update(newPill)
                                }
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(Pill.PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY)) {
            val pk = intent.getIntExtra(Pill.PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY, -1)
            if (pk != -1) {
                toasts.showCustomToast("Đã mở từ thông báo thuốc!")
            }
        }
    }

    private fun showPendingCrashReport() {
        val crashFile = File(filesDir, Simpill.CRASH_REPORT_FILE_NAME)
        if (crashFile.exists()) {
            val crashData = crashFile.readText()
            crashFile.delete()
            dialogs.getCrashDialog(crashData).show()
        }
    }

    private fun checkOpenCount() {
        var count = sharedPrefs.openCountPref
        if (count == 0) {
            dialogs.welcomeDialog.show()
        } else if (count % 150 == 0) {
            dialogs.donationDialog.show()
        }
        count++
        sharedPrefs.openCountPref = count
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
