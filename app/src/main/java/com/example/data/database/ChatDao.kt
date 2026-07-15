package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isMe = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isMe = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Chats
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC, id DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isGroup = 1 ORDER BY lastMessageTime DESC, id DESC")
    fun getGroupsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isChannel = 1 ORDER BY lastMessageTime DESC, id DESC")
    fun getChannelsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isGroup = 0 AND isChannel = 0 ORDER BY lastMessageTime DESC, id DESC")
    fun getPrivateChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    // Messages
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE chatId = :chatId AND senderPhone != :currentUserPhone")
    suspend fun markMessagesAsRead(chatId: String, currentUserPhone: String)

    // Members
    @Query("SELECT * FROM chat_members WHERE chatId = :chatId")
    suspend fun getMembersForChat(chatId: String): List<ChatMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ChatMemberEntity)

    @Query("DELETE FROM chat_members WHERE chatId = :chatId AND userPhone = :phone")
    suspend fun removeMember(chatId: String, phone: String)
}
