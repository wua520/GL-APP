package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_messages")
data class AiChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val content: String,
    val isUser: Boolean,  // true=用户消息, false=AI回复
    val timestamp: Long = System.currentTimeMillis()
)
