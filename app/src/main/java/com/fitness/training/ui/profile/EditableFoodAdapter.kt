package com.fitness.training.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton

/**
 * 可编辑食物列表适配器
 */
class EditableFoodAdapter(
    private val items: MutableList<EditableFoodItem>,
    private val onDeleteItem: (Int) -> Unit
) : RecyclerView.Adapter<EditableFoodAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_food_editable, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    fun getItems(): List<EditableFoodItem> {
        currentViewHolders().forEach { it.syncItemFromViews() }
        return items
    }

    private fun currentViewHolders(): List<ViewHolder> =
        (0 until itemCount).mapNotNull { position ->
            (recyclerView?.findViewHolderForAdapterPosition(position) as? ViewHolder)
        }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chipGroupMeal: ChipGroup = itemView.findViewById(R.id.chip_group_meal)
        private val chipBreakfast: Chip = itemView.findViewById(R.id.chip_breakfast)
        private val chipLunch: Chip = itemView.findViewById(R.id.chip_lunch)
        private val chipDinner: Chip = itemView.findViewById(R.id.chip_dinner)
        private val chipSnack: Chip = itemView.findViewById(R.id.chip_snack)
        private val etFoodName: TextInputEditText = itemView.findViewById(R.id.et_food_name)
        private val etAmount: TextInputEditText = itemView.findViewById(R.id.et_amount)
        private val etCalories: TextInputEditText = itemView.findViewById(R.id.et_calories)
        private val etProtein: TextInputEditText = itemView.findViewById(R.id.et_protein)
        private val etCarbs: TextInputEditText = itemView.findViewById(R.id.et_carbs)
        private val etFat: TextInputEditText = itemView.findViewById(R.id.et_fat)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)

        fun syncItemFromViews() {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return

            items[position].apply {
                foodName = etFoodName.text.toString()
                amount = etAmount.text.toString()
                calories = etCalories.text.toString().toIntOrNull() ?: 0
                protein = etProtein.text.toString().toFloatOrNull() ?: 0f
                carbs = etCarbs.text.toString().toFloatOrNull() ?: 0f
                fat = etFat.text.toString().toFloatOrNull() ?: 0f
            }
        }

        fun bind(item: EditableFoodItem, position: Int) {
            // 设置餐次选择
            when (item.mealType) {
                "早餐" -> chipBreakfast.isChecked = true
                "午餐" -> chipLunch.isChecked = true
                "晚餐" -> chipDinner.isChecked = true
                "加餐" -> chipSnack.isChecked = true
                else -> chipBreakfast.isChecked = true
            }
            
            etFoodName.setText(item.foodName)
            etAmount.setText(item.amount)
            etCalories.setText(if (item.calories > 0) item.calories.toString() else "")
            etProtein.setText(if (item.protein > 0) item.protein.toString() else "")
            etCarbs.setText(if (item.carbs > 0) item.carbs.toString() else "")
            etFat.setText(if (item.fat > 0) item.fat.toString() else "")

            // 监听餐次选择变化
            chipGroupMeal.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    item.mealType = when (checkedIds[0]) {
                        R.id.chip_breakfast -> "早餐"
                        R.id.chip_lunch -> "午餐"
                        R.id.chip_dinner -> "晚餐"
                        R.id.chip_snack -> "加餐"
                        else -> "早餐"
                    }
                }
            }

            // 监听输入变化
            etFoodName.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.foodName = etFoodName.text.toString()
            }
            etAmount.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.amount = etAmount.text.toString()
            }
            etCalories.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.calories = etCalories.text.toString().toIntOrNull() ?: 0
            }
            etProtein.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.protein = etProtein.text.toString().toFloatOrNull() ?: 0f
            }
            etCarbs.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.carbs = etCarbs.text.toString().toFloatOrNull() ?: 0f
            }
            etFat.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.fat = etFat.text.toString().toFloatOrNull() ?: 0f
            }

            btnDelete.setOnClickListener {
                onDeleteItem(position)
            }
        }
    }
}

/**
 * 可编辑食物项
 */
data class EditableFoodItem(
    var mealType: String,
    var foodName: String,
    var amount: String,
    var calories: Int,
    var protein: Float,
    var carbs: Float,
    var fat: Float,
    val isEstimated: Boolean = false
)
