package com.fitness.training.ui.diet

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Food
import com.google.android.material.button.MaterialButton

class AddFoodFragment : Fragment() {

    private lateinit var viewModel: DietViewModel
    
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var layoutAddCustom: View
    private lateinit var btnAddCustom: View
    private lateinit var etSearch: EditText
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var recyclerFoods: RecyclerView
    
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var foodAdapter: FoodListAdapter
    
    private var mealType: String = "早餐"
    private var selectedDate: Long = System.currentTimeMillis()
    private var allFoods: List<Food> = emptyList()
    
    private val categories = listOf("全部", "自定义", "主食", "肉类", "海鲜", "蛋奶", "蔬菜", "水果", "豆类", "坚果", "补剂", "饮品", "零食", "调味")

    companion object {
        private const val ARG_MEAL_TYPE = "meal_type"
        private const val ARG_SELECTED_DATE = "selected_date"
        
        fun newInstance(mealType: String, selectedDate: Long = System.currentTimeMillis()): AddFoodFragment {
            return AddFoodFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEAL_TYPE, mealType)
                    putLong(ARG_SELECTED_DATE, selectedDate)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_food, container, false)
        
        mealType = arguments?.getString(ARG_MEAL_TYPE) ?: "早餐"
        selectedDate = arguments?.getLong(ARG_SELECTED_DATE) ?: System.currentTimeMillis()
        viewModel = ViewModelProvider(this)[DietViewModel::class.java]
        viewModel.setDate(selectedDate)
        
        initViews(view)
        setupAdapters()
        setupListeners()
        loadFoods()
        
