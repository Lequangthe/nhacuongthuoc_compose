package com.quangthe.nhacnho_uongthuoc.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quangthe.nhacnho_uongthuoc.AudioHelper
import com.quangthe.nhacnho_uongthuoc.DateTimeManager
import com.quangthe.nhacnho_uongthuoc.Pill
import com.quangthe.nhacnho_uongthuoc.R
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPillScreen(
    pill: Pill? = null,
    onSave: (Pill) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dateTimeManager = remember { DateTimeManager() }

    var name by remember { mutableStateOf(pill?.name ?: "") }
    var frequency by remember { mutableStateOf(pill?.frequency ?: 1) }
    val times = remember { mutableStateListOf<String>().apply { 
        if (pill != null) addAll(pill.timesArray) else add("08:00")
    } }
    // Lọc bỏ các cữ uống trống trước khi lưu
    val filteredTimes = times.filter { it.isNotBlank() }
    var startDate by remember { mutableStateOf(pill?.startDate ?: dateTimeManager.currentDateString) }
    var supply by remember { mutableStateOf(if (pill != null && pill.supply >= 0) pill.supply.toString() else "") }
    var stockupDate by remember { mutableStateOf(pill?.stockupDate ?: "null") }
    var bottleColor by remember { mutableStateOf(pill?.bottleColor ?: 2) }
    var alarmType by remember { mutableStateOf(pill?.alarmType ?: Pill.ALARM) }
    var customAlarmUriString by remember {
        mutableStateOf(pill?.customAlarmUriString ?: Pill.getDefaultAlarmUri().toString())
    }
    var isPlaying by remember { mutableStateOf(false) }
    val alarmPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            alarmPlayer.value?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
    }
    val audioHelper = remember { AudioHelper(context) }

    val openAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            customAlarmUriString = uri.toString()
            alarmType = Pill.CUSTOM_ALARM
        }
    }

    fun queryDisplayName(uri: Uri): String {
        var displayName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) displayName = cursor.getString(index)
            }
        }
        return displayName ?: uri.lastPathSegment ?: "Âm báo tùy chỉnh"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pill == null) "Thêm thuốc mới" else "Chỉnh sửa thuốc") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (name.isBlank() || filteredTimes.isEmpty()) return@IconButton
                        
                        val newPill = (pill?.copy() ?: Pill()).apply {
                            this.name = name
                            this.frequency = frequency
                            this.startDate = startDate
                            this.supply = supply.toIntOrNull() ?: -1
                            this.stockupDate = stockupDate
                            this.bottleColor = bottleColor
                            this.alarmType = alarmType
                            this.customAlarmUriString = customAlarmUriString
                            this.timesArray = filteredTimes.toTypedArray()
                            this.updateDerivedFields()
                        }
                        onSave(newPill)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên thuốc") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Cữ uống", style = MaterialTheme.typography.titleMedium)
            times.forEachIndexed { index, time ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val parts = time.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, h, m ->
                                times[index] = String.format("%02d:%02d", h, m)
                            }, hour, minute, true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(time)
                    }
                    if (times.size > 1) {
                        IconButton(onClick = { times.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
            Button(
                onClick = { times.add("08:00") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Thêm cữ uống")
            }

            Text(text = "Tần suất: $frequency ngày/lần")
            Slider(
                value = frequency.toFloat(),
                onValueChange = { frequency = it.toInt() },
                valueRange = 1f..30f,
                steps = 29
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Ngày bắt đầu", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = {
                            val parts = (if (startDate == "null") dateTimeManager.currentDateString else startDate).split("/")
                            val y = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
                            val d = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            DatePickerDialog(context, { _, year, month, day ->
                                startDate = String.format("%04d/%02d/%02d", year, month + 1, day)
                            }, y, m, d).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (startDate == "null") "Chọn ngày" else startDate)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Ngày nhập kho", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = {
                            val parts = if (stockupDate == "null") dateTimeManager.currentDateString.split("/") else stockupDate.split("/")
                            val y = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
                            val d = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            DatePickerDialog(context, { _, year, month, day ->
                                stockupDate = String.format("%04d/%02d/%02d", year, month + 1, day)
                            }, y, m, d).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (stockupDate == "null") "Chọn ngày" else stockupDate)
                    }
                }
            }

            OutlinedTextField(
                value = supply,
                onValueChange = { supply = it },
                label = { Text("Số lượng thuốc hiện có") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Màu sắc lọ thuốc", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..12).forEach { colorId ->
                    val resId = when (colorId) {
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
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (bottleColor == colorId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { bottleColor = colorId }
                            .padding(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = if (alarmType == Pill.ALARM || alarmType == Pill.CUSTOM_ALARM) "Chế độ: Báo động" else "Chế độ: Thông báo")
                Switch(
                    checked = alarmType == Pill.ALARM || alarmType == Pill.CUSTOM_ALARM,
                    onCheckedChange = { alarmType = if (it) Pill.ALARM else Pill.NOTIFICATION }
                )
            }

            Text(text = "Âm báo", style = MaterialTheme.typography.titleMedium)
            val customUri = runCatching { Uri.parse(customAlarmUriString) }.getOrNull()
            val isDefaultSound = customUri == null || customUri == Pill.getDefaultAlarmUri()
            val soundLabel = if (isDefaultSound) {
                "Mặc định"
            } else {
                queryDisplayName(customUri!!)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { openAudioLauncher.launch(arrayOf("audio/*", "application/ogg")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isDefaultSound) "Chọn âm báo tùy chỉnh" else "Đổi âm báo")
                }
                if (!isDefaultSound) {
                    IconButton(onClick = {
                        if (isPlaying) {
                            alarmPlayer.value?.let {
                                if (it.isPlaying) it.stop()
                                it.release()
                            }
                            alarmPlayer.value = null
                            isPlaying = false
                        } else {
                            val player = audioHelper.getAlarmPlayer(customUri!!)
                            player.start()
                            alarmPlayer.value = player
                            isPlaying = true
                        }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Dừng" else "Nghe thử"
                        )
                    }
                }
            }
            if (!isDefaultSound) {
                Text(
                    text = soundLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Dùng âm báo mặc định của app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
