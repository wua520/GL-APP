package com.fitness.training.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object DeepSeekService {
    // API密钥应该从BuildConfig或本地配置文件读取，不要硬编码
    // 在gradle.properties中配置: DEEPSEEK_API_KEY=your_key_here
    private const val API_KEY = BuildConfig.DEEPSEEK_API_KEY
    private const val BASE_URL = "https://api.deepseek.com/chat/completions"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    private val systemPrompt = """
你是健录App的智能健身助手，专门为用户提供健身、训练、饮食和身体管理方面的建议。

你的职责：
1. 回答健身相关问题（动作要领、训练计划、肌肉锻炼等）
2. 提供饮食建议（热量摄入、蛋白质补充、营养搭配等）
3. 分析用户的训练和身体数据，给出个性化建议
4. 鼓励和激励用户坚持锻炼

回答要求：
- 简洁专业，避免过长的回复
- 使用中文回答
- 给出实用可操作的建议
- 适当使用emoji增加亲和力
""".trimIndent()
    
    // 原有的非流式方法（保留作为备用）
    suspend fun chat(userMessage: String, context: String = ""): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val messages = mutableListOf(
                    Message("system", systemPrompt)
                )
                
                if (context.isNotBlank()) {
                    messages.add(Message("system", "用户数据：\n$context"))
                }
                
                messages.add(Message("user", userMessage))

                val requestBody = ChatRequest(
                    model = "deepseek-chat",
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 1024,
                    stream = false
                )
                
                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                    val content = chatResponse.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        Result.success(content)
                    } else {
                        Result.failure(Exception("无法获取回复"))
                    }
                } else {
                    Result.failure(Exception("请求失败: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun chatStream(
        userMessage: String, 
        context: String = "",
        conversationHistory: List<Pair<String, String>> = emptyList(),  // 新增：对话历史 (用户消息, AI回复)
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val messages = mutableListOf(
                    Message("system", systemPrompt)
                )
                
                if (context.isNotBlank()) {
                    messages.add(Message("system", "用户数据：\n$context"))
                }
                
                // 添加对话历史（最近7轮，即14条消息）
                conversationHistory.takeLast(7).forEach { (userMsg, aiMsg) ->
                    messages.add(Message("user", userMsg))
                    messages.add(Message("assistant", aiMsg))
                }
                
                // 添加当前用户消息
                messages.add(Message("user", userMessage))

                val requestBody = ChatRequest(
                    model = "deepseek-chat",
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 1024,
                    stream = true  // 启用流式响应
                )
                
                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .post(body)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val source = response.body?.source()
                    if (source != null) {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            
                            // SSE格式：data: {...}
                            if (line.startsWith("data: ")) {
                                val data = line.substring(6)
                                
                                // 结束标记
                                if (data == "[DONE]") {
                                    withContext(Dispatchers.Main) {
                                        onComplete()
                                    }
                                    break
                                }
                                
                                try {
                                    val streamResponse = gson.fromJson(data, StreamResponse::class.java)
                                    val content = streamResponse.choices?.firstOrNull()?.delta?.content
                                    
                                    if (content != null) {
                                        withContext(Dispatchers.Main) {
                                            onChunk(content)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // 忽略解析错误，继续处理下一行
                                }
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(Exception("请求失败: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
}

data class Message(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("max_tokens") val maxTokens: Int,
    val stream: Boolean = false  // 新增：是否使用流式响应
)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: Message?
)

// 新增：流式响应的数据类
data class StreamResponse(
    val choices: List<StreamChoice>?
)

data class StreamChoice(
    val delta: Delta?
)

data class Delta(
    val content: String?
)
