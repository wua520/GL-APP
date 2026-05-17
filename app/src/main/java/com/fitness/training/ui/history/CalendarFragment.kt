package com.fitness.training.ui.history

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.Exercise
import com.fitness.training.data.entity.Workout
import com.fitness.training.data.entity.WorkoutExercise
import com.fitness.training.data.entity.WorkoutSet
import com.fitness.training.data.repository.WorkoutRepository
import com.fitness.training.data.repository.WorkoutWithDetails
import com.fitness.training.util.UserSession
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var viewModel: HistoryViewModel
    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var recyclerCalendar: RecyclerView
    
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var repository: WorkoutRepository
    
    private val currentCalendar = Calendar.getInstance()
    private var currentDialog: Dialog? = null
    
    private fun getCurrentUserId(): Long = UserSession.getCurrentUserId(requireContext())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        
        viewModel = ViewModelProvider(requireParentFragment())[HistoryViewModel::class.java]
        
        val db = FitnessDatabase.getDatabase(requireContext())
        repository = WorkoutRepository(
            db.workoutDao(),
            db.workoutExerciseDao(),
            db.workoutSetDao(),
            db.exerciseDao()
        )
        
        initViews(view)
        setupCalendar()
        setupListeners()
        observeData()
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadWorkoutDates()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        currentDialog?.dismiss()
    }
    
    private fun initViews(view: View) {
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
        recyclerCalendar = view.findViewById(R.id.recycler_calendar)
    }
    
    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { day ->
            if (day.dayOfMonth > 0) {
                val cal = Calendar.getInstance()
                cal.set(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), day.dayOfMonth)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                showDayDialog(cal.timeInMillis, day.dayOfMonth)
            }
        }
        
        recyclerCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }
        
        updateCalendar()
    }
    
    private fun setupListeners() {
        btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
            viewModel.loadWorkoutDates()
        }
        
        btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
            viewModel.loadWorkoutDates()
        }
    }
    
    private fun observeData() {
        viewModel.workoutDates.observe(viewLifecycleOwner) { dates ->
            updateWorkoutIndicators(dates)
        }
    }
    
    private fun updateCalendar() {
        val dateFormat = SimpleDateFormat("yyyy年M月", Locale.getDefault())
        tvCurrentMonth.text = dateFormat.format(currentCalendar.time)
        
        val days = generateCalendarDays()
        calendarAdapter.submitList(days)
    }
    
    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        
        val cal = Calendar.getInstance()
        cal.set(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), 1)
        
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        if (firstDayOfWeek < 0) firstDayOfWeek = 6
        
        for (i in 0 until firstDayOfWeek) {
            days.add(CalendarDay(0, false))
        }
        
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            days.add(CalendarDay(day, false))
        }
        
        return days
    }
    
    private fun updateWorkoutIndicators(dates: Set<String>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val days = calendarAdapter.currentList.toMutableList()
        
        days.forEachIndexed { index, day ->
            if (day.dayOfMonth > 0) {
                val cal = Calendar.getInstance()
                cal.set(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), day.dayOfMonth)
                val dateStr = dateFormat.format(cal.time)
                days[index] = day.copy(hasWorkout = dates.contains(dateStr))
            }
        }
        
        calendarAdapter.submitList(days)
    }
    
    private fun showDayDialog(dateMillis: Long, dayOfMonth: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_calendar_day, null)
        
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog)
        
        currentDialog = dialog
        
        // 设置标题
        val dateFormat = SimpleDateFormat("M月d日", Locale.getDefault())
        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = dateFormat.format(Date(dateMillis))
        
        // 关闭按钮
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }
        
        // 开始训练按钮
        dialogView.findViewById<MaterialButton>(R.id.btn_start_training).setOnClickListener {
            dialog.dismiss()
            // 导航到训练页面，传递选中的日期
            navigateToTraining(dateMillis)
        }
        
        // 加载当日训练记录
        val recyclerWorkouts = dialogView.findViewById<RecyclerView>(R.id.recycler_workouts)
        val tvWorkoutsLabel = dialogView.findViewById<TextView>(R.id.tv_workouts_label)
        val tvNoWorkout = dialogView.findViewById<TextView>(R.id.tv_no_workout)
        
        val workoutAdapter = CalendarWorkoutAdapter(
            onViewClick = { workout ->
                dialog.dismiss()
                showWorkoutDetailDialog(workout)
            },
            onEditClick = { workout ->
                dialog.dismiss()
                navigateToEditWorkout(workout)
            },
            onDeleteClick = { workout ->
                confirmDelete(workout, dialog)
            }
        )
        
        recyclerWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
        
        // 加载数据
        lifecycleScope.launch {
            val cal = Calendar.getInstance()
            cal.timeInMillis = dateMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = cal.timeInMillis
            
            val workouts = repository.getWorkoutsByDateRange(getCurrentUserId(), startOfDay, endOfDay).first()
                .filter { it.duration > 0 }
            
            if (workouts.isEmpty()) {
                tvWorkoutsLabel.visibility = View.GONE
                recyclerWorkouts.visibility = View.GONE
                tvNoWorkout.visibility = View.VISIBLE
            } else {
                tvWorkoutsLabel.visibility = View.VISIBLE
                recyclerWorkouts.visibility = View.VISIBLE
                tvNoWorkout.visibility = View.GONE
                
                val items = workouts.map { workout ->
                    val details = repository.getWorkoutWithDetails(workout.id)
                    val exerciseCount = details?.exercises?.size ?: 0
                    var totalVolume = 0.0
                    details?.exercises?.forEach { ex ->
                        ex.sets.forEach { set ->
                            totalVolume += set.weight * set.reps
                        }
                    }
                    CalendarWorkoutItem(workout, exerciseCount, totalVolume)
                }
                workoutAdapter.submitList(items)
            }
        }
        
        dialog.show()
    }
    
    private fun navigateToTraining(dateMillis: Long) {
        // 保存选中的日期到SharedPreferences
        val prefs = requireContext().getSharedPreferences("training_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("selected_training_date", dateMillis).apply()
        
        // 导航到训练页面
        try {
            val navController = androidx.navigation.Navigation.findNavController(
                requireActivity(), R.id.nav_host_fragment_activity_main
            )
            navController.navigate(R.id.navigation_training)
        } catch (e: Exception) {
            // 如果导航失败，尝试其他方式
        }
    }
    
    private fun navigateToEditWorkout(workout: Workout) {
        lifecycleScope.launch {
            val details = repository.getWorkoutWithDetails(workout.id) ?: return@launch
            showEditWorkoutDialog(workout, details)
        }
    }
    
    private fun showEditWorkoutDialog(workout: Workout, details: WorkoutWithDetails) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_workout)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        val etWorkoutName = dialog.findViewById<TextInputEditText>(R.id.et_workout_name)
        val etDuration = dialog.findViewById<TextInputEditText>(R.id.et_duration)
        val recyclerExercises = dialog.findViewById<RecyclerView>(R.id.recycler_exercises)
        val btnAddExercise = dialog.findViewById<MaterialButton>(R.id.btn_add_exercise)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btn_save)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btn_close)
        
        etWorkoutName.setText(workout.name)
        val durationMinutes = workout.duration / 60000
        etDuration.setText(durationMinutes.toString())
        
        // 转换数据为可编辑格式
        val editExercises = details.exercises.map { ex ->
            EditExerciseItem(
                exerciseId = ex.workoutExercise.exerciseId,
                exerciseName = ex.exercise?.name ?: "未知动作",
                sets = ex.sets.map { set ->
                    EditSetItem(set.setNumber, set.weight, set.reps)
                }.toMutableList()
            )
        }.toMutableList()
        
        val editAdapter = EditWorkoutAdapter(
            onDeleteExercise = { position ->
                if (editExercises.size > 1) {
                    editExercises.removeAt(position)
                    recyclerExercises.adapter?.notifyDataSetChanged()
                }
            },
            onDataChanged = { }
        )
        
        recyclerExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = editAdapter
        }
        editAdapter.submitList(editExercises.toList())
        
        btnAddExercise.setOnClickListener {
            showSelectExerciseDialog { selectedExercises ->
                selectedExercises.forEach { exercise ->
                    editExercises.add(EditExerciseItem(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        sets = mutableListOf(EditSetItem(1, 0.0, 10))
                    ))
                }
                editAdapter.submitList(editExercises.toList())
            }
        }
        
        btnSave.setOnClickListener {
            lifecycleScope.launch {
                val newDuration = (etDuration.text.toString().toLongOrNull() ?: 0) * 60000
                saveWorkoutChanges(workout, etWorkoutName.text.toString(), newDuration, editExercises)
                viewModel.loadWorkoutDates()
                dialog.dismiss()
            }
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showSelectExerciseDialog(onSelected: (List<Exercise>) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_exercise, null)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        val etSearch = dialogView.findViewById<EditText>(R.id.et_search)
        val chipGroupEquipment = dialogView.findViewById<ChipGroup>(R.id.chip_group_equipment)
        val rvMuscleGroups = dialogView.findViewById<RecyclerView>(R.id.rv_muscle_groups)
        val recyclerExercises = dialogView.findViewById<RecyclerView>(R.id.recycler_exercises)
        val tvSelectedCount = dialogView.findViewById<TextView>(R.id.tv_selected_count)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm)
        
        val selectedExercises = mutableListOf<Exercise>()
        var allExercises = listOf<Exercise>()
        var currentMuscleGroup: String? = null
        var currentSubGroup: String? = null
        var currentEquipment: String? = null
        var searchQuery = ""
        
        val selectAdapter = ExerciseSelectAdapter { exercise, isSelected ->
            if (isSelected) {
                selectedExercises.add(exercise)
            } else {
                selectedExercises.remove(exercise)
            }
            tvSelectedCount.text = "已选 ${selectedExercises.size} 个"
        }
        
        fun filterExercises() {
            var exercises = allExercises
            
            if (searchQuery.isNotEmpty()) {
                exercises = exercises.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            
            // 收藏筛选
            if (currentMuscleGroup == "★ 收藏") {
                exercises = exercises.filter { it.isFavorite }
            }
            // 肌群过滤
            else if (currentMuscleGroup != null && currentMuscleGroup != "全部") {
                exercises = exercises.filter { exercise ->
                    exercise.muscleGroup == currentMuscleGroup ||
                    exercise.muscleGroup.contains(currentMuscleGroup!!) ||
                    currentMuscleGroup!!.contains(exercise.muscleGroup.replace("部", ""))
                }
                if (currentSubGroup != null) {
                    exercises = exercises.filter { it.subMuscleGroup == currentSubGroup }
                }
            }
            
            if (currentEquipment != null) {
                exercises = exercises.filter { it.equipment == currentEquipment }
            }
            
            selectAdapter.submitList(exercises)
        }
        
        // 设置肌群列表
        val muscleGroupAdapter = com.fitness.training.ui.training.SelectMuscleGroupAdapter { group, subGroup ->
            currentMuscleGroup = group
            currentSubGroup = subGroup
            filterExercises()
        }
        rvMuscleGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = muscleGroupAdapter
        }
        
        recyclerExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectAdapter
        }
        
        // 加载动作列表
        lifecycleScope.launch {
            allExercises = repository.getAllExercises().first()
            filterExercises()
        }
        
        // 搜索
        etSearch.addTextChangedListener { text ->
            searchQuery = text.toString()
            filterExercises()
        }
        
        // 器械筛选
        chipGroupEquipment.setOnCheckedStateChangeListener { _, checkedIds ->
            currentEquipment = if (checkedIds.isEmpty() || checkedIds.first() == R.id.chip_all_equipment) {
                null
            } else {
                dialogView.findViewById<com.google.android.material.chip.Chip>(checkedIds.first())?.text?.toString()
            }
            filterExercises()
        }
        
        btnConfirm.setOnClickListener {
            onSelected(selectedExercises.toList())
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private suspend fun saveWorkoutChanges(
        workout: Workout, 
        newName: String,
        newDuration: Long,
        exercises: List<EditExerciseItem>
    ) {
        // 更新训练名称和时长
        val updatedWorkout = workout.copy(name = newName, duration = newDuration)
        repository.updateWorkout(updatedWorkout)
        
        // 删除旧的动作和组数据
        val oldExercises = repository.getExercisesByWorkout(workout.id).first()
        oldExercises.forEach { we ->
            val oldSets = repository.getSetsByWorkoutExercise(we.id).first()
            oldSets.forEach { set ->
                repository.deleteSet(set)
            }
            repository.deleteWorkoutExercise(we)
        }
        
        // 插入新的动作和组数据
        exercises.forEachIndexed { index, editExercise ->
            val workoutExercise = WorkoutExercise(
                workoutId = workout.id,
                exerciseId = editExercise.exerciseId,
                order = index,
                supersetGroupId = null
            )
            val weId = repository.insertWorkoutExercise(workoutExercise)
            
            editExercise.sets.forEach { editSet ->
                val workoutSet = WorkoutSet(
                    workoutExerciseId = weId,
                    setNumber = editSet.setNumber,
                    weight = editSet.weight,
                    reps = editSet.reps,
                    isCompleted = true
                )
                repository.insertSet(workoutSet)
            }
        }
    }
    
    private fun showWorkoutDetailDialog(workout: Workout) {
        lifecycleScope.launch {
            val details = repository.getWorkoutWithDetails(workout.id) ?: return@launch
            
            val dialogView = layoutInflater.inflate(R.layout.dialog_workout_detail, null)
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            
            dialogView.findViewById<TextView>(R.id.tv_title).text = 
                if (workout.name.isNotEmpty()) workout.name else "训练详情"
            dialogView.findViewById<TextView>(R.id.tv_date).text = dateFormat.format(Date(workout.date))
            
            val durationMinutes = workout.duration / 60000
            dialogView.findViewById<TextView>(R.id.tv_duration).text = "${durationMinutes}分钟"
            
            val exerciseCount = details.exercises.size
            dialogView.findViewById<TextView>(R.id.tv_exercises).text = exerciseCount.toString()
            
            var totalSets = 0
            var totalVolume = 0.0
            details.exercises.forEach { ex ->
                totalSets += ex.sets.size
                ex.sets.forEach { set ->
                    totalVolume += set.weight * set.reps
                }
            }
            
            dialogView.findViewById<TextView>(R.id.tv_sets).text = totalSets.toString()
            dialogView.findViewById<TextView>(R.id.tv_volume).text = 
                if (totalVolume >= 1000) String.format("%.0fkg", totalVolume) 
                else if (totalVolume == totalVolume.toLong().toDouble()) String.format("%.0fkg", totalVolume)
                else String.format("%.1fkg", totalVolume)
            
            val layoutNotes = dialogView.findViewById<LinearLayout>(R.id.layout_notes)
            val tvNotes = dialogView.findViewById<TextView>(R.id.tv_notes)
            if (workout.notes.isNotEmpty()) {
                layoutNotes.visibility = View.VISIBLE
                tvNotes.text = workout.notes
            } else {
                layoutNotes.visibility = View.GONE
            }
            
            val llExercises = dialogView.findViewById<LinearLayout>(R.id.ll_exercises)
            llExercises.removeAllViews()
            
            details.exercises.forEach { exerciseDetail ->
                val exerciseView = layoutInflater.inflate(R.layout.item_detail_exercise, llExercises, false)
                
                val exerciseName = exerciseDetail.exercise?.name ?: "未知动作"
                exerciseView.findViewById<TextView>(R.id.tv_exercise_name).text = exerciseName
                
                val setsText = exerciseDetail.sets.joinToString("\n") { set ->
                    "第${set.setNumber}组: ${set.weight}kg × ${set.reps}次"
                }
                exerciseView.findViewById<TextView>(R.id.tv_sets_detail).text = setsText
                
                llExercises.addView(exerciseView)
            }
            
            val detailDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create()
            
            dialogView.findViewById<View>(R.id.btn_close).setOnClickListener {
                detailDialog.dismiss()
            }
            
            detailDialog.show()
        }
    }
    
    private fun confirmDelete(workout: Workout, parentDialog: Dialog) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除训练记录")
            .setMessage("确定要删除这条训练记录吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteWorkout(workout)
                viewModel.loadWorkoutDates()
                parentDialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

data class CalendarDay(
    val dayOfMonth: Int,
    val hasWorkout: Boolean
)
