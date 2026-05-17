package com.fitness.training.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.training.ai.DeepSeekService
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.AiChatMessage
import com.fitness.training.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _messages = mutableListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages
    
    private val database = FitnessDatabase.getDatabase(application)
    
    var isInitialized = false
        private set
    
    var isLoading = false
        private set
    
    var onResponseComplete: (() -> Unit)? = null  // 回复完成回调
    
    fun addMessage(message: ChatMessage) {
        _messages.add(message)
    }
    
    fun saveUserMessage(message: ChatMessage) {
        saveMessageToDatabase(message)
    }
    
    fun updateLastMessage(content: String) {
        if (_messages.isNotEmpty()) {
            val last = _messages.last()
            _messages[_messages.lastIndex] = last.copy(content = content, isLoading = false)
        }
    }
    
    fun markInitialized() {
        isInitialized = true
    }
    
    fun clearMessages() {
        _messages.clear()
        isInitialized = false
        
        // 清空数据库中的聊天记录
        viewModelScope.launch(Dispatchers.IO) {
            val userId = UserSession.getCurrentUserId(getApplication())
            database.aiChatMessageDao().deleteAllMessages(userId)
        }
    }
    
    fun loadMessagesFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = UserSession.getCurrentUserId(getApplication())
            val dbMessages = database.aiChatMessageDao().getAllMessages(userId)
            
            withContext(Dispatchers.Main) {
                _messages.clear()
                dbMessages.forEach { dbMsg ->
                    _messages.add(ChatMessage(dbMsg.content, dbMsg.isUser))
                }
                isInitialized = dbMessages.isNotEmpty()
            }
        }
    }
    
    private fun saveMessageToDatabase(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = UserSession.getCurrentUserId(getApplication())
            database.aiChatMessageDao().insert(
                AiChatMessage(
                    userId = userId,
                    content = message.content,
                    isUser = message.isUser
                )
            )
        }
    }
    
    fun sendMessage(message: String, onChunk: (String) -> Unit) {
        // 添加AI加载消息
        addMessage(ChatMessage("", isUser = false, isLoading = true))
        isLoading = true
        
        viewModelScope.launch {
            val context = getUserDataContext()
            
            // 构建对话历史（最近7轮，排除当前正在加载的消息）
            val conversationHistory = mutableListOf<Pair<String, String>>()
            var i = 0
            while (i < _messages.size - 1) {  // -1 排除当前加载中的消息
                val msg = _messages[i]
                if (msg.isUser && i + 1 < _messages.size - 1) {
                    val nextMsg = _messages[i + 1]
                    if (!nextMsg.isUser && !nextMsg.isLoading) {
                        conversationHistory.add(Pair(msg.content, nextMsg.content))
                        i += 2
                        continue
                    }
                }
                i++
            }
            
            // 使用流式API
            DeepSeekService.chatStream(
                userMessage = message,
                context = context,
                conversationHistory = conversationHistory,  // 传入对话历史
                onChunk = { chunk ->
                    // 每收到一段文字，就追加到最后一条消息
                    if (_messages.isNotEmpty()) {
                        val lastMessage = _messages.last()
                        val newContent = lastMessage.content + chunk
                        _messages[_messages.lastIndex] = lastMessage.copy(
                            content = newContent,
                            isLoading = false
                        )
                        onChunk(chunk)  // 通知UI追加内容
                    }
                },
                onComplete = {
                    // 流式响应完成，保存AI回复到数据库
                    isLoading = false
                    if (_messages.isNotEmpty()) {
                        val aiMessage = _messages.last()
                        saveMessageToDatabase(aiMessage)
                    }
                    onResponseComplete?.invoke()  // 通知Fragment滚动到底部
                },
                onError = { error ->
                    // 出错时显示错误信息
                    updateLastMessage("抱歉，出现了一些问题：${error.message}")
                    isLoading = false
                    if (_messages.isNotEmpty()) {
                        val aiMessage = _messages.last()
                        saveMessageToDatabase(aiMessage)
                    }
                    onResponseComplete?.invoke()  // 通知Fragment滚动到底部
                }
            )
        }
    }
    
    private suspend fun getUserDataContext(): String = withContext(Dispatchers.IO) {
        val userId = UserSession.getCurrentUserId(getApplication())
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        try {
            // 1. 用户基本信息
            val user = database.userDao().getUserByIdSync(userId)
            if (user != null) {
                sb.append("【用户信息】\n")
                sb.append("昵称：${user.nickname.ifEmpty { user.username }}\n")
                sb.append("注册时间：${dateFormat.format(Date(user.createdAt))}\n\n")
            }
            
            // 2. 身体数据（最近10条记录）
            val bodyRecords = database.bodyRecordDao().getAllByUserSync(userId).take(10)
            if (bodyRecords.isNotEmpty()) {
                sb.append("【身体数据】\n")
                val latest = bodyRecords.first()
                latest.weight?.let { sb.append("当前体重：${it}kg\n") }
                latest.bodyFat?.let { sb.append("当前体脂：${it}%\n") }
                latest.muscleMass?.let { sb.append("当前肌肉量：${it}kg\n") }
                
                if (bodyRecords.size > 1) {
                    val oldest = bodyRecords.last()
                    oldest.weight?.let { oldW ->
                        latest.weight?.let { newW ->
                            val change = newW - oldW
                            sb.append("体重变化：${if (change >= 0) "+" else ""}${"%.1f".format(change)}kg\n")
                        }
                    }
                }
                sb.append("\n")
            }
            
            // 3. 训练记录统计
            val allWorkouts = database.workoutDao().getAllWorkoutsSync(userId)
            if (allWorkouts.isNotEmpty()) {
                sb.append("【训练统计】\n")
                sb.append("总训练次数：${allWorkouts.size}次\n")
                
                val totalDuration = allWorkouts.sumOf { it.duration }
                val hours = totalDuration / 3600000
                val minutes = (totalDuration % 3600000) / 60000
                sb.append("总训练时长：${hours}小时${minutes}分钟\n")
                
                // 最近7天
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                val recentWorkouts = allWorkouts.filter { it.date >= sevenDaysAgo }
                sb.append("最近7天训练：${recentWorkouts.size}次\n")
                
                // 最近5次训练详情
                sb.append("最近训练：\n")
                allWorkouts.take(5).forEach { workout ->
                    val durationMin = workout.duration / 60000
                    sb.append("  - ${dateFormat.format(Date(workout.date))} ${workout.name} (${durationMin}分钟)\n")
                }
                sb.append("\n")
            }
            
            // 4. 训练计划
            val plans = database.trainingPlanDao().getAllPlansSync(userId)
            if (plans.isNotEmpty()) {
                sb.append("【训练计划】\n")
                plans.forEach { plan ->
                    sb.append("  - ${plan.title}：${plan.description}\n")
                }
                sb.append("\n")
            }
            
            // 5. 饮食记录
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayDiet = database.dietRecordDao().getByDateSync(userId, today)
            if (todayDiet.isNotEmpty()) {
                sb.append("【今日饮食】\n")
                val totalCalories = todayDiet.sumOf { it.calories }
                val totalProtein = todayDiet.sumOf { it.protein.toDouble() }
                val totalCarbs = todayDiet.sumOf { it.carbs.toDouble() }
                val totalFat = todayDiet.sumOf { it.fat.toDouble() }
                sb.append("热量：${totalCalories}千卡\n")
                sb.append("蛋白质：${"%.1f".format(totalProtein)}g\n")
                sb.append("碳水：${"%.1f".format(totalCarbs)}g\n")
                sb.append("脂肪：${"%.1f".format(totalFat)}g\n")
                sb.append("记录条数：${todayDiet.size}条\n\n")
            }
            
            // 6. 成就
            val achievements = database.achievementDao().getUnlockedAchievementsSync()
            if (achievements.isNotEmpty()) {
                sb.append("【已解锁成就】\n")
                achievements.take(5).forEach { achievement ->
                    sb.append("  - ${achievement.name}\n")
                }
                if (achievements.size > 5) {
                    sb.append("  ...共${achievements.size}个成就\n")
                }
            }
            
        } catch (e: Exception) {
            // 忽略错误
        }
        
        sb.toString()
    }
}
