package com.quangthe.nhacnho_uongthuoc.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.quangthe.nhacnho_uongthuoc.ArrayHelper
import com.quangthe.nhacnho_uongthuoc.DateTimeManager
import com.quangthe.nhacnho_uongthuoc.Pill
import com.quangthe.nhacnho_uongthuoc.PillViewModel
import com.quangthe.nhacnho_uongthuoc.R
import com.quangthe.nhacnho_uongthuoc.SharedPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PillViewModel,
    onAddPillClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPillClick: (Pill) -> Unit
) {
    val pills by viewModel.allPills.observeAsState(initial = emptyList())
    val trashPills by viewModel.trashPills.observeAsState(initial = emptyList())
    var showTrashSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val sharedPrefs = remember { SharedPrefs(context) }
    val is24HourFormat = sharedPrefs.get24HourFormatPref()
    var testPill by remember { mutableStateOf<Pill?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val pill = testPill
            if (pill != null) {
                pill.sendPillNotificationONLY_FOR_TEST(context, 0)
            }
            testPill = null
        } else {
            testPill = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simpill") },
                actions = {
                    IconButton(onClick = { showTrashSheet = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Trash")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPillClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Pill")
            }
        }
    ) { paddingValues ->
        if (pills.isEmpty()) {
            EmptyPillList(
                modifier = Modifier.padding(paddingValues),
                onAddClick = onAddPillClick
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = pills,
                    key = { it.primaryKey }
                ) { pill ->
                    PillItem(
                        pill = pill,
                        is24HourFormat = is24HourFormat,
                        onClick = { onPillClick(pill) },
                        onTakeDose = { index ->
                            pill.takePill(context, index)
                            viewModel.update(pill)
                        },
                        onReset = {
                            pill.resetPill(context, 0)
                            viewModel.update(pill)
                        },
                        onTestDose = {
                            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                testPill = pill
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val doses = pill.timesArray
                                if (doses.size == 1) {
                                    pill.sendPillNotificationONLY_FOR_TEST(context, 0)
                                } else {
                                    testPill = pill
                                }
                            }
                        },
                        onDelete = {
                            viewModel.softDelete(pill)
                        }
                    )
                }
            }
        }

        if (showTrashSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTrashSheet = false },
                sheetState = sheetState
            ) {
                TrashSheetContent(
                    trashPills = trashPills,
                    onRestore = { viewModel.restore(it) },
                    onDeletePermanently = { viewModel.deletePermanently(it) }
                )
            }
        }

        val pill = testPill
        if (pill != null && pill.timesArray.size > 1) {
            val times = pill.timesArray
            AlertDialog(
                onDismissRequest = { testPill = null },
                title = { Text("Chọn liều để kiểm tra") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        times.forEachIndexed { index, time ->
                            TextButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                                        PackageManager.PERMISSION_GRANTED
                                    ) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        pill.sendPillNotificationONLY_FOR_TEST(context, index)
                                        testPill = null
                                    }
                                }
                            ) {
                                Text(time)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { testPill = null }) { Text("Hủy") }
                }
            )
        }
    }
}

@Composable
fun TrashSheetContent(
    trashPills: List<Pill>,
    onRestore: (Pill) -> Unit,
    onDeletePermanently: (Pill) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(max = 400.dp)
    ) {
        Text(
            text = "Thùng rác",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (trashPills.isEmpty()) {
            Text(
                text = "Thùng rác trống.",
                modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashPills) { pill ->
                    TrashPillItem(
                        pill = pill,
                        onRestore = { onRestore(pill) },
                        onDeletePermanently = { onDeletePermanently(pill) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TrashPillItem(
    pill: Pill,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pill.name, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Refresh, contentDescription = "Khôi phục", tint = Color.Blue)
            }
            IconButton(onClick = onDeletePermanently) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa vĩnh viễn", tint = Color.Red)
            }
        }
    }
}

@Composable
fun EmptyPillList(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val catView = com.airbnb.lottie.LottieAnimationView(LocalContext.current)
            // Sử dụng empty.json thay vì cat.json để tránh chữ "error" nếu có
            catView.setAnimation(R.raw.empty)
            catView.repeatCount = android.animation.ValueAnimator.INFINITE
            catView.playAnimation()

            AndroidView(
                factory = { catView },
                modifier = Modifier.size(250.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Meo! Đang đợi ní thêm thuốc đó...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Thêm thuốc ngay")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PillItem(
    pill: Pill,
    is24HourFormat: Boolean,
    onClick: () -> Unit,
    onTakeDose: (Int) -> Unit,
    onReset: () -> Unit,
    onTestDose: () -> Unit,
    onDelete: () -> Unit
) {
    val bottleDrawableId = when (pill.bottleColor) {
        1 -> R.drawable.pill_bottle_color_1
        2 -> R.drawable.pill_bottle_color_2
        3 -> R.drawable.pill_bottle_color_3
        4 -> R.drawable.pill_bottle_color_4
        5 -> R.drawable.pill_bottle_color_5
        6 -> R.drawable.pill_bottle_color_6
        7 -> R.drawable.pill_bottle_color_7
        8 -> R.drawable.pill_bottle_color_8
        9 -> R.drawable.pill_bottle_color_9
        10 -> R.drawable.pill_bottle_color_10
        11 -> R.drawable.pill_bottle_color_11
        12 -> R.drawable.pill_bottle_color_12
        else -> R.drawable.pill_bottle_color_2
    }
    var menuExpanded by remember { mutableStateOf(false) }
    val dateTimeManager = remember { DateTimeManager() }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = bottleDrawableId),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pill.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reset ngày hôm nay") },
                                onClick = {
                                    menuExpanded = false
                                    onReset()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Test dose") },
                                onClick = {
                                    menuExpanded = false
                                    onTestDose()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa thuốc") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pill.timesArray.forEachIndexed { index, time ->
                        val isTaken = pill.isDoseTaken(index)
                        
                        // Lấy giờ thực tế uống từ chuỗi actualTimes
                        val actualTimePart = pill.actualTimes.split(ArrayHelper.STR_SEPARATOR).getOrNull(index)
                        val hasActualTime = isTaken && actualTimePart != null && actualTimePart != Pill.NULL_DB_ENTRY_STRING
                        
                        val displayTime = if (is24HourFormat) {
                            time
                        } else {
                            dateTimeManager.convert24HrTimeTo12HrTime(time)
                        }
                        
                        val finalDisplayText = if (hasActualTime) {
                            val actualDisplay = if (is24HourFormat) actualTimePart!! else dateTimeManager.convert24HrTimeTo12HrTime(actualTimePart!!)
                            "$displayTime ➡ $actualDisplay"
                        } else {
                            displayTime
                        }

                        AssistChip(
                            onClick = { if (!isTaken) onTakeDose(index) },
                            label = { Text(finalDisplayText) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isTaken) Color(0xFF81C784) else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (isTaken) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                if (pill.frequency > 1 || pill.supply >= 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val freqText = when (pill.frequency) {
                        1 -> ""
                        2 -> "Mỗi 2 ngày"
                        7 -> "Mỗi tuần"
                        else -> "Mỗi ${pill.frequency} ngày"
                    }
                    val parts = buildList {
                        if (freqText.isNotEmpty()) add(freqText)
                        if (pill.supply >= 0) add("Còn ${pill.supply} viên")
                    }
                    if (parts.isNotEmpty()) {
                        Text(
                            text = parts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
