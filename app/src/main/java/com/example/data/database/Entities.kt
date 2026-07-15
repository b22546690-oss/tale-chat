package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val phone: String,
    val email: String,
    val fullName: String,
    val username: String,
    val bio: String,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val lastSeen: Long,
    val isMe: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isGroup: Boolean,
    val isChannel: Boolean,
    val description: String?,
    val creatorPhone: String,
    val avatarUrl: String?,
    val lastMessageText: String?,
    val lastMessageTime: Long?,
    val pinnedMessageId: String? = null,
    val isPublic: Boolean = true
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderPhone: String,
    val senderName: String,
    val text: String,
    val mediaUri: String?,
    val mediaType: String?, // "IMAGE", "FILE", "VOICE"
    val timestamp: Long,
    val isRead: Boolean = false,
    val voiceDurationSec: Int = 0
)

@Entity(tableName = "chat_members")
data class ChatMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val userPhone: String,
    val isAdmin: Boolean = false
)
