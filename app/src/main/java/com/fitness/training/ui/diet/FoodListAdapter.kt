package com.fitness.training.ui.diet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Food

class FoodListAdapter(
    private val onAddClick: (Food) -> Unit,
    private val onDeleteClick: ((Food) -> Unit)? = null
) : ListAdapter<Food, FoodListAdapter.FoodViewHolder>(FoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFoodName: TextView = itemView.findViewById(R.id.tv_food_name)
        private val tvFoodInfo: TextView = itemView.findViewById(R.id.tv_food_info)
        private val btnAdd: ImageButton = itemView.findViewById(R.id.btn_add)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(food: Food) {
            tvFoodName.text = food.name
            tvFoodInfo.text = "${food.calories}千卡/${food.unit}"
            
            // 自定义食物显示删除按钮
            btnDelete.visibility = if (food.isCustom && onDeleteClick != null) View.VISIBLE else View.GONE
            
            btnAdd.setOnClickListener {
                onAddClick(food)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick?.invoke(food)
            }
            
            itemView.setOnClickListener {
                onAddClick(food)
            }
        }
    }

    class FoodDiffCallback : DiffUtil.ItemCallback<Food>() {
        override fun areItemsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem == newItem
        }
    }
}
