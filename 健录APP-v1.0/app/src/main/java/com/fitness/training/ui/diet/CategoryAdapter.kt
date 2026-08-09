package com.fitness.training.ui.diet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R

class CategoryAdapter(
    private val categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    var selectedPosition = 0
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedPosition)
    }

    override fun getItemCount() = categories.size

    fun setSelected(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(position)
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val indicator: View = itemView.findViewById(R.id.indicator)

        fun bind(category: String, isSelected: Boolean) {
            tvCategory.text = category
            
            if (isSelected) {
                tvCategory.setTextColor(itemView.context.getColor(R.color.primary))
                tvCategory.setTypeface(null, android.graphics.Typeface.BOLD)
                indicator.visibility = View.VISIBLE
                itemView.setBackgroundColor(itemView.context.getColor(R.color.background))
            } else {
                tvCategory.setTextColor(itemView.context.getColor(R.color.text_secondary))
                tvCategory.setTypeface(null, android.graphics.Typeface.NORMAL)
                indicator.visibility = View.INVISIBLE
                itemView.setBackgroundColor(itemView.context.getColor(R.color.surface))
            }
            
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos != selectedPosition) {
                    setSelected(pos)
                    onCategoryClick(category)
                }
            }
        }
    }
}
