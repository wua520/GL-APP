package com.fitness.training.ai

import android.content.Context
import com.fitness.training.network.AiChatRequest
import com.fitness.training.network.AiMessage
import com.fitness.training.network.RetrofitClient
import com.fitness.training.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DeepSeek AI服务
 * 
 * 安全改进：API KEY已移到后端服务器
 * 所有AI请求通过后端代理，客户端不再直接调用DeepSeek API
 */
object DeepSeekService {
    
    /**
     * 发送聊天消息（普通模式）
     * 
     * @param userMessage 用户输入的消息
     * @param context 上下文信息（用户数据）
     * @param appContext Android Context，用于获取token
     * @return Result包装的AI回复
     */
    suspend fun chat(
        userMessage: String, 
        context: String = "",
        appContext: Context
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取用户token
                val token = UserSession.getToken(appContext)
                if (token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("未登录，请先登录云端账号"))
                }
                
                // 构建消息列表
                val messages = buildMessages(userMessage, context)
                val request = AiChatRequest(messages)
                
                // 调用后端API
                val response = RetrofitClient.apiService.aiChat("Bearer $token", request)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val aiResponse = response.body()?.data?.message
                    if (aiResponse != null) {
                        Result.success(aiResponse)
                    } else {
                        Result.failure(Exception("AI回复为空"))
                    }
                } else {
                    Result.failure(Exception(response.body()?.message ?: "请求失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }
    
    /**
     * 发送聊天消息（流式模式）
     * 
     * @param userMessage 用户输入的消息
     * @param context 上下文信息
     * @param appContext Android Context
     * @param conversationHistory 对话历史
     * @param onChunk 接收到chunk时的回调（暂不支持真正的流式，返回完整消息）
     * @param onComplete 完成时的回调
     * @param onError 错误时的回调
     */
    suspend fun chatStream(
        userMessage: String, 
        context: String = "",
        appContext: Context,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 获取用户token
                val token = UserSession.getToken(appContext)
                if (token.isNullOrEmpty()) {
                    onError(Exception("未登录，请先登录云端账号"))
                    return@withContext
                }
                
                // 构建消息列表（包含历史对话）
                val messages = buildMessagesWithHistory(userMessage, context, conversationHistory)
                val request = AiChatRequest(messages)
                
                // 调用后端流式API
                val response = RetrofitClient.apiService.aiChatStream("Bearer $token", request)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val aiResponse = response.body()?.data?.message
                    if (aiResponse != null) {
                        // 模拟流式输出效果：逐字返回
                        aiResponse.forEach { char ->
                            onChunk(char.toString())
                            kotlinx.coroutines.delay(20) // 20ms延迟，模拟打字效果
                        }
                        onComplete()
                    } else {
                        onError(Exception("AI回复为空"))
                    }
                } else {
                    onError(Exception(response.body()?.message ?: "请求失败"))
                }
            } catch (e: Exception) {
                onError(Exception("网络错误: ${e.message}"))
            }
        }
    }
    
    /**
     * 构建消息列表
     */
    private fun buildMessages(userMessage: String, context: String): List<AiMessage> {
        val messages = mutableListOf<AiMessage>()
        
        // 添加上下文信息（如果有）
        if (context.isNotBlank()) {
            messages.add(AiMessage("user", "我的数据：\n$context\n\n问题：$userMessage"))
        } else {
            messages.add(AiMessage("user", userMessage))
        }
        
        return messages
    }
    
    /**
     * 构建包含历史对话的消息列表
     */
    private fun buildMessagesWithHistory(
        userMessage: String, 
        context: String,
        conversationHistory: List<Pair<String, String>>
    ): List<AiMessage> {
        val messages = mutableListOf<AiMessage>()
        
        // 添加历史对话
        conversationHistory.forEach { (userMsg, aiMsg) ->
            messages.add(AiMessage("user", userMsg))
            messages.add(AiMessage("assistant", aiMsg))
        }
        
        // 添加当前消息
        if (context.isNotBlank()) {
            messages.add(AiMessage("user", "我的数据：\n$context\n\n问题：$userMessage"))
        } else {
            messages.add(AiMessage("user", userMessage))
        }
        
        return messages
    }
}
