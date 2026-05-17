package com.fitness.training.ui.training

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R

data class SelectMuscleGroup(
    val name: String,
    val displayName: String,
    val subGroups: List<String> = emptyList()
)

class SelectMuscleGroupAdapter(
    private val onGroupClick: (String, String?) -> Unit
) : RecyclerView.Adapter<SelectMuscleGroupAdapter.ViewHolder>() {

    private val groups = listOf(
        SelectMuscleGroup("全部", "全部"),
        SelectMuscleGroup("★ 收藏", "★ 收藏"),
        SelectMuscleGroup("胸部", "胸", listOf("上胸", "中下胸")),
        SelectMuscleGroup("背部", "背", listOf("背阔肌", "斜方肌中下", "竖脊肌")),
        SelectMuscleGroup("腿部", "腿", listOf("股四头", "股二头", "臀大肌")),
        SelectMuscleGroup("肩部", "肩", listOf("前束", "中束", "后束")),
        SelectMuscleGroup("斜方肌", "斜方肌"),
        SelectMuscleGroup("二头肌", "二头"),
        SelectMuscleGroup("三头肌", "三头"),
        SelectMuscleGroup("小腿", "小腿"),
        SelectMuscleGroup("核心", "核心")
    )

    private var selectedGroup: String = "全部"
    private var selectedSubGroup: String? = null
    private var expandedGroup: String? = null

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

        fun bind(group: SelectMuscleGroup) {
            tvGroupName.text = group.displayName
            
            val isSelected = group.name == selectedGroup && selectedSubGroup == null
            tvGroupName.setTextColor(
                if (isSelected) Color.parseColor("#2196F3") 
                else Color.parseColor("#666666")
            )
            tvGroupName.setBackgroundColor(
                if (isSelected) Color.parseColor("#E3F2FD")
                else Color.TRANSPARENT
            )

            layoutSubGroups.removeAllViews()
            if (group.subGroups.isNotEmpty() && expandedGroup == group.name) {
                layoutSubGroups.visibility = View.VISIBLE
                group.subGroups.forEach { subGroup ->
                    val subView = TextView(itemView.context).apply {
                        text = subGroup
                        textSize = 12f
                        setPadding(0, 20, 0, 20)
                        gravity = android.view.Gravity.CENTER
                        val isSubSelected = selectedGroup == group.name && selectedSubGroup == subGroup
                        setTextColor(
                            if (isSubSelected) Color.parseColor("#2196F3")
                            else Color.parseColor("#888888")
                        )
                        setBackgroundColor(
                            if (isSubSelected) Color.parseColor("#E3F2FD")
                            else Color.TRANSPARENT
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
