package com.fitness.training.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R

/**
 * 只读食物列表适配器（用于详细草案展示）
 */
class ReadOnlyFoodAdapter(
    private val items: List<FoodDisplayItem>
) : RecyclerView.Adapter<ReadOnlyFoodAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_food_readonly, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMealType: TextView = itemView.findViewById(R.id.tv_meal_type)
        private val tvFoodName: TextView = itemView.findViewById(R.id.tv_food_name)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvNutrition: TextView = itemView.findViewById(R.id.tv_nutrition)

        fun bind(item: FoodDisplayItem) {
            tvMealType.text = item.mealType
            tvFoodName.text = if (item.isEstimated) "${item.foodName}（估算值，仅供参考）" else item.foodName
            tvAmount.text = item.amount
            tvNutrition.text = "热量: ${item.calories} kcal  |  蛋白质: ${item.protein}g  |  碳水: ${item.carbs}g  |  脂肪: ${item.fat}g"
        }
    }
}

/**
 * 食物显示项（只读）
 */
data class FoodDisplayItem(
    val mealType: String,
    val foodName: String,
    val amount: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val isEstimated: Boolean
)
