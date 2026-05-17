package com.fitness.training.ui.plans

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.TrainingPlan
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class TrainingPlanAdapter(
    private val onDeleteClick: (TrainingPlan) -> Unit,
    private val onEditClick: (TrainingPlan) -> Unit,
    private val onExpandClick: (TrainingPlan) -> Unit,
    private val onPinClick: (TrainingPlan) -> Unit = {}
) : ListAdapter<TrainingPlan, TrainingPlanAdapter.PlanViewHolder>(PlanDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPinned: ImageView = itemView.findViewById(R.id.iv_pinned)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_plan_title)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_plan_description)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_plan_info)
        private val tvCreatedTime: TextView = itemView.findViewById(R.id.tv_created_time)
        private val tvUpdatedTime: TextView = itemView.findViewById(R.id.tv_updated_time)
        private val tvDetails: TextView = itemView.findViewById(R.id.tv_plan_details)
        private val btnPin: MaterialButton = itemView.findViewById(R.id.btn_pin)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnExpand: MaterialButton = itemView.findViewById(R.id.btn_expand)

        fun bind(plan: TrainingPlan) {
            tvTitle.text = plan.title
            tvDescription.text = plan.description
            
            val infoText = buildString {
                append("目标：${plan.goal}")
                append(" | 经验：${plan.experience}")
                append(" | ${plan.trainingDays}天/周")
                append(" | ${plan.trainingDuration}")
                append(" | ${plan.equipment}")
                if (plan.targetMuscles.isNotEmpty()) {
                    append("\n强化：${plan.targetMuscles}")
                }
            }
            tvInfo.text = infoText
            
            // 显示时间
            tvCreatedTime.text = "创建：${dateFormat.format(Date(plan.createdAt))}"
            tvUpdatedTime.text = "编辑：${dateFormat.format(Date(plan.updatedAt))}"
            
            tvDetails.text = plan.details
            tvDetails.maxLines = 3
            
            // 置顶状态
            ivPinned.visibility = if (plan.isPinned) View.VISIBLE else View.GONE
            btnPin.text = if (plan.isPinned) "取消置顶" else "置顶"
            
            btnPin.setOnClickListener {
                onPinClick(plan)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(plan)
            }
            
            btnEdit.setOnClickListener {
                onEditClick(plan)
            }
            
            btnExpand.setOnClickListener {
                if (tvDetails.maxLines == 3) {
                    tvDetails.maxLines = Int.MAX_VALUE
                    btnExpand.text = "收起"
                } else {
                    tvDetails.maxLines = 3
                    btnExpand.text = "查看详情"
                }
            }
        }
    }

    class PlanDiffCallback : DiffUtil.ItemCallback<TrainingPlan>() {
        override fun areItemsTheSame(oldItem: TrainingPlan, newItem: TrainingPlan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TrainingPlan, newItem: TrainingPlan): Boolean {
            return oldItem == newItem
        }
    }
}
