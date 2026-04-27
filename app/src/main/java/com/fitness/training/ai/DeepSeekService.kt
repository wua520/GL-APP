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
    // TODO: 上线前需要移除硬编码的API密钥
    // 建议：让用户在设置中输入自己的API密钥，或通过后端代理
    private const val API_KEY = "" // 已移除，需要用户自己配置
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
                    maxTokens = 1024
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
}

data class Message(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("max_tokens") val maxTokens: Int
)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: Message?
)
