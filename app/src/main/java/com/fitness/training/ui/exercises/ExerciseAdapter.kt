package com.fitness.training.ui.exercises

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Exercise

class ExerciseAdapter(
    private val onItemClick: (Exercise) -> Unit,
    private val onFavoriteClick: (Exercise) -> Unit,
    private val onDeleteClick: (Exercise) -> Unit
) : ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_library, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_exercise_info)
        private val tvCustomTag: TextView = itemView.findViewById(R.id.tv_custom_tag)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.iv_favorite)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)

        fun bind(exercise: Exercise) {
            tvName.text = exercise.name
            tvInfo.text = "${exercise.muscleGroup} · ${exercise.equipment}"
            
            // 自定义标签
            tvCustomTag.visibility = if (exercise.isCustom) View.VISIBLE else View.GONE
            
            // 收藏图标
            ivFavorite.setImageResource(
                if (exercise.isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            
            // 删除按钮只对自定义动作显示
            ivDelete.visibility = if (exercise.isCustom) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener { onItemClick(exercise) }
            ivFavorite.setOnClickListener { onFavoriteClick(exercise) }
            ivDelete.setOnClickListener { onDeleteClick(exercise) }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem
        }
    }
}


