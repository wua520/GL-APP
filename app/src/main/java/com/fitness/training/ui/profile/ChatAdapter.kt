package com.fitness.training.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.google.android.material.card.MaterialCardView

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    
    private val messages = mutableListOf<ChatMessage>()
    private var lastMessageViewHolder: ViewHolder? = null  // 保存最后一条消息的ViewHolder
    
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    fun updateLastMessage(content: String) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            messages[lastIndex] = messages[lastIndex].copy(content = content, isLoading = false)
            notifyItemChanged(lastIndex)
        }
    }
    
    // 新增：直接更新TextView，不触发notifyItemChanged
    fun appendToLastMessageDirect(chunk: String) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            messages[lastIndex] = messages[lastIndex].copy(
                content = messages[lastIndex].content + chunk,
                isLoading = false
            )
            // 直接更新TextView，不通知Adapter
            lastMessageViewHolder?.updateText(messages[lastIndex].content)
        }
    }
    
    // 保留原方法作为备用
    fun appendToLastMessage(chunk: String) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            val currentContent = messages[lastIndex].content
            messages[lastIndex] = messages[lastIndex].copy(
                content = currentContent + chunk,
                isLoading = false
            )
            notifyItemChanged(lastIndex)
        }
    }
    
    fun getItemCount2() = messages.size
    
    fun clearMessages() {
        messages.clear()
        lastMessageViewHolder = null
        notifyDataSetChanged()
    }
    
    override fun getItemCount() = messages.size
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
        // 保存最后一条消息的ViewHolder引用
        if (position == messages.size - 1) {
            lastMessageViewHolder = holder
        }
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardUser: MaterialCardView = itemView.findViewById(R.id.card_user)
        private val tvUserMessage: TextView = itemView.findViewById(R.id.tv_user_message)
        private val layoutAi: LinearLayout = itemView.findViewById(R.id.layout_ai)
        private val tvAiMessage: TextView = itemView.findViewById(R.id.tv_ai_message)
        
        fun bind(message: ChatMessage) {
            if (message.isUser) {
                cardUser.visibility = View.VISIBLE
                layoutAi.visibility = View.GONE
                tvUserMessage.text = message.content
            } else {
                cardUser.visibility = View.GONE
                layoutAi.visibility = View.VISIBLE
                tvAiMessage.text = if (message.isLoading) "思考中..." else message.content
            }
        }
        
        // 新增：直接更新TextView文本
        fun updateText(content: String) {
            tvAiMessage.text = content
        }
    }
}
