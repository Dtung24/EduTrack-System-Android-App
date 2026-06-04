package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassEntity
import com.example.data.LogEntity
import com.example.data.StudentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Gather states
    val students by viewModel.students.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val filterDate by viewModel.filterDate.collectAsState()
    val isLight by viewModel.isLightMode.collectAsState()
    val isChatOpen by viewModel.isChatOpen.collectAsState()

    // Dialog trigger states
    var showManageStudentsDialog by remember { mutableStateOf(false) }
    var selectedStudentForDetail by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedClassForDetail by remember { mutableStateOf<String?>(null) }
    var showConfirmClearLogs by remember { mutableStateOf(false) }

    // Color definitions
    val slateBg = if (isLight) Color(0xFFF3F4F6) else Color(0xFF111827) // Gray-100 vs Gray-900
    val cardBg = if (isLight) Color.White else Color(0xFF1F2937) // Gray-800
    val textMain = if (isLight) Color(0xFF111827) else Color.White
    val textMuted = if (isLight) Color(0xFF4B5563) else Color(0xFF9CA3AF)
    val cardOutline = if (isLight) Color(0xFFD1D5DB) else Color(0xFF374151)
    val tealAccent = Color(0xFF14B8A6)

    // Cached SimpleDateFormats to prevent expensive allocations during recompositions or list iterations
    val dmyFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val ymdFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Filter logs belonging to selected date. Cache formatters and reuse them.
    val filteredLogs = remember(logs, filterDate) {
        logs.filter { log ->
            val shortDate = try {
                if (log.displayTime != null && log.displayTime.contains(" ")) {
                    val datePart = log.displayTime.substringAfter(" ").trim()
                    val d = dmyFormatter.parse(datePart)
                    if (d != null) ymdFormatter.format(d) else ""
                } else {
                    ymdFormatter.format(Date(log.timestamp))
                }
            } catch (e: Exception) {
                ""
            }
            shortDate == filterDate
        }
    }

    val totalSwipes = filteredLogs.size
    val totalCheckIns = filteredLogs.count { it.event.equals("CHECK_IN", ignoreCase = true) }
    val totalCheckOuts = filteredLogs.count { it.event.equals("CHECK_OUT", ignoreCase = true) }
    val totalViolations = filteredLogs.count {
        it.event.equals("APB_VIOLATION", ignoreCase = true) || it.event.equals("Violation", ignoreCase = true)
    }

    val checkedInCount = remember(filteredLogs) {
        filteredLogs.filter { it.event.equals("CHECK_IN", ignoreCase = true) }.distinctBy { it.uid }.size
    }
    val totalStudents = students.size
    val attendanceRate = remember(checkedInCount, totalStudents) {
        if (totalStudents > 0) (checkedInCount * 100) / totalStudents else 0
    }
    val lastLog = remember(filteredLogs, logs) {
        filteredLogs.firstOrNull() ?: logs.firstOrNull()
    }
    val lastStudentName = remember(lastLog, students) {
        lastLog?.let { log -> students.find { it.uid.equals(log.uid, ignoreCase = true) }?.name } ?: "Chưa có lượt quét"
    }

    // Clear logs dialog
    if (showConfirmClearLogs) {
        AlertDialog(
            onDismissRequest = { showConfirmClearLogs = false },
            title = { Text("⚠️ XÓA TOÀN BỘ NHẬT KÝ", color = textMain, fontWeight = FontWeight.Bold) },
            text = { Text("Hành động này sẽ xóa vĩnh viễn toàn bộ nhật ký điểm danh cả trên mạng (Firebase) và bộ nhớ cache. Hãy đảm bảo bạn đã xuất dữ liệu ra file trước khi thực hiện!", color = textMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs { success, error ->
                            if (success) {
                                Toast.makeText(context, "Đã xóa toàn bộ nhật ký thành công!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lỗi khi xóa: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                        showConfirmClearLogs = false
                    }
                ) {
                    Text("Xác nhận xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearLogs = false }) {
                    Text("Hủy", color = textMain)
                }
            },
            containerColor = cardBg
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(slateBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main Dashboard Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header console
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "EduTrack",
                                tint = tealAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EduTrack",
                                color = tealAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Toolbar widgets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Theme Switcher
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isLight) Color(0xFFE5E7EB) else Color(0xFF1F2937))
                                    .clickable { viewModel.toggleTheme() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLight) Icons.Default.WbSunny else Icons.Default.ModeNight,
                                    contentDescription = "Theme Toggle",
                                    tint = tealAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Manage Students
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isLight) Color(0xFFE5E7EB) else Color(0xFF1F2937))
                                    .clickable { showManageStudentsDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Sinh viên",
                                    tint = tealAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Logout
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.12f))
                                    .clickable { viewModel.logout() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Thoát",
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Stats Bento Grid Block
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectedBentoHighlightCard(
                        totalSwipes = totalSwipes,
                        filterDate = filterDate,
                        isLight = isLight,
                        tealAccent = tealAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BentoEnergyCard(
                            rate = attendanceRate,
                            isLight = isLight,
                            modifier = Modifier.weight(1f)
                        )
                        BentoWarningCard(
                            totalViolations = totalViolations,
                            isLight = isLight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    BentoWideInfoCard(
                        lastLog = lastLog,
                        lastStudentName = lastStudentName,
                        totalStudents = totalStudents,
                        totalClasses = classes.size,
                        isLight = isLight,
                        cardOutline = cardOutline
                    )
                }

                // Logs and Filters Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BoxBorder(cardOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Table title with action buttons - weight used to prevent clashing and ensure responsive scaling
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Attendance Logs",
                                color = textMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Quick clear & Export Row - beautifully rounded with padded action buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { exportLogsToTextCsv(context, filteredLogs, students, filterDate) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xuất CSV", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showConfirmClearLogs = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xóa Logs", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Elegant interactive date filter row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isLight) Color(0xFFF3F4F6) else Color(0xFF111827))
                                .border(1.dp, cardOutline, RoundedCornerShape(16.dp))
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                            viewModel.setFilterDate(formattedDate)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = tealAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ngày đang lọc: $filterDate",
                                    color = textMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Date", tint = textMuted)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable grid table columns
                        val state = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(state)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, cardOutline, RoundedCornerShape(8.dp))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .width(760.dp)
                                    .heightIn(max = 450.dp)
                            ) {
                                // Header row columns as item
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isLight) Color(0xFFE5E7EB) else Color(0xFF111827))
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                    ) {
                                        TableCell(text = "Học sinh", weight = 1.3f, isHeader = true, color = textMuted)
                                        TableCell(text = "Mã sinh viên", weight = 1.0f, isHeader = true, color = textMuted)
                                        TableCell(text = "Mã thẻ UID", weight = 1.0f, isHeader = true, color = textMuted)
                                        TableCell(text = "Phòng học", weight = 1.0f, isHeader = true, color = textMuted)
                                        TableCell(text = "Trạng thái", weight = 1.0f, isHeader = true, color = textMuted)
                                        TableCell(text = "Thời gian quẹt", weight = 1.5f, isHeader = true, color = textMuted)
                                        TableCell(text = "Thời lượng", weight = 1.0f, isHeader = true, color = textMuted)
                                    }
                                    Divider(color = cardOutline)
                                }

                                if (filteredLogs.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Không thấy nhật ký nào phù hợp ngày lọc.",
                                                color = textMuted,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredLogs) { log ->
                                        LogTableRow(
                                            log = log,
                                            students = students,
                                            isLight = isLight,
                                            textMain = textMain,
                                            textMuted = textMuted,
                                            tealAccent = tealAccent,
                                            onStudentClick = { student ->
                                                selectedStudentForDetail = student
                                            }
                                        )
                                        Divider(color = cardOutline.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chat drawer sidebar if screen is wide (or can overlap on floating mobile)
            AnimatedVisibility(
                visible = isChatOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                AiChatPanel(viewModel = viewModel)
            }
        }

        // Floating Robot Action Button (Opens AI Panel)
        if (!isChatOpen) {
            FloatingActionButton(
                onClick = { viewModel.setChatOpen(true) },
                containerColor = tealAccent,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI Assistant",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Modal dialog overlays
    if (showManageStudentsDialog) {
        ManageStudentsDialog(
            viewModel = viewModel,
            students = students,
            classes = classes,
            onDismissRequest = { showManageStudentsDialog = false },
            onViewClass = { className ->
                selectedClassForDetail = className
            }
        )
    }

    selectedStudentForDetail?.let { student ->
        StudentDetailsDialog(
            student = student,
            logs = logs,
            isLight = isLight,
            onDismissRequest = { selectedStudentForDetail = null }
        )
    }

    selectedClassForDetail?.let { className ->
        ClassDetailsDialog(
            className = className,
            students = students,
            isLight = isLight,
            onDismissRequest = { selectedClassForDetail = null },
            onStudentClick = { student ->
                selectedStudentForDetail = student
            }
        )
    }
}

@Composable
fun SelectedBentoHighlightCard(
    totalSwipes: Int,
    filterDate: String,
    isLight: Boolean,
    tealAccent: Color
) {
    val bg = if (isLight) Color(0xFFDBE1FF) else Color(0xFF1B2347)
    val textMain = if (isLight) Color(0xFF00174B) else Color(0xFFDBE1FF)
    val buttonBg = if (isLight) Color(0xFF00174B) else Color(0xFFDBE1FF)
    val buttonIcon = if (isLight) Color.White else Color(0xFF00174B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 150.dp)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Hệ thống EduTrack".uppercase(),
                    color = textMain.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Giám sát thời gian thực",
                    color = textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$totalSwipes",
                        color = textMain,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 42.sp
                    )
                    Text(
                        text = "Lượt quẹt hôm nay ($filterDate)",
                        color = textMain.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(buttonBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Active Reader",
                        tint = buttonIcon,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BentoEnergyCard(
    rate: Int,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isLight) Color(0xFFE2E2EC) else Color(0xFF282A30)
    val textMain = if (isLight) Color(0xFF1A1C1E) else Color(0xFFECEFF1)
    val textMuted = if (isLight) Color(0xFF44474E) else Color(0xFF90A4AE)

    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 145.dp)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Attendance",
                    tint = textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = "Tỉ lệ có mặt",
                color = textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$rate%",
                    color = textMain,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                LinearProgressIndicator(
                    progress = rate / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = Color(0xFF14B8A6),
                    trackColor = (if (isLight) Color.White else Color(0xFF121212)).copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun BentoWarningCard(
    totalViolations: Int,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isLight) Color(0xFFFFD9E2) else Color(0xFF4C1D28)
    val textMain = if (isLight) Color(0xFF31111D) else Color(0xFFFFD9E2)

    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 145.dp)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alerts",
                    tint = textMain,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = "Tổng vi phạm",
                color = textMain.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "$totalViolations ca(s)",
                color = textMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BentoWideInfoCard(
    lastLog: LogEntity?,
    lastStudentName: String,
    totalStudents: Int,
    totalClasses: Int,
    isLight: Boolean,
    cardOutline: Color
) {
    val bg = if (isLight) Color.White else Color(0xFF1F2937)
    val textMain = if (isLight) Color(0xFF111827) else Color.White
    val textMuted = if (isLight) Color(0xFF4B5563) else Color(0xFF9CA3AF)
    val iconBoxBg = if (isLight) Color(0xFFF3F4F9) else Color(0xFF111827)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(20.dp),
        border = BoxBorder(cardOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Recent scan activity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBoxBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Scan",
                        tint = Color(0xFF3F51B5),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quẹt thẻ gần nhất",
                        color = textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (lastLog != null) "$lastStudentName • ${lastLog.event}" else "Hệ thống đang chờ...",
                        color = textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = lastLog?.displayTime?.substringBefore(" ") ?: "---",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 4.dp)
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Divider(color = cardOutline.copy(alpha = 0.5f))
            
            // Row 2: Database Si so information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBoxBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Sĩ số",
                        tint = Color(0xFF14B8A6),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sĩ số học viên & Lớp học",
                        color = textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalStudents sinh viên • $totalClasses lớp học",
                        color = textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LogTableRow(
    log: LogEntity,
    students: List<StudentEntity>,
    isLight: Boolean,
    textMain: Color,
    textMuted: Color,
    tealAccent: Color,
    onStudentClick: (StudentEntity) -> Unit
) {
    val student = remember(students, log.uid) {
        students.find { it.uid.equals(log.uid, ignoreCase = true) }
    }

    val isLate = remember(log) {
        if (log.event.equals("CHECK_IN", ignoreCase = true)) {
            val displayTime = log.displayTime
            if (displayTime != null && displayTime.contains(":")) {
                val hourStr = displayTime.substringBefore(":")
                val hour = hourStr.toIntOrNull()
                if (hour != null) {
                    hour >= 15
                } else {
                    val calendar = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                    calendar.get(Calendar.HOUR_OF_DAY) >= 15
                }
            } else {
                val calendar = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                calendar.get(Calendar.HOUR_OF_DAY) >= 15
            }
        } else false
    }

    val (badgeText, badgeColor) = when {
        log.event.equals("APB_VIOLATION", ignoreCase = true) || log.event.equals("Violation", ignoreCase = true) -> {
            "APB VIOLATION" to Color(0xFFEF4444)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name Column
        Box(modifier = Modifier.weight(1.3f)) {
            if (student != null) {
                Text(
                    text = student.name,
                    color = textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onStudentClick(student) }
                )
            } else {
                Text(
                    text = "Sinh viên chưa đăng ký",
                    color = textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // MSSV Column
        TableCell(text = student?.studentId ?: "---------", weight = 1.0f, color = textMuted)

        // Card ID
        TableCell(text = log.uid, weight = 1.0f, color = tealAccent, fontWeight = FontWeight.Bold)

        // Classroom
        TableCell(text = log.deviceId, weight = 1.0f, color = textMain)

        // Status Badge Column
        Box(modifier = Modifier.weight(1.0f)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor.copy(alpha = 0.1f),
                border = BoxBorder(badgeColor.copy(alpha = 0.2f))
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Timestamp
        TableCell(text = log.displayTime ?: "---", weight = 1.5f, color = textMuted)

        // Duration in class
        val durationText = when {
            log.durationSeconds != null && log.durationSeconds > 0 -> {
                val mins = log.durationSeconds / 60
                val secs = log.durationSeconds % 60
                "${mins}m ${secs}s"
            }
            log.event.equals("CHECK_IN", ignoreCase = true) -> {
                "In Class"
            }
            else -> "---"
        }
        TableCell(text = durationText, weight = 1.0f, color = textMain)
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    color: Color,
    fontWeight: FontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = color,
        fontSize = if (isHeader) 11.sp else 12.sp,
        fontWeight = fontWeight,
        fontFamily = if (isHeader) FontFamily.Default else FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// Share text format CSV report to copy or send directly
fun exportLogsToTextCsv(context: Context, logs: List<LogEntity>, students: List<StudentEntity>, date: String) {
    if (logs.isEmpty()) {
        Toast.makeText(context, "Không có lịch sử để xuất file!", Toast.LENGTH_SHORT).show()
        return
    }

    val csv = StringBuilder()
    csv.append("Họ tên sinh viên,MSSV,Mã thẻ (UID),Phòng học,Trạng thái điểm danh,Thời gian quẹt thẻ,Thời lượng học\n")
    logs.forEach { log ->
        val s = students.find { it.uid.equals(log.uid, ignoreCase = true) }
        val name = s?.name ?: "Sinh viên chưa đăng ký"
        val mssv = s?.studentId ?: "---------"
        val duration = if (log.durationSeconds != null) "${log.durationSeconds / 60}m ${log.durationSeconds % 60}s" else "---"
        csv.append("\"$name\",\"$mssv\",\"${log.uid}\",\"${log.deviceId}\",\"${log.event}\",\"${log.displayTime}\",\"$duration\"\n")
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "EduTrack Report - $date")
        putExtra(Intent.EXTRA_TEXT, csv.toString())
    }
    context.startActivity(Intent.createChooser(shareIntent, "Xuất báo cáo điểm danh ngày $date"))
}
