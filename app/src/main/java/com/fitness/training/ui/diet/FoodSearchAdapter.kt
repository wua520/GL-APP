package com.fitness.training.ui.diet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Food

class FoodSearchAdapter(
    private val onFoodClick: (Food) -> Unit
) : ListAdapter<Food, FoodSearchAdapter.FoodViewHolder>(FoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_search, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFoodName: TextView = itemView.findViewById(R.id.tv_food_name)
        private val tvFoodCalories: TextView = itemView.findViewById(R.id.tv_food_calories)
        private val tvFoodNutrition: TextView = itemView.findViewById(R.id.tv_food_nutrition)

        fun bind(food: Food) {
            tvFoodName.text = food.name
            tvFoodCalories.text = "${food.calories}千卡/${food.unit}"
            tvFoodNutrition.text = "蛋白${food.protein.toInt()}g · 碳水${food.carbs.toInt()}g · 脂肪${food.fat.toInt()}g"
            
            itemView.setOnClickListener {
                onFoodClick(food)
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
