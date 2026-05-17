package com.fitness.training.ui.training

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.WorkoutTemplate

class TemplateAdapter(
    private val onItemClick: (WorkoutTemplate) -> Unit,
    private val onDeleteClick: (WorkoutTemplate) -> Unit
) : ListAdapter<TemplateItem, TemplateAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvExercises: TextView = itemView.findViewById(R.id.tv_exercises)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        private val layoutContent: View = itemView.findViewById(R.id.layout_content)

        fun bind(item: TemplateItem) {
            tvName.text = item.template.name
            tvExercises.text = "${item.exerciseCount}个动作"
            
            layoutContent.setOnClickListener {
                onItemClick(item.template)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(item.template)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TemplateItem>() {
        override fun areItemsTheSame(oldItem: TemplateItem, newItem: TemplateItem): Boolean {
            return oldItem.template.id == newItem.template.id
        }

        override fun areContentsTheSame(oldItem: TemplateItem, newItem: TemplateItem): Boolean {
            return oldItem == newItem
        }
    }
}

data class TemplateItem(
    val template: WorkoutTemplate,
    val exerciseCount: Int
)
