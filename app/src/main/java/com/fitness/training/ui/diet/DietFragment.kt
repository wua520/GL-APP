package com.fitness.training.ui.diet

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.DietRecord
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DietFragment : Fragment() {

    private lateinit var viewModel: DietViewModel
    
    private lateinit var btnBack: ImageButton
    private lateinit var btnPrevDay: ImageButton
    private lateinit var btnNextDay: ImageButton
    private lateinit var tvDate: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var tvTotalBurn: TextView
    private lateinit var tvBurnHint: TextView
    private lateinit var tvTotalProtein: TextView
    private lateinit var tvTotalCarbs: TextView
    private lateinit var tvTotalFat: TextView
    private lateinit var cardBreakfast: MaterialCardView
    private lateinit var cardLunch: MaterialCardView
    private lateinit var cardDinner: MaterialCardView
    private lateinit var cardSnack: MaterialCardView
    private lateinit var tvBreakfastCalories: TextView
    private lateinit var tvLunchCalories: TextView
    private lateinit var tvDinnerCalories: TextView
    private lateinit var tvSnackCalories: TextView
    private lateinit var layoutRecords: LinearLayout
    private lateinit var layoutEmpty: View
    private lateinit var scrollContent: ScrollView
    private lateinit var dietContainer: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_diet, container, false)
        
        viewModel = ViewModelProvider(this)[DietViewModel::class.java]
        
        initViews(view)
        setupObservers()
        setupListeners()
        setupBackPressHandler()
        
        return view
    }
    
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back)
        btnPrevDay = view.findViewById(R.id.btn_prev_day)
        btnNextDay = view.findViewById(R.id.btn_next_day)
        tvDate = view.findViewById(R.id.tv_date)
        tvTotalCalories = view.findViewById(R.id.tv_total_calories)
        tvTotalBurn = view.findViewById(R.id.tv_total_burn)
        tvBurnHint = view.findViewById(R.id.tv_burn_hint)
        tvTotalProtein = view.findViewById(R.id.tv_total_protein)
        tvTotalCarbs = view.findViewById(R.id.tv_total_carbs)
        tvTotalFat = view.findViewById(R.id.tv_total_fat)
        cardBreakfast = view.findViewById(R.id.card_breakfast)
        cardLunch = view.findViewById(R.id.card_lunch)
        cardDinner = view.findViewById(R.id.card_dinner)
        cardSnack = view.findViewById(R.id.card_snack)
        tvBreakfastCalories = view.findViewById(R.id.tv_breakfast_calories)
        tvLunchCalories = view.findViewById(R.id.tv_lunch_calories)
        tvDinnerCalories = view.findViewById(R.id.tv_dinner_calories)
        tvSnackCalories = view.findViewById(R.id.tv_snack_calories)
        layoutRecords = view.findViewById(R.id.layout_records)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        scrollContent = view.findViewById(R.id.scroll_content)
        dietContainer = view.findViewById(R.id.diet_container)
    }
    
    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (dietContainer.visibility == View.VISIBLE) {
                    // 如果子页面显示，返回主页面
                    showMainContent()
                    childFragmentManager.popBackStack()
                } else {
                    // 返回上一页
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    private fun setupObservers() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            val sdf = SimpleDateFormat("M月d日", Locale.CHINA)
            tvDate.text = sdf.format(date)
        }
        
        viewModel.todayRecords.observe(viewLifecycleOwner) { records ->
            layoutRecords.removeAllViews()
            
            if (records.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                layoutRecords.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                layoutRecords.visibility = View.VISIBLE
                
                // 按餐次分组显示
                val mealTypes = listOf("早餐", "午餐", "晚餐", "加餐")
                mealTypes.forEach { mealType ->
                    val mealRecords = records.filter { it.mealType == mealType }
                    if (mealRecords.isNotEmpty()) {
                        // 添加餐次标题
                        val headerView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_meal_header, layoutRecords, false)
                        val tvMealTitle = headerView.findViewById<TextView>(R.id.tv_meal_title)
                        val tvMealCalories = headerView.findViewById<TextView>(R.id.tv_meal_calories)
                        
                        val emoji = when (mealType) {
                            "早餐" -> "🍳"
                            "午餐" -> "🍚"
                            "晚餐" -> "🍜"
                            else -> "🍎"
                        }
                        tvMealTitle.text = "$emoji $mealType"
                        val mealCal = mealRecords.sumOf { it.calories }
                        tvMealCalories.text = "${mealCal}千卡"
                        
                        layoutRecords.addView(headerView)
                        
                        // 添加该餐次的食物记录
                        mealRecords.forEach { record ->
                            val itemView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_diet_record, layoutRecords, false)
                            
                            val tvFoodName = itemView.findViewById<TextView>(R.id.tv_food_name)
                            val tvMealInfo = itemView.findViewById<TextView>(R.id.tv_meal_type)
                            val tvCalories = itemView.findViewById<TextView>(R.id.tv_calories)
                            val btnDelete = itemView.findViewById<ImageButton>(R.id.btn_delete)
                            
                            tvFoodName.text = record.foodName
                            tvMealInfo.text = record.amount
                            tvCalories.text = "${record.calories}千卡"
                            
                            btnDelete.setOnClickListener {
                                showDeleteDialog(record)
                            }
                            
                            layoutRecords.addView(itemView)
                        }
                    }
                }
            }
            
            // 更新各餐次热量
            val breakfastCal = records.filter { it.mealType == "早餐" }.sumOf { it.calories }
            val lunchCal = records.filter { it.mealType == "午餐" }.sumOf { it.calories }
            val dinnerCal = records.filter { it.mealType == "晚餐" }.sumOf { it.calories }
            val snackCal = records.filter { it.mealType == "加餐" }.sumOf { it.calories }
            
            tvBreakfastCalories.text = "${breakfastCal}千卡"
            tvLunchCalories.text = "${lunchCal}千卡"
            tvDinnerCalories.text = "${dinnerCal}千卡"
            tvSnackCalories.text = "${snackCal}千卡"
        }
        
        viewModel.totalCalories.observe(viewLifecycleOwner) { calories ->
            tvTotalCalories.text = "$calories"
        }
        
        viewModel.totalBurn.observe(viewLifecycleOwner) { burn ->
            tvTotalBurn.text = "$burn"
        }
        
        viewModel.needBodyData.observe(viewLifecycleOwner) { needData ->
            if (needData) {
                tvTotalBurn.text = "--"
                tvBurnHint.text = "请先填写身体数据"
                tvBurnHint.setTextColor(resources.getColor(R.color.primary, null))
            } else {
                tvBurnHint.text = "千卡"
                tvBurnHint.setTextColor(resources.getColor(R.color.text_hint, null))
            }
        }
        
        viewModel.totalProtein.observe(viewLifecycleOwner) { protein ->
            tvTotalProtein.text = "${protein.toInt()}g"
        }
        
        viewModel.totalCarbs.observe(viewLifecycleOwner) { carbs ->
            tvTotalCarbs.text = "${carbs.toInt()}g"
        }
        
        viewModel.totalFat.observe(viewLifecycleOwner) { fat ->
            tvTotalFat.text = "${fat.toInt()}g"
        }
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnPrevDay.setOnClickListener {
            val current = viewModel.selectedDate.value ?: System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = current }
            cal.add(Calendar.DAY_OF_MONTH, -1)
            viewModel.setDate(cal.timeInMillis)
        }
        
        btnNextDay.setOnClickListener {
            val current = viewModel.selectedDate.value ?: System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = current }
            cal.add(Calendar.DAY_OF_MONTH, 1)
            viewModel.setDate(cal.timeInMillis)
        }
        
        tvDate.setOnClickListener {
            showDatePicker()
        }
        
        cardBreakfast.setOnClickListener { openAddFood("早餐") }
        cardLunch.setOnClickListener { openAddFood("午餐") }
        cardDinner.setOnClickListener { openAddFood("晚餐") }
        cardSnack.setOnClickListener { openAddFood("加餐") }
    }
    
    private fun showDatePicker() {
        val current = viewModel.selectedDate.value ?: System.currentTimeMillis()
        val selectedCal = Calendar.getInstance().apply { timeInMillis = current }
        val displayCal = Calendar.getInstance().apply { timeInMillis = current }
        
        viewModel.getDatesWithRecords { datesWithRecords ->
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_diet_calendar, null)
            
            val btnPrevMonth = dialogView.findViewById<ImageButton>(R.id.btn_prev_month)
            val btnNextMonth = dialogView.findViewById<ImageButton>(R.id.btn_next_month)
            val tvMonth = dialogView.findViewById<TextView>(R.id.tv_month)
            val recyclerCalendar = dialogView.findViewById<RecyclerView>(R.id.recycler_calendar)
            
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()
            
            val adapter = DietCalendarAdapter { day ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, day.year)
                    set(Calendar.MONTH, day.month)
                    set(Calendar.DAY_OF_MONTH, day.day)
                }
                viewModel.setDate(newCal.timeInMillis)
                dialog.dismiss()
            }
            
            recyclerCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
            recyclerCalendar.adapter = adapter
            
            fun updateCalendar() {
                val sdf = SimpleDateFormat("yyyy年M月", Locale.CHINA)
                tvMonth.text = sdf.format(displayCal.time)
                
                val days = generateCalendarDays(
                    displayCal.get(Calendar.YEAR),
                    displayCal.get(Calendar.MONTH),
                    selectedCal,
                    datesWithRecords
                )
                adapter.submitList(days)
            }
            
            btnPrevMonth.setOnClickListener {
                displayCal.add(Calendar.MONTH, -1)
                updateCalendar()
            }
            
            btnNextMonth.setOnClickListener {
                displayCal.add(Calendar.MONTH, 1)
                updateCalendar()
            }
            
            updateCalendar()
            dialog.show()
        }
    }
    
    private fun generateCalendarDays(
        year: Int,
        month: Int,
        selectedCal: Calendar,
        datesWithRecords: Set<String>
    ): List<DietCalendarDay> {
        val days = mutableListOf<DietCalendarDay>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        val todayCal = Calendar.getInstance()
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // 填充月初空白
        for (i in 0 until firstDayOfWeek) {
            days.add(DietCalendarDay(0, year, month, false, false, false, false))
        }
        
        // 填充日期
        for (day in 1..daysInMonth) {
            val dateKey = "$year-$month-$day"
            val hasRecord = datesWithRecords.contains(dateKey)
            val isSelected = selectedCal.get(Calendar.YEAR) == year &&
                    selectedCal.get(Calendar.MONTH) == month &&
                    selectedCal.get(Calendar.DAY_OF_MONTH) == day
            val isToday = todayCal.get(Calendar.YEAR) == year &&
                    todayCal.get(Calendar.MONTH) == month &&
                    todayCal.get(Calendar.DAY_OF_MONTH) == day
            
            days.add(DietCalendarDay(day, year, month, true, hasRecord, isSelected, isToday))
        }
        
        return days
    }
    
    private fun openAddFood(mealType: String) {
        val selectedDate = viewModel.selectedDate.value ?: System.currentTimeMillis()
        val fragment = AddFoodFragment.newInstance(mealType, selectedDate)
        
        // 显示子页面容器，隐藏主内容
        scrollContent.visibility = View.GONE
        dietContainer.visibility = View.VISIBLE
        
        childFragmentManager.beginTransaction()
            .replace(R.id.diet_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    fun showMainContent() {
        scrollContent.visibility = View.VISIBLE
        dietContainer.visibility = View.GONE
        viewModel.loadTodayRecords()
    }
    
    private fun showDeleteDialog(record: DietRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除「${record.foodName}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteRecord(record)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
