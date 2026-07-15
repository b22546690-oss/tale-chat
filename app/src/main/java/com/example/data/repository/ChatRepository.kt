package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {

    val allUsersFlow: Flow<List<UserEntity>> = chatDao.getAllUsersFlow()
    val allChatsFlow: Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()
    val groupsFlow: Flow<List<ChatEntity>> = chatDao.getGroupsFlow()
    val channelsFlow: Flow<List<ChatEntity>> = chatDao.getChannelsFlow()
    val privateChatsFlow: Flow<List<ChatEntity>> = chatDao.getPrivateChatsFlow()
    val currentUserFlow: Flow<UserEntity?> = chatDao.getCurrentUserFlow()

    suspend fun getAllUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        chatDao.getAllUsers()
    }

    suspend fun getUserByPhone(phone: String): UserEntity? = withContext(Dispatchers.IO) {
        chatDao.getUserByPhone(phone)
    }

    suspend fun getUserByUsername(username: String): UserEntity? = withContext(Dispatchers.IO) {
        chatDao.getUserByUsername(username)
    }

    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        chatDao.getCurrentUser()
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        chatDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        chatDao.updateUser(user)
    }

    suspend fun getChatById(chatId: String): ChatEntity? = withContext(Dispatchers.IO) {
        chatDao.getChatById(chatId)
    }

    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>> {
        return chatDao.getMessagesForChatFlow(chatId)
    }

    suspend fun markMessagesAsRead(chatId: String, currentUserPhone: String) = withContext(Dispatchers.IO) {
        chatDao.markMessagesAsRead(chatId, currentUserPhone)
    }

    suspend fun createPrivateChat(myPhone: String, myName: String, otherUserPhone: String): ChatEntity = withContext(Dispatchers.IO) {
        val otherUser = chatDao.getUserByPhone(otherUserPhone) ?: throw IllegalArgumentException("User not found")
        
        // Deterministic private chat ID to avoid duplicates
        val chatParts = listOf(myPhone, otherUserPhone).sorted()
        val chatId = "private_${chatParts[0]}_${chatParts[1]}"
        
        val existingChat = chatDao.getChatById(chatId)
        if (existingChat != null) {
            return@withContext existingChat
        }

        val newChat = ChatEntity(
            id = chatId,
            name = otherUser.fullName,
            isGroup = false,
            isChannel = false,
            description = otherUser.bio,
            creatorPhone = myPhone,
            avatarUrl = otherUser.avatarUrl,
            lastMessageText = "Chat started",
            lastMessageTime = System.currentTimeMillis()
        )
        chatDao.insertChat(newChat)
        
        // Add chat members
        chatDao.insertMember(ChatMemberEntity(chatId = chatId, userPhone = myPhone, isAdmin = true))
        chatDao.insertMember(ChatMemberEntity(chatId = chatId, userPhone = otherUserPhone, isAdmin = false))
        
        newChat
    }

    suspend fun createGroup(name: String, description: String?, creatorPhone: String, creatorName: String, memberPhones: List<String>): ChatEntity = withContext(Dispatchers.IO) {
        val chatId = "group_${UUID.randomUUID()}"
        val newChat = ChatEntity(
            id = chatId,
            name = name,
            isGroup = true,
            isChannel = false,
            description = description,
            creatorPhone = creatorPhone,
            avatarUrl = null,
            lastMessageText = "$creatorName created the group",
            lastMessageTime = System.currentTimeMillis()
        )
        chatDao.insertChat(newChat)

        // Insert members
        chatDao.insertMember(ChatMemberEntity(chatId = chatId, userPhone = creatorPhone, isAdmin = true))
        for (phone in memberPhones) {
            chatDao.insertMember(ChatMemberEntity(chatId = chatId, userPhone = phone, isAdmin = false))
        }

        // Send a system message
        val systemMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderPhone = creatorPhone,
            senderName = creatorName,
            text = "Welcome to the new group '$name'!",
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        chatDao.insertMessage(systemMessage)

        newChat
    }

    suspend fun createChannel(name: String, description: String?, creatorPhone: String, creatorName: String, isPublic: Boolean): ChatEntity = withContext(Dispatchers.IO) {
        val chatId = "channel_${UUID.randomUUID()}"
        val newChat = ChatEntity(
            id = chatId,
            name = name,
            isGroup = false,
            isChannel = true,
            description = description,
            creatorPhone = creatorPhone,
            avatarUrl = null,
            lastMessageText = "Channel created",
            lastMessageTime = System.currentTimeMillis(),
            isPublic = isPublic
        )
        chatDao.insertChat(newChat)

        chatDao.insertMember(ChatMemberEntity(chatId = chatId, userPhone = creatorPhone, isAdmin = true))

        val welcomeMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderPhone = creatorPhone,
            senderName = creatorName,
            text = "Channel created: $name\n$description",
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(welcomeMsg)

        newChat
    }

    suspend fun sendMessage(
        chatId: String,
        senderPhone: String,
        senderName: String,
        text: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        voiceDurationSec: Int = 0
    ): MessageEntity = withContext(Dispatchers.IO) {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderPhone = senderPhone,
            senderName = senderName,
            text = text,
            mediaUri = mediaUri,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            voiceDurationSec = voiceDurationSec
        )
        chatDao.insertMessage(message)

        // Update chat's last message info
        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            val updatedChat = chat.copy(
                lastMessageText = if (mediaType != null) "[$mediaType] $text" else text,
                lastMessageTime = message.timestamp
            )
            chatDao.updateChat(updatedChat)
        }

        // Simulate support response
        if (chatId == "private_support" && senderPhone != "support") {
            simulateSupportResponse(chatId, text)
        }

        message
    }

    private suspend fun simulateSupportResponse(chatId: String, text: String) {
        // Simple support bot response
        val replyText = when {
            text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) -> {
                "Hi there! TeleChat support is fully operational. How can I assist you today?"
            }
            text.contains("group", ignoreCase = true) || text.contains("channel", ignoreCase = true) -> {
                "To create a Group or Channel, tap the edit floating action button in the bottom-right corner of the Dashboard. You can invite your contacts and publish posts instantly!"
            }
            text.contains("profile", ignoreCase = true) || text.contains("settings", ignoreCase = true) -> {
                "You can edit your Profile details, change your unique username, adjust privacy configurations, or switch themes directly in the Settings tab."
            }
            text.contains("voice", ignoreCase = true) || text.contains("audio", ignoreCase = true) -> {
                "We support voice messages! Tap and hold the Mic icon in any chat, or tap it once to send a mock voice message for testing."
            }
            text.contains("image", ignoreCase = true) || text.contains("photo", ignoreCase = true) -> {
                "You can send images in private chats or groups by clicking the Attachment (paperclip) icon."
            }
            else -> {
                "That's awesome! TeleChat is fully reactive, keeping your chats synchronized in real-time. Type 'group' or 'settings' to see helper guides!"
            }
        }

        // Introduce a small typing delay to simulate realism
        kotlinx.coroutines.delay(1000)

        val botMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderPhone = "support",
            senderName = "Telegram Support",
            text = replyText,
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        chatDao.insertMessage(botMessage)

        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            val updatedChat = chat.copy(
                lastMessageText = replyText,
                lastMessageTime = botMessage.timestamp
            )
            chatDao.updateChat(updatedChat)
        }
    }

    suspend fun seedMockData() = withContext(Dispatchers.IO) {
        if (chatDao.getAllUsers().isNotEmpty()) return@withContext

        // Seed users
        val durov = UserEntity(
            phone = "+12345678",
            email = "durov@telegram.org",
            fullName = "Pavel Durov",
            username = "durov",
            bio = "Freedom is not negotiable.",
            avatarUrl = null,
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )
        val alice = UserEntity(
            phone = "+987654321",
            email = "alice@kotlin.org",
            fullName = "Alice Johnson",
            username = "alice_j",
            bio = "Kotlin enthusiast | Jetpack Compose wizard ⚡",
            avatarUrl = null,
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )
        val bob = UserEntity(
            phone = "+111222333",
            email = "bob@uxdesign.com",
            fullName = "Bob Smith",
            username = "bob_smith",
            bio = "UX designer | Material 3 believer ✨",
            avatarUrl = null,
            isOnline = false,
            lastSeen = System.currentTimeMillis() - 3600000
        )
        val support = UserEntity(
            phone = "support",
            email = "support@telechat.org",
            fullName = "Telegram Support",
            username = "support",
            bio = "Official support channel for TeleChat users.",
            avatarUrl = null,
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )

        chatDao.insertUser(durov)
        chatDao.insertUser(alice)
        chatDao.insertUser(bob)
        chatDao.insertUser(support)

        // Seed support chat
        val supportChat = ChatEntity(
            id = "private_support",
            name = "Telegram Support",
            isGroup = false,
            isChannel = false,
            description = "Official support channel for TeleChat users.",
            creatorPhone = "support",
            avatarUrl = null,
            lastMessageText = "Welcome to TeleChat Support!",
            lastMessageTime = System.currentTimeMillis()
        )
        chatDao.insertChat(supportChat)
        chatDao.insertMember(ChatMemberEntity(chatId = "private_support", userPhone = "support", isAdmin = true))

        val welcomeSupportMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = "private_support",
            senderPhone = "support",
            senderName = "Telegram Support",
            text = "Welcome to TeleChat! 🚀\n\nThis is a modern, responsive Telegram-inspired application. You can explore: \n• Private chats with smart automated replies\n• Beautiful gradients, custom gestures, dark mode\n• Creating Group Chats and Public Channels\n• Attachment and simulated Voice Message sharing\n\nHow can I help you today?",
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis() - 50000
        )
        chatDao.insertMessage(welcomeSupportMsg)

        // Seed kotlin developers group
        val groupChat = ChatEntity(
            id = "group_kotlin",
            name = "Compose Developers",
            isGroup = true,
            isChannel = false,
            description = "A group for active Jetpack Compose and Kotlin developers.",
            creatorPhone = "+987654321",
            avatarUrl = null,
            lastMessageText = "Bob Smith: Agreed, performance is amazing!",
            lastMessageTime = System.currentTimeMillis()
        )
        chatDao.insertChat(groupChat)
        chatDao.insertMember(ChatMemberEntity(chatId = "group_kotlin", userPhone = "+987654321", isAdmin = true))
        chatDao.insertMember(ChatMemberEntity(chatId = "group_kotlin", userPhone = "+111222333", isAdmin = false))

        val gMsg1 = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = "group_kotlin",
            senderPhone = "+987654321",
            senderName = "Alice Johnson",
            text = "Hey everyone! Jetpack Compose 1.7's compilation speeds are absolutely stellar.",
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis() - 100000
        )
        val gMsg2 = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = "group_kotlin",
            senderPhone = "+111222333",
            senderName = "Bob Smith",
            text = "Agreed, performance is amazing! The layout measurement overhead feels practically zero now.",
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis() - 40000
        )
        chatDao.insertMessage(gMsg1)
        chatDao.insertMessage(gMsg2)

        // Seed tech news channel
        val channelChat = ChatEntity(
            id = "channel_tech",
            name = "Tech News & Updates",
            isGroup = false,
            isChannel = true,
            description = "Official channel publishing tech news and Android announcements.",
            creatorPhone = "+12345678",
            avatarUrl = null,
            lastMessageText = "TeleChat has been compiled successfully!",
            lastMessageTime = System.currentTimeMillis()
        )
        chatDao.insertChat(channelChat)
        chatDao.insertMember(ChatMemberEntity(chatId = "channel_tech", userPhone = "+12345678", isAdmin = true))

        val cMsg1 = MessageEntity(
            id = "pinned_msg_tech",
            chatId = "channel_tech",
            senderPhone = "+12345678",
            senderName = "Pavel Durov",
            text = "📍 PINNED: TeleChat builds complete local database caching and reactive UI streams natively. This ensures instant performance on foldable screens, tablets, and phones.",
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis() - 200000
        )
        val cMsg2 = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = "channel_tech",
            senderPhone = "+12345678",
            senderName = "Pavel Durov",
            text = "We have successfully built and optimized the Telegram-style responsive layout for mobile and tablet devices. Enjoy seamless communication! 🚀",
            mediaUri = null,
            mediaType = "TEXT",
            timestamp = System.currentTimeMillis() - 10000
        )
        chatDao.insertMessage(cMsg1)
        chatDao.insertMessage(cMsg2)

        // Mark first message as pinned
        val updatedChannel = channelChat.copy(pinnedMessageId = "pinned_msg_tech")
        chatDao.updateChat(updatedChannel)
    }
}
