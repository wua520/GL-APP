package com.fitness.training.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.google.android.material.textfield.TextInputEditText

class AiAssistantFragment : Fragment() {
    
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSend: ImageButton
    private lateinit var chipGroupQuickQuestions: com.google.android.material.chip.ChipGroup
    private lateinit var adapter: ChatAdapter
    
    private val viewModel: AiAssistantViewModel by activityViewModels()
    
    // 快捷问题列表
    private val quickQuestions = listOf(
        QuickQuestion("💪", "今天练什么", "根据我的训练记录，今天应该练什么部位？给我一些建议。"),
        QuickQuestion("🍎", "饮食建议", "根据我的目标和今天的训练，我应该怎么安排饮食？"),
        QuickQuestion("📊", "分析数据", "帮我分析最近的训练和身体数据，看看有什么需要改进的地方。"),
        QuickQuestion("🏋️", "深蹲要领", "深蹲的正确姿势和注意事项是什么？"),
        QuickQuestion("⚡", "增肌计划", "我想增肌，帮我制定一个训练计划。"),
        QuickQuestion("🔥", "减脂建议", "我想减脂，应该怎么训练和饮食？"),
        QuickQuestion("🍗", "蛋白质", "我每天需要摄入多少蛋白质？有什么好的蛋白质来源？"),
        QuickQuestion("😴", "休息恢复", "训练后如何更好地恢复？需要休息多久？"),
        QuickQuestion("💊", "补剂建议", "健身需要吃什么补剂吗？"),
        QuickQuestion("🤔", "训练疑问", "为什么我练了很久还是没效果？")
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ai_assistant, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupListeners()
        restoreMessages()
        
        return view
    }
    
    private fun initViews(view: View) {
        rvMessages = view.findViewById(R.id.rv_messages)
        etMessage = view.findViewById(R.id.et_message)
        btnSend = view.findViewById(R.id.btn_send)
        chipGroupQuickQuestions = view.findViewById(R.id.chip_group_quick_questions)
        
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }
        
        view.findViewById<ImageButton>(R.id.btn_clear).setOnClickListener {
            showClearConfirmDialog()
        }
        
        // 初始化快捷问题按钮
        setupQuickQuestions()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = false  // 不从底部堆叠，保持正常顺序
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter
    }
    
    private fun setupListeners() {
        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty() && !viewModel.isLoading) {
                sendMessage(message)
                etMessage.text?.clear()
            }
        }
    }
    
    private fun restoreMessages() {
        // 先从数据库加载历史记录
        viewModel.loadMessagesFromDatabase()
        
        // 延迟一下等待数据库加载完成
        rvMessages.postDelayed({
            if (viewModel.isInitialized) {
                // 有历史记录，恢复显示
                viewModel.messages.forEach { msg ->
                    adapter.addMessage(msg)
                }
                scrollToBottom()
            } else {
                // 首次打开，显示欢迎消息
                val welcomeMsg = ChatMessage(
                    "👋 你好！我是你的智能健身助手。\n\n你可以问我：\n• 训练动作的正确姿势\n• 制定训练计划\n• 饮食和营养建议\n• 分析你的训练数据\n\n有什么可以帮你的？",
                    isUser = false
                )
                adapter.addMessage(welcomeMsg)
                viewModel.addMessage(welcomeMsg)
                viewModel.markInitialized()
            }
        }, 100)
    }
    
    private fun sendMessage(message: String) {
        // 添加用户消息
        val userMessage = ChatMessage(message, isUser = true)
        adapter.addMessage(userMessage)
        viewModel.addMessage(userMessage)
        
        // 保存用户消息到数据库
        viewModel.saveUserMessage(userMessage)
        
        // 添加AI加载消息
        adapter.addMessage(ChatMessage("", isUser = false, isLoading = true))
        scrollToBottom()
        
        // 通过ViewModel发送消息（流式响应会实时更新）
        viewModel.sendMessage(message) { chunk ->
            // 直接更新TextView，不触发notifyItemChanged，避免卡顿
            adapter.appendToLastMessageDirect(chunk)
        }
        
        // 监听回复完成，滚动到底部
        viewModel.onResponseComplete = {
            scrollToBottom()
        }
    }
    
    private fun refreshMessages() {
        adapter.clearMessages()
        viewModel.messages.forEach { msg ->
            adapter.addMessage(msg)
        }
        scrollToBottom()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.onResponseComplete = null
    }
    
    private fun scrollToBottom() {
        rvMessages.post {
            if (adapter.getItemCount2() > 0) {
                rvMessages.smoothScrollToPosition(adapter.getItemCount2() - 1)
            }
        }
    }
    
    private fun clearChat() {
        adapter.clearMessages()
        viewModel.clearMessages()
        
        // 重新显示欢迎消息
        val welcomeMsg = ChatMessage(
            "👋 你好！我是你的智能健身助手。\n\n你可以问我：\n• 训练动作的正确姿势\n• 制定训练计划\n• 饮食和营养建议\n• 分析你的训练数据\n\n有什么可以帮你的？",
            isUser = false
        )
        adapter.addMessage(welcomeMsg)
        viewModel.addMessage(welcomeMsg)
        viewModel.markInitialized()
        
        Toast.makeText(requireContext(), "聊天记录已清空", Toast.LENGTH_SHORT).show()
    }
    
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清空聊天记录")
            .setMessage("确定要清空所有聊天记录吗？")
            .setPositiveButton("清空") { _, _ ->
                clearChat()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun setupQuickQuestions() {
        chipGroupQuickQuestions.removeAllViews()
        
        quickQuestions.forEach { question ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = "${question.emoji} ${question.label}"
                isClickable = true
                isCheckable = false
                setChipBackgroundColorResource(R.color.surface)
                setTextColor(resources.getColor(R.color.text_primary, null))
                chipStrokeWidth = 1f
                setChipStrokeColorResource(R.color.divider)
                
                setOnClickListener {
                    // 点击快捷问题时发送完整问题
                    if (!viewModel.isLoading) {
                        sendMessage(question.fullQuestion)
                    }
                }
            }
            chipGroupQuickQuestions.addView(chip)
        }
    }
}

// 快捷问题数据类
data class QuickQuestion(
    val emoji: String,
    val label: String,
    val fullQuestion: String
)
