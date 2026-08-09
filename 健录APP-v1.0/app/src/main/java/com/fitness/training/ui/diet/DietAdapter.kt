package com.fitness.training.ui.diet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.DietRecord

data class MealGroup(
    val mealType: String,
    val records: List<DietRecord>,
    val totalCalories: Int
)

class DietAdapter(
    private val onDeleteClick: (DietRecord) -> Unit
) : ListAdapter<MealGroup, DietAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMealType: TextView = itemView.findViewById(R.id.tv_meal_type)
        private val tvMealCalories: TextView = itemView.findViewById(R.id.tv_meal_calories)
        private val recyclerFoods: RecyclerView = itemView.findViewById(R.id.recycler_foods)

        fun bind(mealGroup: MealGroup) {
            tvMealType.text = mealGroup.mealType
            tvMealCalories.text = "${mealGroup.totalCalories} 千卡"
            
            val foodAdapter = FoodAdapter(onDeleteClick)
            recyclerFoods.layoutManager = LinearLayoutManager(itemView.context)
            recyclerFoods.adapter = foodAdapter
            foodAdapter.submitList(mealGroup.records)
        }
    }

    class MealDiffCallback : DiffUtil.ItemCallback<MealGroup>() {
        override fun areItemsTheSame(oldItem: MealGroup, newItem: MealGroup): Boolean {
            return oldItem.mealType == newItem.mealType
        }

        override fun areContentsTheSame(oldItem: MealGroup, newItem: MealGroup): Boolean {
            return oldItem == newItem
        }
    }
}

class FoodAdapter(
    private val onDeleteClick: (DietRecord) -> Unit
) : ListAdapter<DietRecord, FoodAdapter.FoodViewHolder>(FoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFoodName: TextView = itemView.findViewById(R.id.tv_food_name)
        private val tvFoodAmount: TextView = itemView.findViewById(R.id.tv_food_amount)
        private val tvFoodNutrition: TextView = itemView.findViewById(R.id.tv_food_nutrition)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(record: DietRecord) {
            tvFoodName.text = record.foodName
            
            if (record.amount.isNotEmpty()) {
                tvFoodAmount.text = record.amount
                tvFoodAmount.visibility = View.VISIBLE
            } else {
                tvFoodAmount.visibility = View.GONE
            }
            
            // 显示营养信息
            val nutritionParts = mutableListOf<String>()
            if (record.calories > 0) nutritionParts.add("${record.calories}千卡")
            if (record.protein > 0) nutritionParts.add("蛋白${record.protein.toInt()}g")
            tvFoodNutrition.text = nutritionParts.joinToString(" · ")
            tvFoodNutrition.visibility = if (nutritionParts.isEmpty()) View.GONE else View.VISIBLE
            
            btnDelete.setOnClickListener {
                onDeleteClick(record)
            }
        }
    }

    class FoodDiffCallback : DiffUtil.ItemCallback<DietRecord>() {
        override fun areItemsTheSame(oldItem: DietRecord, newItem: DietRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DietRecord, newItem: DietRecord): Boolean {
            return oldItem == newItem
        }
    }
}
