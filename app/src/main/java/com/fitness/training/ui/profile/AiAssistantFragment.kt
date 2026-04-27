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
    private lateinit var adapter: ChatAdapter
    
    private val viewModel: AiAssistantViewModel by activityViewModels()
    
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
        
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }
        
        view.findViewById<ImageButton>(R.id.btn_clear).setOnClickListener {
            showClearConfirmDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        rvMessages.layoutManager = LinearLayoutManager(requireContext())
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
        if (viewModel.isInitialized) {
            // 恢复之前的消息
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
    }
    
    private fun sendMessage(message: String) {
        // 先更新UI显示用户消息和加载状态
        adapter.addMessage(ChatMessage(message, isUser = true))
        adapter.addMessage(ChatMessage("", isUser = false, isLoading = true))
        scrollToBottom()
        
        // 移除刚添加的，让ViewModel管理
        adapter.clearMessages()
        
        // 通过ViewModel发送消息
        viewModel.sendMessage(message) {
            // 回调时刷新UI
            refreshMessages()
        }
        
        // 立即刷新显示
        refreshMessages()
    }
    
    private fun refreshMessages() {
        adapter.clearMessages()
        viewModel.messages.forEach { msg ->
            adapter.addMessage(msg)
        }
        scrollToBottom()
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
}
