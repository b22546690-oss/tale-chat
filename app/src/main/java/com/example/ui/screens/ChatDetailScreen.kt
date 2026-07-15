package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ReadCheckBlue
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var chat by remember { mutableStateOf<ChatEntity?>(null) }
    val messages by viewModel.getMessagesForChat(chatId).collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }

    // Fetch chat details
    LaunchedEffect(chatId) {
        chat = viewModel.getChatById(chatId)
        viewModel.markAsRead(chatId)
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (chat == null) {
        Scaffold { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val currentChat = chat!!

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarPlaceholder(name = currentChat.name, size = 40)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentChat.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    currentChat.isGroup -> "5 members, 2 online"
                                    currentChat.isChannel -> "1,240 subscribers"
                                    isTyping -> "typing..."
                                    else -> "Online"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (currentChat.isChannel || currentChat.isGroup) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                // Simulate pinning latest message or a generic message
                                val pinnedMsg = messages.lastOrNull()
                                if (pinnedMsg != null) {
                                    chat = currentChat.copy(pinnedMessageId = pinnedMsg.id)
                                    viewModel.createChannelChat(currentChat.name, currentChat.description, currentChat.isPublic) { }
                                }
                            }
                        }) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pin message")
                        }
                    }
                    IconButton(onClick = { /* Extra Actions */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // PINNED MESSAGE BANNER
                if (currentChat.pinnedMessageId != null) {
                    val pinnedMessage = messages.find { it.id == currentChat.pinnedMessageId }
                    if (pinnedMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                                .clickable {
                                    val index = messages.indexOf(pinnedMessage)
                                    if (index != -1) {
                                        coroutineScope.launch { listState.animateScrollToItem(index) }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Pinned Message",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    pinnedMessage.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // MESSAGES LIST
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(messages) { index, msg ->
                        MessageBubble(
                            message = msg,
                            isMe = msg.senderPhone == currentUser?.phone,
                            showSenderName = currentChat.isGroup && msg.senderPhone != currentUser?.phone
                        )
                    }
                }

                // BOTTOM TEXT INPUT BAR
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach file",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Input text field with round design
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = {
                                    messageText = it
                                    // Set typing status briefly
                                    if (it.isNotEmpty() && !isTyping) {
                                        isTyping = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            isTyping = false
                                        }
                                    }
                                },
                                placeholder = { Text("Message") },
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_text_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        messageText += " 🚀"
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEmotions,
                                            contentDescription = "Add emoji",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // Smart Send / Voice Action Button
                            IconButton(
                                onClick = {
                                    if (messageText.trim().isNotEmpty()) {
                                        viewModel.sendMessage(chatId, messageText.trim())
                                        messageText = ""
                                    } else {
                                        // Send a realistic mock voice message for testing
                                        viewModel.sendMessage(
                                            chatId = chatId,
                                            text = "Voice message (0:12)",
                                            mediaType = "VOICE",
                                            voiceDurationSec = 12
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (messageText.trim().isNotEmpty()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        // Sliding attachment menu
                        AnimatedVisibility(
                            visible = showAttachmentMenu,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                AttachmentOption(
                                    icon = Icons.Default.Image,
                                    label = "Photo",
                                    color = Color(0xFF4CAF50),
                                    onClick = {
                                        viewModel.sendMessage(
                                            chatId = chatId,
                                            text = "Check out this beautiful photo!",
                                            mediaUri = "https://picsum.photos/400/300",
                                            mediaType = "IMAGE"
                                        )
                                        showAttachmentMenu = false
                                    }
                                )
                                AttachmentOption(
                                    icon = Icons.Default.InsertDriveFile,
                                    label = "Document",
                                    color = Color(0xFF2196F3),
                                    onClick = {
                                        viewModel.sendMessage(
                                            chatId = chatId,
                                            text = "Project_Specs_Draft.pdf (2.4 MB)",
                                            mediaType = "FILE"
                                        )
                                        showAttachmentMenu = false
                                    }
                                )
                                AttachmentOption(
                                    icon = Icons.Default.LocationOn,
                                    label = "Location",
                                    color = Color(0xFFFF5722),
                                    onClick = {
                                        viewModel.sendMessage(
                                            chatId = chatId,
                                            text = "📍 Live Location Shared: 37.7749° N, 122.4194° W"
                                        )
                                        showAttachmentMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    showSenderName: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .testTag(if (isMe) "msg_me" else "msg_other")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // SENDER NAME (Inside group chats)
                if (showSenderName) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TelegramBlue,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // IMAGE ATTACHMENT
                if (message.mediaType == "IMAGE" && message.mediaUri != null) {
                    AsyncImage(
                        model = message.mediaUri,
                        contentDescription = "Shared Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // FILE ATTACHMENT
                if (message.mediaType == "FILE") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.text.substringBefore(" "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = message.text.substringAfter(" "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // VOICE MESSAGE PLAYER
                if (message.mediaType == "VOICE") {
                    var isPlaying by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play voice message",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Simulated voice wave visualizer
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val waves = listOf(14, 24, 18, 30, 22, 12, 18, 28, 14, 20)
                            waves.forEach { height ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(height.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (isPlaying) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "0:${message.voiceDurationSec}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (message.mediaType != "IMAGE" && message.mediaType != "FILE") {
                    // REGULAR TEXT MESSAGE
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // TIMESTAMP AND READ RECEIPTS
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read status",
                            tint = ReadCheckBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
