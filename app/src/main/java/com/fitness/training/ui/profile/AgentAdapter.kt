package com.fitness.training.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton

/**
 * Agent 消息适配器
 * 支持多种消息类型：用户消息、助手消息、进度步骤、待确认操作、错误消息、加载中
 */
class AgentAdapter(
    private val onConfirmAction: (Long, Long) -> Unit,  // (taskId, actionId)
    private val onCancelAction: (Long, Long) -> Unit,  // (taskId, actionId)
    private val onEditDraft: (Long, Long, List<EditableFoodItem>) -> Unit, // (taskId, actionId, foodItems)
    private val onRetryLocalWrite: (Long, Long) -> Unit // (taskId, actionId)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private val items = mutableListOf<AgentMessageItem>()
    
    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_ASSISTANT = 1
        private const val VIEW_TYPE_PROGRESS = 2
        private const val VIEW_TYPE_PENDING_ACTION = 3
        private const val VIEW_TYPE_ERROR = 4
        private const val VIEW_TYPE_LOADING = 5
        private const val VIEW_TYPE_DIET_DRAFT_DETAILED = 6
        private const val VIEW_TYPE_LOCAL_WRITE_RECOVERY = 7
    }
    
    fun submitList(newItems: List<AgentMessageItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is AgentMessageItem.UserMessage -> VIEW_TYPE_USER
            is AgentMessageItem.AssistantMessage -> VIEW_TYPE_ASSISTANT
            is AgentMessageItem.ProgressStep -> VIEW_TYPE_PROGRESS
            is AgentMessageItem.PendingActionItem -> {
                // 如果是饮食记录且 preview 是 Map（详细模式），使用详细布局
                if (item.action.type == "CREATE_DIET_RECORD" && item.action.preview is Map<*, *>) {
                    VIEW_TYPE_DIET_DRAFT_DETAILED
                } else {
                    VIEW_TYPE_PENDING_ACTION
                }
            }
            is AgentMessageItem.LocalWriteRecoveryItem -> VIEW_TYPE_LOCAL_WRITE_RECOVERY
            is AgentMessageItem.ErrorMessage -> VIEW_TYPE_ERROR
            is AgentMessageItem.LoadingMessage -> VIEW_TYPE_LOADING
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_agent_user_message, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_ASSISTANT -> {
                val view = inflater.inflate(R.layout.item_agent_assistant_message, parent, false)
                AssistantMessageViewHolder(view)
            }
            VIEW_TYPE_PROGRESS -> {
                val view = inflater.inflate(R.layout.item_agent_progress, parent, false)
                ProgressViewHolder(view)
            }
            VIEW_TYPE_PENDING_ACTION -> {
                val view = inflater.inflate(R.layout.item_agent_pending_action, parent, false)
                PendingActionViewHolder(view, onConfirmAction, onCancelAction)
            }
            VIEW_TYPE_LOCAL_WRITE_RECOVERY -> {
                val view = inflater.inflate(R.layout.item_agent_local_write_recovery, parent, false)
                LocalWriteRecoveryViewHolder(view, onRetryLocalWrite)
            }
            VIEW_TYPE_ERROR -> {
                val view = inflater.inflate(R.layout.item_agent_error, parent, false)
                ErrorViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = inflater.inflate(R.layout.item_agent_loading, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_DIET_DRAFT_DETAILED -> {
                val view = inflater.inflate(R.layout.item_agent_diet_draft_detailed, parent, false)
                DietDraftDetailedViewHolder(view, onConfirmAction, onCancelAction, onEditDraft)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AgentMessageItem.UserMessage -> (holder as UserMessageViewHolder).bind(item)
            is AgentMessageItem.AssistantMessage -> (holder as AssistantMessageViewHolder).bind(item)
            is AgentMessageItem.ProgressStep -> (holder as ProgressViewHolder).bind(item)
            is AgentMessageItem.PendingActionItem -> {
                if (holder is DietDraftDetailedViewHolder) {
                    holder.bind(item)
                } else {
                    (holder as PendingActionViewHolder).bind(item)
                }
            }
            is AgentMessageItem.LocalWriteRecoveryItem -> (holder as LocalWriteRecoveryViewHolder).bind(item)
            is AgentMessageItem.ErrorMessage -> (holder as ErrorViewHolder).bind(item)
            is AgentMessageItem.LoadingMessage -> (holder as LoadingViewHolder).bind(item)
        }
    }
    
    override fun getItemCount() = items.size
    
    // ViewHolders
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardUser: MaterialCardView = itemView.findViewById(R.id.card_user)
        private val tvUserMessage: TextView = itemView.findViewById(R.id.tv_user_message)
        
        fun bind(item: AgentMessageItem.UserMessage) {
            tvUserMessage.text = item.content
        }
    }
    
    class AssistantMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAiMessage: TextView = itemView.findViewById(R.id.tv_ai_message)
        
        fun bind(item: AgentMessageItem.AssistantMessage) {
            tvAiMessage.text = item.content
        }
    }
    
    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProgress: TextView = itemView.findViewById(R.id.tv_progress)
        
        fun bind(item: AgentMessageItem.ProgressStep) {
            val steps = item.steps.joinToString("\n") { "✓ $it" }
            tvProgress.text = steps
        }
    }
    
    class PendingActionViewHolder(
        itemView: View,
        private val onConfirm: (Long, Long) -> Unit,
        private val onCancel: (Long, Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvActionTitle: TextView = itemView.findViewById(R.id.tv_action_title)
        private val tvActionDesc: TextView = itemView.findViewById(R.id.tv_action_desc)
        private val btnConfirm: MaterialButton = itemView.findViewById(R.id.btn_confirm)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btn_cancel)
        
        fun bind(item: AgentMessageItem.PendingActionItem) {
            // 解析preview（可能是Map或String）
            val title: String
            val description: String
            
            when (val preview = item.action.preview) {
                is Map<*, *> -> {
                    title = preview["title"]?.toString() ?: "待确认操作"
                    description = preview["description"]?.toString() ?: ""
                }
                is String -> {
                    title = "待确认操作"
                    description = preview
                }
                else -> {
                    title = "待确认操作"
                    description = ""
                }
            }
            
            tvActionTitle.text = title
            tvActionDesc.text = description
            
            btnConfirm.setOnClickListener {
                onConfirm(item.taskId, item.action.actionId)
            }
            
            btnCancel.setOnClickListener {
                onCancel(item.taskId, item.action.actionId)
            }
        }
    }
    
    class LocalWriteRecoveryViewHolder(
        itemView: View,
        private val onRetry: (Long, Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_local_write_recovery_title)
        private val error: TextView = itemView.findViewById(R.id.tv_local_write_recovery_error)
        private val retry: MaterialButton = itemView.findViewById(R.id.btn_retry_local_write)

        fun bind(item: AgentMessageItem.LocalWriteRecoveryItem) {
            title.text = when (item.type) {
                "CREATE_TRAINING_PLAN" -> "训练计划已保存，等待完成同步"
                "CREATE_DIET_RECORD" -> "饮食记录已保存，等待完成同步"
                else -> "本地数据已保存，等待完成同步"
            }
            error.text = "上次失败：${item.error}（已重试 ${item.retryCount} 次）"
            retry.setOnClickListener { onRetry(item.taskId, item.actionId) }
        }
    }

    class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvError: TextView = itemView.findViewById(R.id.tv_error)
        
        fun bind(item: AgentMessageItem.ErrorMessage) {
            tvError.text = item.message
        }
    }
    
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: AgentMessageItem.LoadingMessage) {
            // Loading animation handled by layout
        }
    }
    
    /**
     * 饮食草案详细展示 ViewHolder
     */
    class DietDraftDetailedViewHolder(
        itemView: View,
        private val onConfirm: (Long, Long) -> Unit,
        private val onCancel: (Long, Long) -> Unit,
        private val onEdit: (Long, Long, List<EditableFoodItem>) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvDraftTitle: TextView = itemView.findViewById(R.id.tv_draft_title)
        private val tvDraftDate: TextView = itemView.findViewById(R.id.tv_draft_date)
        private val tvDraftCount: TextView = itemView.findViewById(R.id.tv_draft_count)
        private val tvDraftCalories: TextView = itemView.findViewById(R.id.tv_draft_calories)
        private val tvDraftProtein: TextView = itemView.findViewById(R.id.tv_draft_protein)
        private val rvFoodList: RecyclerView = itemView.findViewById(R.id.rv_food_list)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btn_cancel)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnConfirm: MaterialButton = itemView.findViewById(R.id.btn_confirm)
        
        fun bind(item: AgentMessageItem.PendingActionItem) {
            val preview = item.action.preview as? Map<*, *> ?: return
            
            // 解析 preview 数据
            val title = preview["title"]?.toString() ?: "饮食记录待确认"
            val date = preview["date"]?.toString() ?: ""
            val recordCount = (preview["recordCount"] as? Number)?.toInt() ?: 0
            val totalCalories = (preview["totalCalories"] as? Number)?.toInt() ?: 0
            val totalProtein = (preview["totalProtein"] as? Number)?.toFloat() ?: 0f
            val details = preview["details"] as? List<*> ?: emptyList<Any>()
            
            tvDraftTitle.text = title
            tvDraftDate.text = "日期：$date"
            tvDraftCount.text = "共${recordCount}条记录"
            tvDraftCalories.text = "热量：$totalCalories kcal"
            tvDraftProtein.text = "蛋白质：${String.format("%.1f", totalProtein)}g"
            
            // 构建食物列表
            val foodItems = details.mapNotNull { detail ->
                val detailMap = detail as? Map<*, *> ?: return@mapNotNull null
                FoodDisplayItem(
                    mealType = detailMap["mealType"]?.toString() ?: "",
                    foodName = detailMap["foodName"]?.toString() ?: "",
                    amount = detailMap["amount"]?.toString() ?: "",
                    calories = (detailMap["calories"] as? Number)?.toInt() ?: 0,
                    protein = (detailMap["protein"] as? Number)?.toFloat() ?: 0f,
                    carbs = (detailMap["carbs"] as? Number)?.toFloat() ?: 0f,
                    fat = (detailMap["fat"] as? Number)?.toFloat() ?: 0f,
                    isEstimated = detailMap["isEstimated"] as? Boolean ?: false
                )
            }
            
            // 设置食物列表适配器
            rvFoodList.layoutManager = LinearLayoutManager(itemView.context)
            rvFoodList.adapter = ReadOnlyFoodAdapter(foodItems)
            
            // 按钮点击事件
            btnCancel.setOnClickListener {
                onCancel(item.taskId, item.action.actionId)
            }
            
            btnEdit.setOnClickListener {
                // 转换为可编辑格式
                val editableItems = foodItems.map { food ->
                    EditableFoodItem(
                        mealType = food.mealType,
                        foodName = food.foodName,
                        amount = food.amount,
                        calories = food.calories,
                        protein = food.protein,
                        carbs = food.carbs,
                        fat = food.fat,
                        isEstimated = food.isEstimated
                    )
                }
                onEdit(item.taskId, item.action.actionId, editableItems)
            }
            
            btnConfirm.setOnClickListener {
                onConfirm(item.taskId, item.action.actionId)
            }
        }
    }
}
