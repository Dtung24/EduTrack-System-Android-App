package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatPanel(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isChatOpen by viewModel.isChatOpen.collectAsState()
    val isLight by viewModel.isLightMode.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val tealAccent = Color(0xFF14B8A6)
    val cardBg = if (isLight) Color.White else Color(0xFF1F2937)
    val appBg = if (isLight) Color(0xFFF3F4F6) else Color(0xFF111827)
    val textMain = if (isLight) Color(0xFF111827) else Color.White
    val textMuted = if (isLight) Color(0xFF4B5563) else Color(0xFF9CA3AF)
    val outlineColor = if (isLight) Color(0xFFD1D5DB) else Color(0xFF374151)

    AnimatedVisibility(
        visible = isChatOpen,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 360.dp)
            .fillMaxWidth(0.9f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, outlineColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .border(width = 0.dp, color = Color.Transparent)
                        .border(width = 1.dp, color = outlineColor, shape = RoundedCornerShape(topStart = 14.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Icon",
                            tint = tealAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EduTrack AI Assistant",
                            color = tealAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { viewModel.setChatOpen(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textMuted)
                    }
                }

                Divider(color = outlineColor)

                // Message Thread list
                val listState = rememberLazyListState()
                LaunchedEffect(messages.size, isChatLoading) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { msg ->
                            BubbleItem(msg = msg, isLight = isLight, tealAccent = tealAccent, textMain = textMain)
                        }

                        if (isChatLoading) {
                            item {
                                TypingIndicator(isLight = isLight, tealAccent = tealAccent)
                            }
                        }
                    }
                }

                Divider(color = outlineColor)

                // Message Input Row
                var inputText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Hỏi AI về lịch sử quẹt thẻ...", fontSize = 12.sp, color = textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tealAccent,
                            unfocusedBorderColor = outlineColor,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            if (inputText.isNotBlank()) {
                                IconButton(onClick = {
                                    viewModel.sendChatMessage(inputText)
                                    inputText = ""
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = "Gửi", tint = tealAccent)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BubbleItem(
    msg: ChatMessage,
    isLight: Boolean,
    tealAccent: Color,
    textMain: Color
) {
    val isBot = msg.sender == "bot"
    val alignment = if (isBot) Alignment.Start else Alignment.End

    val bubbleBg = if (isBot) {
        if (isLight) Color(0xFFF3F4F6) else Color(0xFF374151) // Gray-700
    } else {
        tealAccent
    }

    val textColor = if (isBot) textMain else Color.White

    val shape = if (isBot) {
        RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isBot) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleBg)
                .padding(10.dp)
        ) {
            if (isBot) {
                // Draw formatted helper
                MarkdownText(text = msg.text, textColor = textColor)
            } else {
                Text(text = msg.text, color = textColor, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, textColor: Color) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = Color(0xFF14B8A6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = formatBoldText(trimmedLine.substring(2)),
                            color = textColor,
                            fontSize = 13.sp
                        )
                    }
                }
                trimmedLine.startsWith("###") -> {
                    Text(
                        text = formatBoldText(trimmedLine.substring(3).trim()),
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("##") -> {
                    Text(
                        text = formatBoldText(trimmedLine.substring(2).trim()),
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                else -> {
                    Text(
                        text = formatBoldText(line),
                        color = textColor,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// Formats **bold** string segments dynamically inside buildAnnotatedString
fun formatBoldText(raw: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < raw.length) {
            val starIdx = raw.indexOf("**", cursor)
            if (starIdx == -1) {
                append(raw.substring(cursor))
                break
            }
            append(raw.substring(cursor, starIdx))
            val endStarIdx = raw.indexOf("**", starIdx + 2)
            if (endStarIdx == -1) {
                append(raw.substring(starIdx))
                break
            }
            // Appends highlighted sections
            val boldSegment = raw.substring(starIdx + 2, endStarIdx)
            val startMark = length
            append(boldSegment)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF2DD4BF)), startMark, length)
            cursor = endStarIdx + 2
        }
    }
}

@Composable
fun TypingIndicator(
    isLight: Boolean,
    tealAccent: Color
) {
    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF3F4F6) else Color(0xFF374151)),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CircularProgressIndicator(
                color = tealAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(12.dp)
            )
            Text(
                "AI đang phân tích...",
                color = if (isLight) Color.DarkGray else Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
