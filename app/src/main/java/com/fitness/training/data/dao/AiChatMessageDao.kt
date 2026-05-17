package com.fitness.training.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitness.training.data.entity.AiChatMessage

@Dao
interface AiChatMessageDao {
    
    @Insert
    suspend fun insert(message: AiChatMessage): Long
    
    @Query("SELECT * FROM ai_chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getAllMessages(userId: Long): List<AiChatMessage>
    
    @Query("DELETE FROM ai_chat_messages WHERE userId = :userId")
    suspend fun deleteAllMessages(userId: Long)
    
    @Query("DELETE FROM ai_chat_messages")
    suspend fun deleteAll()
}
