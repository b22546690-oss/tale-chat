package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChatEntity
import com.example.data.database.UserEntity
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ReadCheckBlue
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramLightBlue
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Deterministic colors for beautiful avatar placeholders
fun getAvatarGradient(name: String): Brush {
    val hash = name.hashCode()
    val colors = listOf(
        listOf(Color(0xFFE57373), Color(0xFFE53935)), // Red
        listOf(Color(0xFF81C784), Color(0xFF43A047)), // Green
        listOf(Color(0xFF64B5F6), Color(0xFF1E88E5)), // Blue
        listOf(Color(0xFFFFD54F), Color(0xFFFFB300)), // Amber
        listOf(Color(0xFFBA68C8), Color(0xFF8E24AA)), // Purple
        listOf(Color(0xFF4DB6AC), Color(0xFF00897B)), // Teal
        listOf(Color(0xFFFF8A65), Color(0xFFF4511E))  // Orange
    )
    val chosenIndex = Math.abs(hash) % colors.size
    return Brush.linearGradient(colors[chosenIndex])
}

@Composable
fun AvatarPlaceholder(name: String, size: Int = 50, isOnline: Boolean = false) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
    
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(getAvatarGradient(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (size * 0.35).sp
            )
        )

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size * 0.28).dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.BottomEnd)
                    .padding((size * 0.05).dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(OnlineGreen)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val currentUser by viewModel.currentUser.collectAsState()
    
    val isDark by viewModel.isDarkMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showCreateChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                0 -> "TeleChat"
                                1 -> "Chats"
                                2 -> "Groups"
                                3 -> "Channels"
                                4 -> "Profile"
                                else -> "Settings"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        if (selectedTab < 4) {
                            IconButton(onClick = { showCreateChatDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search users and conversations"
                                )
                            }
                        }
                    }
                )
                
                // Unified Search Bar inside tabs 0, 1, 2, 3
                if (selectedTab < 4) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search chats, groups, channels...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                val items = listOf(
                    Triple(0, "Home", Icons.Default.Home),
                    Triple(1, "Chats", Icons.Default.Chat),
                    Triple(2, "Groups", Icons.Default.Group),
                    Triple(3, "Channels", Icons.Default.Campaign),
                    Triple(4, "Profile", Icons.Default.Person),
                    Triple(5, "Settings", Icons.Default.Settings)
                )
                
                items.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab < 4) {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            2 -> showCreateGroupDialog = true
                            3 -> showCreateChannelDialog = true
                            else -> showCreateChatDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("action_fab")
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            2 -> Icons.Default.GroupAdd
                            3 -> Icons.Default.Campaign
                            else -> Icons.Default.Edit
                        },
                        contentDescription = "New Action"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> ChatsListTab(viewModel, onNavigateToChat, "All Chats")
                1 -> ChatsListTab(viewModel, onNavigateToChat, "Private Chats")
                2 -> ChatsListTab(viewModel, onNavigateToChat, "Groups")
                3 -> ChatsListTab(viewModel, onNavigateToChat, "Channels")
                4 -> ProfileTab(currentUser, onEditProfile = { showEditProfileDialog = true })
                5 -> SettingsTab(viewModel, onEditProfile = { showEditProfileDialog = true })
            }
        }
    }

    // CREATE CHAT DIALOG
    if (showCreateChatDialog) {
        CreateChatDialog(
            viewModel = viewModel,
            onDismiss = { showCreateChatDialog = false },
            onStartChat = { chatId ->
                showCreateChatDialog = false
                onNavigateToChat(chatId)
            }
        )
    }

    // CREATE GROUP DIALOG
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            viewModel = viewModel,
            onDismiss = { showCreateGroupDialog = false },
            onGroupCreated = { chatId ->
                showCreateGroupDialog = false
                onNavigateToChat(chatId)
            }
        )
    }

    // CREATE CHANNEL DIALOG
    if (showCreateChannelDialog) {
        CreateChannelDialog(
            viewModel = viewModel,
            onDismiss = { showCreateChannelDialog = false },
            onChannelCreated = { chatId ->
                showCreateChannelDialog = false
                onNavigateToChat(chatId)
            }
        )
    }

    // EDIT PROFILE DIALOG
    if (showEditProfileDialog) {
        EditProfileDialog(
            viewModel = viewModel,
            onDismiss = { showEditProfileDialog = false }
        )
    }
}

