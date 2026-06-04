package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.LogEntity
import com.example.data.StudentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StudentDetailsDialog(
    student: StudentEntity,
    logs: List<LogEntity>,
    isLight: Boolean,
    onDismissRequest: () -> Unit
) {
    var showLightbox by remember { mutableStateOf(false) }

    val dialogBg = if (isLight) Color.White else Color(0xFF1F2937) // Gray-800
    val textMain = if (isLight) Color(0xFF111827) else Color.White
    val textMuted = if (isLight) Color(0xFF6B7280) else Color(0xFF9CA3AF)
    val cardOutline = if (isLight) Color(0xFFE5E7EB) else Color(0xFF374151)
    val tealAccent = Color(0xFF14B8A6)

    // Calculate specific stats for this student
    val studentLogs = logs.filter { it.uid.equals(student.uid, ignoreCase = true) }
    
    val totalCheckIns = studentLogs.count { it.event.equals("CHECK_IN", ignoreCase = true) }
    
    // Check-in from 15:00 onwards is LATE IN
    val totalLates = studentLogs.count { log ->
        if (log.event.equals("CHECK_IN", ignoreCase = true)) {
            val calendar = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour >= 15
        } else false
    }

    val totalViolations = studentLogs.count { 
        it.event.equals("APB_VIOLATION", ignoreCase = true) || it.event.equals("Violation", ignoreCase = true) 
    }

    // Weekly unique attendance days (Monday to Sunday check)
    val uniqueDaysThisWeek = remember(studentLogs) {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfWeek = calendar.timeInMillis
        val endOfWeek = System.currentTimeMillis()

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val daysSet = mutableSetOf<String>()

        studentLogs.forEach { log ->
            if (log.event.equals("CHECK_IN", ignoreCase = true) && log.timestamp in startOfWeek..endOfWeek) {
                daysSet.add(format.format(Date(log.timestamp)))
            }
        }
        daysSet.size
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder(cardOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Control cancel button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textMuted)
                    }
                }

                // Profile card header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(tealAccent.copy(alpha = 0.1f))
                            .border(1.2.dp, tealAccent.copy(alpha = 0.3f), CircleShape)
                            .clickable {
                                if (student.photo != null && student.photo.isNotBlank()) {
                                    showLightbox = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (student.photo != null && student.photo.isNotBlank()) {
                            if (student.photo.startsWith("http")) {
                                AsyncImage(
                                    model = student.photo,
                                    contentDescription = student.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val bitmap = remember(student.photo) { decodeBase64ToBitmap(student.photo) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = student.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.School, contentDescription = null, tint = tealAccent, modifier = Modifier.size(36.dp))
                                }
                            }
                        } else {
                            Icon(Icons.Default.School, contentDescription = null, tint = tealAccent, modifier = Modifier.size(36.dp))
                        }
                    }

                    Text(
                        text = student.name,
                        color = textMain,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = "Lớp: ${student.className ?: "---"}",
                        color = tealAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isLight) Color(0xFFF3F4F6) else Color(0xFF111827),
                            border = BoxBorder(cardOutline)
                        ) {
                            Text(
                                text = student.studentId,
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tealAccent.copy(alpha = 0.1f),
                            border = BoxBorder(tealAccent.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = student.uid,
                                color = tealAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Stats Dashboard Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(title = "Điểm danh", value = "$totalCheckIns", valueColor = tealAccent, isLight = isLight, outline = cardOutline, modifier = Modifier.weight(1f))
                    StatBox(title = "Đi muộn", value = "$totalLates", valueColor = Color(0xFFF59E0B), isLight = isLight, outline = cardOutline, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(title = "Vi phạm", value = "$totalViolations", valueColor = Color(0xFFEF4444), isLight = isLight, outline = cardOutline, modifier = Modifier.weight(1f))
                    StatBox(title = "Đi học/Tuần", value = "$uniqueDaysThisWeek ngày", valueColor = Color(0xFF10B981), isLight = isLight, outline = cardOutline, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Log History Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = "History", tint = tealAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LỊCH SỬ ĐIỂM DANH",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Scrollable log history
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp)
                        .background(if (isLight) Color(0xFFF9FAFB) else Color(0xFF111827), RoundedCornerShape(8.dp))
                        .border(1.dp, cardOutline, RoundedCornerShape(8.dp))
                ) {
                    if (studentLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chưa có lịch sử quẹt thẻ.",
                                fontSize = 12.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            studentLogs.forEach { log ->
                                HistoryLogItem(log, isLight, textMain, textMuted, cardOutline, tealAccent)
                            }
                        }
                    }
                }
            }
        }
    }

    // High fidelity Lightbox popup
    AnimatedVisibility(
        visible = showLightbox,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Dialog(onDismissRequest = { showLightbox = false }) {
            Card(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = dialogBg)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (student.photo != null) {
                        if (student.photo.startsWith("http")) {
                            AsyncImage(
                                model = student.photo,
                                contentDescription = student.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val bitmap = remember(student.photo) { decodeBase64ToBitmap(student.photo) }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = student.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Floating close button
                    IconButton(
                        onClick = { showLightbox = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    valueColor: Color,
    isLight: Boolean,
    outline: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF9FAFB) else Color(0xFF111827).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        border = BoxBorder(outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun HistoryLogItem(
    log: LogEntity,
    isLight: Boolean,
    textMain: Color,
    textMuted: Color,
    outline: Color,
    tealAccent: Color
) {
    // Style badge dynamically
    val isLate = remember(log) {
        if (log.event.equals("CHECK_IN", ignoreCase = true)) {
            val calendar = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            calendar.get(Calendar.HOUR_OF_DAY) >= 15
        } else false
    }

    val (badgeText, badgeColor) = when {
        log.event.equals("APB_VIOLATION", ignoreCase = true) || log.event.equals("Violation", ignoreCase = true) -> {
            "VIOLATION" to Color(0xFFEF4444)
        }
        log.event.equals("CHECK_OUT", ignoreCase = true) -> {
            "CHECK_OUT" to Color(0xFF3B82F6)
        }
        isLate -> {
            "LATE IN" to Color(0xFFF59E0B)
        }
        else -> {
            "CHECK_IN" to Color(0xFF10B981)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isLight) Color.White else Color(0xFF1F2937).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .border(1.dp, outline, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = log.displayTime ?: "---",
                    color = textMain,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = "", tint = textMuted, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.deviceId,
                        color = textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor.copy(alpha = 0.1f),
                border = BoxBorder(badgeColor.copy(alpha = 0.2f))
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
