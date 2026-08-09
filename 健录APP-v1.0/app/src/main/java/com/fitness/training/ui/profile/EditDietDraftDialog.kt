package com.fitness.training.ui.profile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 编辑饮食草案对话框
 */
class EditDietDraftDialog(
    private val date: String,
    private val foodItems: List<EditableFoodItem>,
    private val onSave: (List<EditableFoodItem>) -> Unit
) : DialogFragment() {

    private lateinit var rvEditableFoodList: RecyclerView
    private lateinit var adapter: EditableFoodAdapter
    private val editableItems = foodItems.toMutableList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_diet_draft, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvEditDate = view.findViewById<TextView>(R.id.tv_edit_date)
        rvEditableFoodList = view.findViewById(R.id.rv_editable_food_list)
        val btnAddFood = view.findViewById<MaterialButton>(R.id.btn_add_food)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_dialog_cancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_dialog_save)

        tvEditDate.text = date

        adapter = EditableFoodAdapter(editableItems) { position ->
            editableItems.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, editableItems.size)
        }
        rvEditableFoodList.layoutManager = LinearLayoutManager(requireContext())
        rvEditableFoodList.adapter = adapter
        rvEditableFoodList.itemAnimator = null

        // 添加食物
        btnAddFood.setOnClickListener {
            val newItem = EditableFoodItem(
                mealType = "早餐",
                foodName = "",
                amount = "",
                calories = 0,
                protein = 0f,
                carbs = 0f,
                fat = 0f
            )
            editableItems.add(newItem)
            adapter.notifyItemInserted(editableItems.size - 1)
            rvEditableFoodList.smoothScrollToPosition(editableItems.size - 1)
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            adapter.getItems()
            val validationError = validateItems()
            if (validationError != null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("输入错误")
                    .setMessage(validationError)
                    .setPositiveButton("确定", null)
                    .show()
                return@setOnClickListener
            }

            // 保存
            onSave(adapter.getItems())
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(R.drawable.bg_dialog)
        }
    }

    /**
     * 校验输入数据
     */
    private fun validateItems(): String? {
        if (editableItems.isEmpty()) {
            return "至少需要一条食物记录"
        }

        for ((index, item) in editableItems.withIndex()) {
            if (item.mealType.isBlank()) {
                return "第${index + 1}项：餐次不能为空"
            }
            if (item.foodName.isBlank()) {
                return "第${index + 1}项：食物名称不能为空"
            }
            if (item.calories < 0) {
                return "第${index + 1}项：热量不能为负数"
            }
            if (item.protein < 0) {
                return "第${index + 1}项：蛋白质不能为负数"
            }
            if (item.carbs < 0) {
                return "第${index + 1}项：碳水不能为负数"
            }
            if (item.fat < 0) {
                return "第${index + 1}项：脂肪不能为负数"
            }
        }

        return null
    }
}