@Composable
fun ChatsListTab(
    viewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit,
    tabType: String
) {
    val chats by when (tabType) {
        "Private Chats" -> viewModel.privateChats.collectAsState()
        "Groups" -> viewModel.groups.collectAsState()
        "Channels" -> viewModel.channels.collectAsState()
        else -> viewModel.filteredChats.collectAsState() // Unified search filters everything
    }

    if (chats.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (tabType) {
                    "Groups" -> Icons.Default.Group
                    "Channels" -> Icons.Default.Campaign
                    else -> Icons.Default.Forum
                },
                contentDescription = "Empty",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No $tabType found",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the button in the bottom right corner to start chatting!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(chats) { chat ->
                ChatItemRow(chat = chat, onClick = { onNavigateToChat(chat.id) })
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 82.dp)
                )
            }
        }
    }
}

@Composable
fun ChatItemRow(
    chat: ChatEntity,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Placeholder with deterministic coloring
        AvatarPlaceholder(name = chat.name, size = 52)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isGroup) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group",
                            modifier = Modifier.size(16.dp),
                            tint = TelegramBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (chat.isChannel) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Channel",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (chat.lastMessageTime != null) timeFormat.format(Date(chat.lastMessageTime)) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessageText ?: "No messages",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Render tick marks for read receipts
                Row {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        modifier = Modifier.size(16.dp),
                        tint = ReadCheckBlue
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileTab(
    user: UserEntity?,
    onEditProfile: () -> Unit
) {
    if (user == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Large Premium Avatar with online status
        AvatarPlaceholder(name = user.fullName, size = 110, isOnline = true)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.fullName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Cards Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileInfoRow(label = "Phone Number", value = user.phone, icon = Icons.Default.Phone)
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ProfileInfoRow(label = "Email Address", value = user.email, icon = Icons.Default.Email)
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ProfileInfoRow(label = "Bio", value = user.bio, icon = Icons.Default.Info)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onEditProfile,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile Details")
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: ChatViewModel,
    onEditProfile: () -> Unit
) {
    val isDark by viewModel.isDarkMode.collectAsState()
    val privacyEnabled by viewModel.privacySettingsEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Dark Theme", fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.isDarkMode.value = it }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Notifications Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Push Notifications", fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.notificationsEnabled.value = it }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Privacy Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Privacy Caching", fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = privacyEnabled,
                        onCheckedChange = { viewModel.privacySettingsEnabled.value = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Account Options
        Text(
            text = "Account Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditProfile)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Edit Profile Details", fontWeight = FontWeight.Medium)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.logout() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Logout from TeleChat", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// DIALOGS SECTION
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onStartChat: (String) -> Unit
) {
    val users by viewModel.filteredUsers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Private Chat", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                Text(
                    "Search user by username or full name:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                if (users.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = "Search", modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No other users found", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(users) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.startPrivateChat(user.phone, onStartChat) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarPlaceholder(name = user.fullName, size = 42, isOnline = user.isOnline)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(user.fullName, fontWeight = FontWeight.Bold)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val users by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val selectedUsers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group Chat", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Group Members:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(8.dp))

                users.filter { it.phone != currentUser?.phone }.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedUsers.contains(user.phone)) selectedUsers.remove(user.phone)
                                else selectedUsers.add(user.phone)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedUsers.contains(user.phone),
                            onCheckedChange = {
                                if (it == true) selectedUsers.add(user.phone)
                                else selectedUsers.remove(user.phone)
                            }
                        )
                        AvatarPlaceholder(name = user.fullName, size = 36)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(user.fullName, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        viewModel.createGroupChat(groupName, description, selectedUsers.toList(), onGroupCreated)
                    }
                },
                enabled = groupName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onChannelCreated: (String) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Public Channel", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Channel Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Public Channel", fontWeight = FontWeight.Medium)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (channelName.isNotBlank()) {
                        viewModel.createChannelChat(channelName, description, isPublic, onChannelCreated)
                    }
                },
                enabled = channelName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var nameInput by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var usernameInput by remember { mutableStateOf(currentUser?.username ?: "") }
    var bioInput by remember { mutableStateOf(currentUser?.bio ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Details", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; errorMsg = null },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' }; errorMsg = null },
                    label = { Text("Username") },
                    singleLine = true,
                    prefix = { Text("@") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    label = { Text("Bio") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateProfile(
                        fullName = nameInput,
                        username = usernameInput,
                        bio = bioInput,
                        onSuccess = onDismiss,
                        onError = { errorMsg = it }
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
