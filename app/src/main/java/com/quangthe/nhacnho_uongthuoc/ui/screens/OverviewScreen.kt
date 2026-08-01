package com.quangthe.nhacnho_uongthuoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Nền xám nhạt như trong ảnh
    ) {
        // 1. Header: Tổng quan
        TopAppBar(
            title = { Text("Tổng quan", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
            }
        )

        // 2. Dải ngày (Horizontal Calendar)
        HorizontalCalendar()

        // 3. Thanh lọc (Filter bar)
        FilterBar()

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Danh sách các cữ uống (Dose List)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            DoseStatusCard(
                scheduledTime = "06:25 AM",
                actualTime = "07:05 AM",
                period = "Sáng",
                statusColor = Color(0xFF4A80F1) // Màu xanh chủ đạo
            )
        }
    }
}

@Composable
fun HorizontalCalendar() {
    val days = listOf("T.4 29", "T.5 30", "T.6 31", "T.7 1", "CN 2", "T.2 3", "T.3 4")
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items(days) { dayInfo ->
            val parts = dayInfo.split(" ")
            val isSelected = parts[1] == "1" // Giả sử đang chọn ngày 1

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF5D6679) else Color.Transparent)
                    .padding(8.dp)
            ) {
                Text(parts[0], fontSize = 12.sp, color = if (isSelected) Color.White else Color.Gray)
                Text(parts[1], fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black)
            }
        }
    }
}

@Composable
fun FilterBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.LightGray, RoundedCornerShape(50))
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val icons = listOf(Icons.Default.Check, Icons.Default.Close, Icons.Default.Notifications, Icons.Default.Timer)
        icons.forEach { icon ->
            IconButton(onClick = { /* TODO */ }) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            if (icon != icons.last()) {
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray).align(Alignment.CenterVertically))
            }
        }
    }
}

@Composable
fun DoseStatusCard(
    scheduledTime: String,
    actualTime: String,
    period: String,
    statusColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Biểu tượng vòng tròn bên trái
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(statusColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nội dung thời gian bên phải
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = scheduledTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " ➡ ", // Mũi tên chuyển hướng
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = actualTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = period,
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
