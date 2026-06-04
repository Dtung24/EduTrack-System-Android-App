package com.example.ui

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.StudentEntity

@Composable
fun ClassDetailsDialog(
    className: String,
    students: List<StudentEntity>,
    isLight: Boolean,
    onDismissRequest: () -> Unit,
    onStudentClick: (StudentEntity) -> Unit // Navigates to student details
) {
    val dialogBg = if (isLight) Color.White else Color(0xFF1F2937) // Gray-800
    val textMain = if (isLight) Color(0xFF111827) else Color.White
    val textMuted = if (isLight) Color(0xFF6B7280) else Color(0xFF9CA3AF)
    val cardOutline = if (isLight) Color(0xFFE5E7EB) else Color(0xFF374151)
    val tealAccent = Color(0xFF14B8A6)

    // Filter students belonging to this class
    val classStudents = students.filter { it.className.equals(className, ignoreCase = true) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
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
                // Header with cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tealAccent.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = "", tint = tealAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = className,
                                color = textMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sĩ số: ${classStudents.size} sinh viên",
                                color = textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DANH SÁCH SINH VIÊN TRONG LỚP",
                    fontSize = 10.sp,
                    color = textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Grid list of students
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(if (isLight) Color(0xFFF9FAFB) else Color(0xFF111827), RoundedCornerShape(16.dp))
                        .border(1.dp, cardOutline, RoundedCornerShape(16.dp))
                ) {
                    if (classStudents.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chưa có sinh viên nào đăng ký lớp này.",
                                fontSize = 12.sp,
                                color = textMuted
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
                            classStudents.forEach { student ->
                                ClassStudentRow(student, isLight, textMain, textMuted, cardOutline, tealAccent, onStudentClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassStudentRow(
    student: StudentEntity,
    isLight: Boolean,
    textMain: Color,
    textMuted: Color,
    outline: Color,
    tealAccent: Color,
    onStudentClick: (StudentEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isLight) Color.White else Color(0xFF1F2937).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .border(1.dp, outline, RoundedCornerShape(6.dp))
            .clickable { onStudentClick(student) }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Photo
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isLight) Color(0xFFF3F4F6) else Color(0xFF111827)),
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
                            Icon(Icons.Default.Person, contentDescription = student.name, tint = tealAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Icon(Icons.Default.Person, contentDescription = student.name, tint = tealAccent, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    color = textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "MSSV: ${student.studentId}",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
