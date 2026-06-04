package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.ClassEntity
import com.example.data.StudentEntity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.random.Random

// Helper to decode Base64 back into Bitmap for rendering
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        // Strip out header if present (e.g. data:image/jpeg;base64,)
        val cleanBase64 = if (base64Str.contains(",")) {
            base64Str.substring(base64Str.indexOf(",") + 1)
        } else {
            base64Str
        }
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

// Predefined demo avatar templates to simplify reviewer tests
val DEMO_AVATAR_LINKS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStudentsDialog(
    viewModel: MainViewModel,
    students: List<StudentEntity>,
    classes: List<ClassEntity>,
    onDismissRequest: () -> Unit,
    onViewClass: (String) -> Unit // Navigates to custom class breakdown
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Sinh viên, 1: Lớp hành chính
    val isLight by viewModel.isLightMode.collectAsState()

    val dialogBg = if (isLight) Color.White else Color(0xFF1F2937) // Gray-800
    val textMain = if (isLight) Color(0xFF111827) else Color.White
    val textMuted = if (isLight) Color(0xFF6B7280) else Color(0xFF9CA3AF)
    val cardOutline = if (isLight) Color(0xFFE5E7EB) else Color(0xFF374151)
    val tealAccent = Color(0xFF14B8A6)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Quản lý sinh viên",
                            tint = tealAccent,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "Student Management",
                            color = tealAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Buttons
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = tealAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = tealAccent
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Sinh viên", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selectedContentColor = tealAccent,
                        unselectedContentColor = textMuted
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Lớp hành chính", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selectedContentColor = tealAccent,
                        unselectedContentColor = textMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        StudentTabContent(viewModel, students, classes, isLight, dialogBg, textMain, textMuted, cardOutline, tealAccent)
                    } else {
                        ClassTabContent(viewModel, classes, isLight, dialogBg, textMain, textMuted, cardOutline, tealAccent, onViewClass)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTabContent(
    viewModel: MainViewModel,
    students: List<StudentEntity>,
    classes: List<ClassEntity>,
    isLight: Boolean,
    dialogBg: Color,
    textMain: Color,
    textMuted: Color,
    cardOutline: Color,
    tealAccent: Color
) {
    val context = LocalContext.current

    // Form inputs state
    var cardUid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var base64Photo by remember { mutableStateOf("") }
    var photoPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editingUid by remember { mutableStateOf<String?>(null) }

    var expandedClassSelect by remember { mutableStateOf(false) }

    // Launcher for image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // Compress bitmap
                    val compressedBase64 = compressBitmapToBase64(bitmap)
                    base64Photo = compressedBase64
                    photoPreviewBitmap = bitmap
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi nén ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val resetForm = {
        cardUid = ""
        name = ""
        studentId = ""
        selectedClass = ""
        base64Photo = ""
        photoPreviewBitmap = null
        editingUid = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Register / Edit Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF9FAFB) else Color(0xFF111827)),
                shape = RoundedCornerShape(16.dp),
                border = BoxBorder(cardOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingUid != null) "CHỈNH SỬA SINH VIÊN" else "ĐĂNG KÝ SINH VIÊN MỚI",
                        fontSize = 11.sp,
                        color = tealAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Card UID Field
                    OutlinedTextField(
                        value = cardUid,
                        onValueChange = { cardUid = it.uppercase() },
                        label = { Text("Mã thẻ UID (Mã Hex)", fontSize = 11.sp) },
                        enabled = editingUid == null, // Lock UID on Edit
                        colors = outlinedTextFieldColors(isLight, tealAccent, cardOutline),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Student Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên", fontSize = 11.sp) },
                        colors = outlinedTextFieldColors(isLight, tealAccent, cardOutline),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Row of Student ID and Class
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = studentId,
                            onValueChange = { studentId = it },
                            label = { Text("Mã sinh viên", fontSize = 11.sp) },
                            colors = outlinedTextFieldColors(isLight, tealAccent, cardOutline),
                            singleLine = true,
                            modifier = Modifier.weight(1.1f)
                        )

                        Box(modifier = Modifier.weight(0.9f)) {
                            OutlinedTextField(
                                value = selectedClass.ifBlank { "Chọn lớp..." },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Lớp học", fontSize = 11.sp) },
                                colors = outlinedTextFieldColors(isLight, tealAccent, cardOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedClassSelect = true },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { expandedClassSelect = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Chọn lớp")
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = expandedClassSelect,
                                onDismissRequest = { expandedClassSelect = false },
                                modifier = Modifier.background(dialogBg)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("-- Chọn lớp học --", color = textMain) },
                                    onClick = {
                                        selectedClass = ""
                                        expandedClassSelect = false
                                    }
                                )
                                classes.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text(cls.className, color = textMain) },
                                        onClick = {
                                            selectedClass = cls.className
                                            expandedClassSelect = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Photo Picker Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isLight) Color(0xFFE5E7EB) else Color(0xFF1F2937)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = tealAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Chọn ảnh",
                                color = if (isLight) Color.DarkGray else Color.LightGray,
                                fontSize = 11.sp
                            )
                        }

                        // Preloaded Demo Avatars for fast local tests
                        Button(
                            onClick = {
                                val randomAvatar = DEMO_AVATAR_LINKS[Random.nextInt(DEMO_AVATAR_LINKS.size)]
                                base64Photo = randomAvatar
                                photoPreviewBitmap = null // Resets locally decoded preview
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isLight) Color(0xFFE5E7EB) else Color(0xFF1F2937)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Demo Profile", tint = tealAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ảnh mẫu", color = if (isLight) Color.DarkGray else Color.LightGray, fontSize = 11.sp)
                        }

                        // Avatar Preview
                        if (photoPreviewBitmap != null || base64Photo.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                                    .border(1.dp, tealAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (photoPreviewBitmap != null) {
                                    Image(
                                        bitmap = photoPreviewBitmap!!.asImageBitmap(),
                                        contentDescription = "Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (base64Photo.startsWith("http")) {
                                    AsyncImage(
                                        model = base64Photo,
                                        contentDescription = "Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val decodedBmp = decodeBase64ToBitmap(base64Photo)
                                    if (decodedBmp != null) {
                                        Image(
                                            bitmap = decodedBmp.asImageBitmap(),
                                            contentDescription = "Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                // Delete selected image button
                                IconButton(
                                    onClick = {
                                        base64Photo = ""
                                        photoPreviewBitmap = null
                                    },
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.8f))
                                        .fillMaxSize()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Save Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cardUidClean = cardUid.trim()
                                val nameClean = name.trim()
                                val studentIdClean = studentId.trim()

                                if (cardUidClean.isBlank() || nameClean.isBlank() || studentIdClean.isBlank()) {
                                    Toast.makeText(context, "Vui lòng điền đầy đủ các thông tin!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (editingUid == null && students.any { it.uid.equals(cardUidClean, ignoreCase = true) }) {
                                    Toast.makeText(context, "Mã thẻ UID [$cardUidClean] đã gán cho sinh viên khác!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                val isDuplicateId = students.any { it.studentId == studentIdClean && it.uid != editingUid }
                                if (isDuplicateId) {
                                    Toast.makeText(context, "Mã sinh viên [$studentIdClean] đã được gán cho người khác!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                val isDuplicateName = students.any { it.name.equals(nameClean, ignoreCase = true) && it.uid != editingUid }
                                if (isDuplicateName) {
                                    Toast.makeText(context, "Tên sinh viên [$nameClean] đã tồn tại, hãy chỉnh sửa hoặc thêm ký hiệu!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                viewModel.addStudent(
                                    uid = cardUidClean,
                                    name = nameClean,
                                    studentId = studentIdClean,
                                    className = selectedClass.ifBlank { null },
                                    photoBase64 = base64Photo.ifBlank { null }
                                ) { success, error ->
                                    if (success) {
                                        Toast.makeText(context, "Lưu sinh viên thành công!", Toast.LENGTH_SHORT).show()
                                        resetForm()
                                    } else {
                                        Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tealAccent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1.3f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Lưu", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (editingUid != null) "Cập nhật" else "Lưu CSDL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (editingUid != null) {
                            Button(
                                onClick = { resetForm() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(0.7f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Hủy", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Student List Header
        item {
            Text(
                text = "DANH SÁCH SINH VIÊN (${students.size})",
                fontSize = 11.sp,
                color = textMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // Loaded list of registered students
        if (students.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có sinh viên nào đăng ký.", color = textMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(students) { student ->
                StudentListItem(
                    student = student,
                    isLight = isLight,
                    textMain = textMain,
                    textMuted = textMuted,
                    cardOutline = cardOutline,
                    tealAccent = tealAccent,
                    onEdit = {
                        cardUid = student.uid
                        name = student.name
                        studentId = student.studentId
                        selectedClass = student.className ?: ""
                        base64Photo = student.photo ?: ""
                        editingUid = student.uid
                        val isBase64 = student.photo?.startsWith("http") == false && student.photo?.isNotBlank() == true
                        photoPreviewBitmap = if (isBase64) decodeBase64ToBitmap(student.photo!!) else null
                    },
                    onDelete = {
                        viewModel.removeStudent(student.uid)
                    }
                )
            }
        }
    }
}

@Composable
fun StudentListItem(
    student: StudentEntity,
    isLight: Boolean,
    textMain: Color,
    textMuted: Color,
    cardOutline: Color,
    tealAccent: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xác nhận xóa", color = textMain) },
            text = { Text("Bạn có muốn xóa sinh viên ${student.name} (Card: ${student.uid})?", color = textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showConfirmDelete = false
                }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Hủy", color = textMain)
                }
            },
            containerColor = if (isLight) Color.White else Color(0xFF1F2937)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1F2937)),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder(cardOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                            Icon(Icons.Default.Person, contentDescription = student.name, tint = tealAccent)
                        }
                    }
                } else {
                    Icon(Icons.Default.Person, contentDescription = student.name, tint = tealAccent)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Horizontally scrollable row containing all text properties
            val detailScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(detailScrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = student.name,
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Mã số: ${student.studentId}",
                            color = textMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column {
                        Text(
                            text = "Mã thẻ UID",
                            color = textMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = student.uid,
                            color = tealAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    student.className?.let {
                        Column {
                            Text(
                                text = "Lớp học",
                                color = textMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = it,
                                color = textMain,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Edit and Delete controls
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa",
                        tint = textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassTabContent(
    viewModel: MainViewModel,
    classes: List<ClassEntity>,
    isLight: Boolean,
    dialogBg: Color,
    textMain: Color,
    textMuted: Color,
    cardOutline: Color,
    tealAccent: Color,
    onViewClass: (String) -> Unit
) {
    val context = LocalContext.current
    var classNameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add Class Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF9FAFB) else Color(0xFF111827)),
                shape = RoundedCornerShape(16.dp),
                border = BoxBorder(cardOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "THÊM LỚP HỌC MỚI",
                        fontSize = 11.sp,
                        color = tealAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = classNameInput,
                            onValueChange = { classNameInput = it.uppercase() },
                            label = { Text("Tên lớp hành chính (ví dụ: CNTT-01)", fontSize = 11.sp) },
                            colors = outlinedTextFieldColors(isLight, tealAccent, cardOutline),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val cleanName = classNameInput.trim()
                                if (cleanName.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập tên lớp!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (classes.any { it.className.equals(cleanName, ignoreCase = true) }) {
                                    Toast.makeText(context, "Lớp học [$cleanName] đã tồn tại!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.addClass(cleanName) { success, error ->
                                    if (success) {
                                        Toast.makeText(context, "Thêm lớp thành công!", Toast.LENGTH_SHORT).show()
                                        classNameInput = ""
                                    } else {
                                        Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tealAccent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Classes List Header
        item {
            Text(
                text = "DANH SÁCH LỚP HÀNH CHÍNH",
                fontSize = 11.sp,
                color = textMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // Dynamically load classrooms
        if (classes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có lớp nào được tạo.", color = textMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(classes) { cls ->
                ClassListItem(
                    className = cls.className,
                    isLight = isLight,
                    textMain = textMain,
                    textMuted = textMuted,
                    cardOutline = cardOutline,
                    onViewClick = { onViewClass(cls.className) },
                    onDelete = {
                        viewModel.removeClass(cls.className)
                    }
                )
            }
        }
    }
}

@Composable
fun ClassListItem(
    className: String,
    isLight: Boolean,
    textMain: Color,
    textMuted: Color,
    cardOutline: Color,
    onViewClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xác nhận xóa lớp", color = textMain) },
            text = { Text("Bạn muốn xóa lớp $className?\nCác sinh viên thuộc lớp này vẫn sẽ giữ nguyên tên lớp.", color = textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showConfirmDelete = false
                }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Hủy", color = textMain)
                }
            },
            containerColor = if (isLight) Color.White else Color(0xFF1F2937)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1F2937)),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder(cardOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = className,
                color = textMain,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onViewClick() }
                    .padding(vertical = 12.dp)
                    .weight(1f)
            )

            Row {
                IconButton(onClick = onViewClick) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Xem lớp", tint = textMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa lớp", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun BoxBorder(color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors(isLight: Boolean, accent: Color, outline: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = outline,
    focusedTextColor = if (isLight) Color.Black else Color.White,
    unfocusedTextColor = if (isLight) Color.Black else Color.White,
    focusedLabelColor = accent,
    unfocusedLabelColor = Color.Gray
)

// Helper function to compress large bitmaps into lightweight Base64 to scale database sizing
fun compressBitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    // Compress with high ratio
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
    val byteArray = outputStream.toByteArray()
    return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
