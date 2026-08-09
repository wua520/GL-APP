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
import com.fitness.training.util.ExerciseImageLoader

class ExerciseGridAdapter(
    private val onItemClick: (Exercise) -> Unit,
    private val onItemLongClick: (Exercise) -> Unit
) : ListAdapter<Exercise, ExerciseGridAdapter.ViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val ivExercise: ImageView = itemView.findViewById(R.id.iv_exercise)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.iv_favorite)

        fun bind(exercise: Exercise) {
            tvName.text = exercise.name
            ivFavorite.visibility = if (exercise.isFavorite) View.VISIBLE else View.GONE
            
            // 加载动作图片
            ExerciseImageLoader.loadExerciseImage(ivExercise, exercise.imageUrl)

            itemView.setOnClickListener { onItemClick(exercise) }
            itemView.setOnLongClickListener { 
                onItemLongClick(exercise)
                true
            }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem == newItem
    }
}
