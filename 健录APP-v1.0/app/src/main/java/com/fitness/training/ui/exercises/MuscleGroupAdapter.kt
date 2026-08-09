package com.fitness.training.ui.exercises

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R

data class MuscleGroup(
    val name: String,
    val subGroups: List<String> = emptyList()
)

class MuscleGroupAdapter(
    private val onGroupClick: (String, String?) -> Unit // (主分类, 子分类)
) : RecyclerView.Adapter<MuscleGroupAdapter.ViewHolder>() {

    private val groups = listOf(
        MuscleGroup("★ 收藏"),
        MuscleGroup("★ 自定义"),
        MuscleGroup("胸", listOf("上胸", "中下胸")),
        MuscleGroup("背", listOf("背阔肌", "斜方肌中下", "竖脊肌")),
        MuscleGroup("腿", listOf("股四头", "股二头", "臀大肌")),
        MuscleGroup("肩", listOf("前束", "中束", "后束")),
        MuscleGroup("斜方肌"),
        MuscleGroup("二头肌"),
        MuscleGroup("三头肌"),
        MuscleGroup("小腿"),
        MuscleGroup("核心")
    )

    private var selectedGroup: String = "胸"
    private var selectedSubGroup: String? = null
    private var expandedGroup: String? = "胸"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_muscle_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGroupName: TextView = itemView.findViewById(R.id.tv_group_name)
        private val layoutSubGroups: LinearLayout = itemView.findViewById(R.id.layout_sub_groups)

        fun bind(group: MuscleGroup) {
            val context = itemView.context
            tvGroupName.text = group.name
            
            // 选中状态
            val isSelected = group.name == selectedGroup && selectedSubGroup == null
            tvGroupName.setTextColor(
                ContextCompat.getColor(context, 
                    if (isSelected) R.color.primary else R.color.text_secondary)
            )
            tvGroupName.setBackgroundColor(
                if (isSelected) ContextCompat.getColor(context, R.color.background)
                else ContextCompat.getColor(context, android.R.color.transparent)
            )

            // 子分类
            layoutSubGroups.removeAllViews()
            if (group.subGroups.isNotEmpty() && expandedGroup == group.name) {
                layoutSubGroups.visibility = View.VISIBLE
                group.subGroups.forEach { subGroup ->
                    val subView = TextView(context).apply {
                        text = subGroup
                        textSize = 12f
                        setPadding(0, 24, 0, 24)
                        gravity = android.view.Gravity.CENTER
                        val isSubSelected = selectedGroup == group.name && selectedSubGroup == subGroup
                        setTextColor(
                            ContextCompat.getColor(context,
                                if (isSubSelected) R.color.primary else R.color.text_hint)
                        )
                        setBackgroundColor(
                            if (isSubSelected) ContextCompat.getColor(context, R.color.surface)
                            else ContextCompat.getColor(context, android.R.color.transparent)
                        )
                        setOnClickListener {
                            selectedGroup = group.name
                            selectedSubGroup = subGroup
                            notifyDataSetChanged()
                            onGroupClick(group.name, subGroup)
                        }
                    }
                    layoutSubGroups.addView(subView)
                }
            } else {
                layoutSubGroups.visibility = View.GONE
            }

            // 点击主分类
            tvGroupName.setOnClickListener {
                if (group.subGroups.isNotEmpty()) {
                    expandedGroup = if (expandedGroup == group.name) null else group.name
                }
                selectedGroup = group.name
                selectedSubGroup = null
                notifyDataSetChanged()
                onGroupClick(group.name, null)
            }
        }
    }
}