        return view
    }
    
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back)
        tvTitle = view.findViewById(R.id.tv_title)
        layoutAddCustom = view.findViewById(R.id.layout_add_custom)
        btnAddCustom = view.findViewById(R.id.btn_add_custom)
        etSearch = view.findViewById(R.id.et_search)
        recyclerCategories = view.findViewById(R.id.recycler_categories)
        recyclerFoods = view.findViewById(R.id.recycler_foods)
        
        tvTitle.text = mealType
    }
    
    private fun setupAdapters() {
        categoryAdapter = CategoryAdapter(categories) { category ->
            filterByCategory(category)
        }
        recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        recyclerCategories.adapter = categoryAdapter
        
        foodAdapter = FoodListAdapter(
            onAddClick = { food -> showAddConfirmDialog(food) },
            onDeleteClick = { food -> showDeleteFoodDialog(food) }
        )
        recyclerFoods.layoutManager = LinearLayoutManager(requireContext())
        recyclerFoods.adapter = foodAdapter
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            goBack()
        }
        
        btnAddCustom.setOnClickListener {
            showAddCustomFoodDialog()
        }
        
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFiltering) return
                val query = s?.toString() ?: ""
                if (query.isNotEmpty()) {
                    val filtered = allFoods.filter { it.name.contains(query, ignoreCase = true) }
                    foodAdapter.submitList(filtered)
                } else {
                    val category = categories.getOrNull(categoryAdapter.selectedPosition) ?: "全部"
                    val filtered = if (category == "全部") {
                        allFoods
                    } else {
                        allFoods.filter { it.category == category }
                    }
                    foodAdapter.submitList(filtered)
                }
            }
        })
    }
    
    private fun loadFoods() {
        viewModel.getAllFoods { foods ->
            allFoods = foods
            foodAdapter.submitList(foods)
        }
    }
    
    private var isFiltering = false
    
    private fun filterByCategory(category: String) {
        if (isFiltering) return
        isFiltering = true
        etSearch.setText("")
        isFiltering = false
        
        // 只在自定义分类显示添加按钮
        layoutAddCustom.visibility = if (category == "自定义") View.VISIBLE else View.GONE
        
        if (category == "自定义") {
            // 显示自定义食物
            val filtered = allFoods.filter { it.isCustom }
            foodAdapter.submitList(filtered)
        } else {
            val filtered = if (category == "全部") {
                allFoods
            } else {
                allFoods.filter { it.category == category }
            }
            foodAdapter.submitList(filtered)
        }
    }
    
    private fun showAddConfirmDialog(food: Food) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_confirm_food, null)
        
        val tvFoodName = dialogView.findViewById<TextView>(R.id.tv_food_name)
        val tvFoodInfo = dialogView.findViewById<TextView>(R.id.tv_food_info)
        val etAmount = dialogView.findViewById<EditText>(R.id.et_amount)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm)
        
        tvFoodName.text = food.name
        
        // 基准值（每100g）
        val baseUnit = food.unit.replace("g", "").toIntOrNull() ?: 100
        val baseCalories = food.calories
        val baseProtein = food.protein
        val baseCarbs = food.carbs
        val baseFat = food.fat
        
        // 更新营养信息显示
        fun updateNutritionInfo(grams: Int) {
            val ratio = grams.toFloat() / baseUnit
            val calories = (baseCalories * ratio).toInt()
            val protein = (baseProtein * ratio).toInt()
            val carbs = (baseCarbs * ratio).toInt()
            val fat = (baseFat * ratio).toInt()
            tvFoodInfo.text = "${calories}千卡 · 蛋白${protein}g · 碳水${carbs}g · 脂肪${fat}g"
        }
        
        // 初始显示100g的营养信息
        updateNutritionInfo(100)
        
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val grams = s?.toString()?.toIntOrNull() ?: 100
                updateNutritionInfo(grams)
            }
        })
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnConfirm.setOnClickListener {
            val grams = etAmount.text.toString().toIntOrNull() ?: 100
            val ratio = grams.toFloat() / baseUnit
            val calories = (baseCalories * ratio).toInt()
            val protein = baseProtein * ratio
            val carbs = baseCarbs * ratio
            val fat = baseFat * ratio
            
            viewModel.addRecord(
                mealType = mealType,
                foodName = food.name,
                amount = "${grams}g",
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat
            )
            Toast.makeText(requireContext(), "已添加到${mealType}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            goBack()
        }
        
        dialog.show()
    }
    
    private fun showAddCustomFoodDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_custom_food, null)
        
        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val etUnit = dialogView.findViewById<EditText>(R.id.et_unit)
        val etCalories = dialogView.findViewById<EditText>(R.id.et_calories)
        val etProtein = dialogView.findViewById<EditText>(R.id.et_protein)
        val etCarbs = dialogView.findViewById<EditText>(R.id.et_carbs)
        val etFat = dialogView.findViewById<EditText>(R.id.et_fat)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入食物名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val unitGrams = etUnit.text.toString().toIntOrNull() ?: 100
            val inputCalories = etCalories.text.toString().toIntOrNull() ?: 0
            val inputProtein = etProtein.text.toString().toFloatOrNull() ?: 0f
            val inputCarbs = etCarbs.text.toString().toFloatOrNull() ?: 0f
            val inputFat = etFat.text.toString().toFloatOrNull() ?: 0f
            
            // 换算成每100g的标准值
            val ratio = 100f / unitGrams
            val calories = (inputCalories * ratio).toInt()
            val protein = inputProtein * ratio
            val carbs = inputCarbs * ratio
            val fat = inputFat * ratio
            
            viewModel.addCustomFood(name, "100g", calories, protein, carbs, fat) {
                Toast.makeText(requireContext(), "食物已保存", Toast.LENGTH_SHORT).show()
                loadFoods() // 刷新列表
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showDeleteFoodDialog(food: Food) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除食物")
            .setMessage("确定要删除「${food.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteCustomFood(food) {
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                    loadFoods()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun goBack() {
        // 通知父Fragment显示主内容
        (parentFragment as? DietFragment)?.showMainContent()
        parentFragmentManager.popBackStack()
    }
}
