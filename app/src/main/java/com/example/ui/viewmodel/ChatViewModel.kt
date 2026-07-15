package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import com.example.data.database.UserEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthState {
    object PhoneInput : AuthState
    object EmailInput : AuthState
    object NameInput : AuthState
    object Authenticated : AuthState
}

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.PhoneInput)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allChats: StateFlow<List<ChatEntity>> = repository.allChatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<ChatEntity>> = repository.groupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<ChatEntity>> = repository.channelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privateChats: StateFlow<List<ChatEntity>> = repository.privateChatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query State
    val searchQuery = MutableStateFlow("")

    // Filtered chats based on search
    val filteredChats: StateFlow<List<ChatEntity>> = combine(allChats, searchQuery) { chats, query ->
        if (query.isBlank()) chats
        else chats.filter { it.name.contains(query, ignoreCase = true) || (it.description?.contains(query, ignoreCase = true) == true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered users based on search
    val filteredUsers: StateFlow<List<UserEntity>> = combine(allUsers, searchQuery) { users, query ->
        val me = currentUser.value
        val filtered = if (query.isBlank()) users
        else users.filter { it.fullName.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) }
        
        // Exclude current user from search results
        filtered.filter { it.phone != me?.phone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    val isDarkMode = MutableStateFlow(true) // Modern Telegram dark theme by default
    val privacySettingsEnabled = MutableStateFlow(true)
    val notificationsEnabled = MutableStateFlow(true)

    // Temporary values during onboarding / registration flow
    var tempPhone = ""
    var tempEmail = ""
    var tempName = ""

    init {
        viewModelScope.launch {
            repository.seedMockData()
            // Check if user is already authenticated
            val me = repository.getCurrentUser()
            if (me != null) {
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.PhoneInput
            }
        }
    }

    // AUTH ACTIONS
    fun loginWithPhone(phone: String) {
        viewModelScope.launch {
            tempPhone = phone
            val existingUser = repository.getUserByPhone(phone)
            if (existingUser != null) {
                // User already exists, log in immediately
                val loggedInUser = existingUser.copy(isMe = true, isOnline = true, lastSeen = System.currentTimeMillis())
                repository.insertUser(loggedInUser)
                _authState.value = AuthState.Authenticated
            } else {
                // User not found, proceed to registration flow
                _authState.value = AuthState.EmailInput
            }
        }
    }

    fun submitEmail(email: String) {
        tempEmail = email
        _authState.value = AuthState.NameInput
    }

    fun submitName(fullName: String) {
        viewModelScope.launch {
            tempName = fullName
            // Generate a simple unique username
            val baseUsername = fullName.lowercase().replace(" ", "_")
            var uniqueUsername = baseUsername
            var counter = 1
            while (repository.getUserByUsername(uniqueUsername) != null) {
                uniqueUsername = "${baseUsername}_$counter"
                counter++
            }

            // Create user
            val newUser = UserEntity(
                phone = tempPhone,
                email = tempEmail,
                fullName = fullName,
                username = uniqueUsername,
                bio = "Hey there! I am using TeleChat.",
                avatarUrl = null,
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isMe = true
            )
            repository.insertUser(newUser)

            // Seed user with default Support Chat
            repository.createPrivateChat(tempPhone, fullName, "support")

            _authState.value = AuthState.Authenticated
        }
    }

    fun logout() {
        viewModelScope.launch {
            val me = repository.getCurrentUser()
            if (me != null) {
                val loggedOut = me.copy(isMe = false, isOnline = false, lastSeen = System.currentTimeMillis())
                repository.insertUser(loggedOut)
            }
            tempPhone = ""
            tempEmail = ""
            tempName = ""
            _authState.value = AuthState.PhoneInput
        }
    }

    // PROFILE ACTIONS
    fun updateProfile(fullName: String, username: String, bio: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            if (fullName.isBlank()) {
                onError("Full Name cannot be empty")
                return@launch
            }
            if (username.isBlank()) {
                onError("Username cannot be empty")
                return@launch
            }

            // Validate unique username if it changed
            if (username != me.username) {
                val existing = repository.getUserByUsername(username)
                if (existing != null) {
                    onError("Username already taken")
                    return@launch
                }
            }

            val updated = me.copy(fullName = fullName, username = username, bio = bio)
            repository.insertUser(updated)
            onSuccess()
        }
    }

    // CHAT ACTIONS
    suspend fun getChatById(chatId: String): ChatEntity? = repository.getChatById(chatId)

    fun startPrivateChat(otherUserPhone: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val chat = repository.createPrivateChat(me.phone, me.fullName, otherUserPhone)
            onComplete(chat.id)
        }
    }

    fun createGroupChat(name: String, description: String?, memberPhones: List<String>, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val chat = repository.createGroup(name, description, me.phone, me.fullName, memberPhones)
            onComplete(chat.id)
        }
    }

    fun createChannelChat(name: String, description: String?, isPublic: Boolean, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val chat = repository.createChannel(name, description, me.phone, me.fullName, isPublic)
            onComplete(chat.id)
        }
    }

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return repository.getMessagesForChatFlow(chatId)
    }

    fun sendMessage(chatId: String, text: String, mediaUri: String? = null, mediaType: String? = null, voiceDurationSec: Int = 0) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            repository.sendMessage(
                chatId = chatId,
                senderPhone = me.phone,
                senderName = me.fullName,
                text = text,
                mediaUri = mediaUri,
                mediaType = mediaType,
                voiceDurationSec = voiceDurationSec
            )
        }
    }

    fun markAsRead(chatId: String) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            repository.markMessagesAsRead(chatId, me.phone)
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
