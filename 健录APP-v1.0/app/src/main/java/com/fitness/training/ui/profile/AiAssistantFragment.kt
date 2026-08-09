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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AiAssistantFragment : Fragment() {
    
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSend: ImageButton
    private lateinit var chipGroupQuickQuestions: com.google.android.material.chip.ChipGroup
    private lateinit var adapter: AgentAdapter
    
    private val viewModel: AgentViewModel by activityViewModels()
    
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
        observeViewModel()
        
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
        adapter = AgentAdapter(
            onConfirmAction = { taskId, actionId ->
                viewModel.confirmAction(taskId, actionId)
            },
            onCancelAction = { taskId, actionId ->
                viewModel.cancelAction(taskId, actionId)
            },
            onEditDraft = { taskId, actionId, foodItems ->
                showEditDraftDialog(taskId, actionId, foodItems)
            },
            onRetryLocalWrite = { taskId, actionId ->
                viewModel.retryLocalWrite(taskId, actionId)
            }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = false
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.items)
                if (state.items.isNotEmpty()) {
                    scrollToBottom()
                }
                
                // 显示错误Toast
                state.error?.let { error ->
                    if (error.isNotEmpty()) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun setupListeners() {
        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty() && !viewModel.uiState.value.isSubmitting) {
                viewModel.sendMessage(message)
                etMessage.text?.clear()
            }
        }
    }
    
    private fun sendMessage(message: String) {
        viewModel.sendMessage(message)
        etMessage.text?.clear()
    }
    
    private fun scrollToBottom() {
        rvMessages.post {
            if (adapter.itemCount > 0) {
                rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
            }
        }
    }
    
    private fun clearChat() {
        viewModel.clearMessages()
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
                    if (!viewModel.uiState.value.isSubmitting) {
                        sendMessage(question.fullQuestion)
                    }
                }
            }
            chipGroupQuickQuestions.addView(chip)
        }
    }
    
    /**
     * 显示编辑饮食草案对话框
     */
    private fun showEditDraftDialog(taskId: Long, actionId: Long, foodItems: List<EditableFoodItem>) {
        // 从当前 UI 状态中获取日期
        val currentItems = viewModel.uiState.value.items
        val pendingItem = currentItems.firstOrNull { 
            it is AgentMessageItem.PendingActionItem
                && it.taskId == taskId
                && it.action.actionId == actionId
        } as? AgentMessageItem.PendingActionItem
        
        val preview = pendingItem?.action?.preview as? Map<*, *>
        val date = preview?.get("date")?.toString() ?: ""
        
        val dialog = EditDietDraftDialog(
            date = date,
            foodItems = foodItems,
            onSave = { modifiedItems ->
                // 调用 ViewModel 更新草案
                viewModel.updateDraftPayload(taskId, actionId, date, modifiedItems)
            }
        )
        
        dialog.show(childFragmentManager, "EditDietDraftDialog")
    }
}

// 快捷问题数据类
data class QuickQuestion(
    val emoji: String,
    val label: String,
    val fullQuestion: String
)