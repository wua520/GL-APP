package com.fitness.training.ui.training

import android.app.AlertDialog
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Exercise
import com.fitness.training.data.entity.WorkoutSet
import com.fitness.training.data.entity.WorkoutTemplate
import com.fitness.training.utils.AnimationUtils.addClickScaleEffect
import com.fitness.training.utils.AnimationUtils.animateNumberChange
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class TrainingFragment : Fragment() {

    private lateinit var viewModel: TrainingViewModel
    
    private lateinit var tvTimer: TextView
    private lateinit var tvTotalSets: TextView
    private lateinit var tvTotalVolume: TextView
    private lateinit var tvExerciseCount: TextView
    private lateinit var btnStartEnd: MaterialButton
    private lateinit var btnTemplate: MaterialButton
    private lateinit var btnPause: MaterialButton
    private lateinit var btnResetTimer: MaterialButton
    private lateinit var btnRestTimer: MaterialButton
    private lateinit var btnNotes: MaterialButton
    private lateinit var btnSaveTemplate: MaterialButton
    private lateinit var layoutTrainingButtons: View
    private lateinit var layoutExercises: LinearLayout
    private lateinit var fabAddExercise: ExtendedFloatingActionButton
    private lateinit var layoutEmpty: View

    
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    
    // 休息计时器相关
    private var restDialog: AlertDialog? = null
    private var restCountDownTimer: CountDownTimer? = null
    private var currentRestDuration = 90 // 默认90秒
    
    // 展开状态
    private val expandedItems = mutableSetOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_training, container, false)
        
        // 使用 Activity 级别的 ViewModel，这样切换 Tab 时状态不会丢失
        viewModel = ViewModelProvider(requireActivity())[TrainingViewModel::class.java]
        
        initViews(view)
        setupObservers()
        setupListeners()
        
        // 如果正在训练中，恢复计时器
        if (viewModel.isTraining.value == true) {
            startTimer()
        }
        
        // 检查是否有从日历传来的日期，自动开始训练
        checkAndStartFromCalendar()
        
        return view
    }
    
    private fun checkAndStartFromCalendar() {
        if (viewModel.isTraining.value == true) return // 已经在训练中
        
        val prefs = requireContext().getSharedPreferences("training_prefs", android.content.Context.MODE_PRIVATE)
        val selectedDate = prefs.getLong("selected_training_date", 0L)
        if (selectedDate > 0) {
            // 清除保存的日期
            prefs.edit().remove("selected_training_date").apply()
            viewModel.startTrainingWithDate(selectedDate)
        }
    }
    
    private fun initViews(view: View) {
        tvTimer = view.findViewById(R.id.tv_timer)
        tvTotalSets = view.findViewById(R.id.tv_total_sets)
        tvTotalVolume = view.findViewById(R.id.tv_total_volume)
        tvExerciseCount = view.findViewById(R.id.tv_exercise_count)
        btnStartEnd = view.findViewById(R.id.btn_start_end)
        btnTemplate = view.findViewById(R.id.btn_template)
        btnPause = view.findViewById(R.id.btn_pause)
        btnResetTimer = view.findViewById(R.id.btn_reset_timer)
        btnRestTimer = view.findViewById(R.id.btn_rest_timer)
        btnNotes = view.findViewById(R.id.btn_notes)
        btnSaveTemplate = view.findViewById(R.id.btn_save_template)
        layoutTrainingButtons = view.findViewById(R.id.layout_training_buttons)
        layoutExercises = view.findViewById(R.id.layout_exercises)
        fabAddExercise = view.findViewById(R.id.fab_add_exercise)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        
        // 添加按钮点击缩放效果
        btnStartEnd.addClickScaleEffect()
        btnTemplate.addClickScaleEffect()
        btnPause.addClickScaleEffect()
        btnResetTimer.addClickScaleEffect()
        btnRestTimer.addClickScaleEffect()
        btnNotes.addClickScaleEffect()
        btnSaveTemplate.addClickScaleEffect()
        fabAddExercise.addClickScaleEffect()
    }

    
    private var lastExerciseCount = -1
    private var lastTotalSets = -1
    
    private fun setupObservers() {
        viewModel.isTraining.observe(viewLifecycleOwner) { isTraining ->
            updateUIForTrainingState(isTraining)
            if (isTraining) {
                lastExerciseCount = -1
                lastTotalSets = -1
            }
        }
        
        viewModel.isPaused.observe(viewLifecycleOwner) { isPaused ->
            updatePauseButton(isPaused)
        }
        
        viewModel.workoutExercises.observe(viewLifecycleOwner) { exercises ->
            val totalSets = exercises.sumOf { it.sets.size }
            
            if (exercises.size != lastExerciseCount || totalSets != lastTotalSets) {
                if (exercises.size > lastExerciseCount && exercises.isNotEmpty()) {
                    val newExercise = exercises.last()
                    expandedItems.add(newExercise.workoutExercise.id)
                }
                
                lastExerciseCount = exercises.size
                lastTotalSets = totalSets
                updateExerciseViews(exercises)
            }
            tvExerciseCount.text = exercises.size.toString()
            
            if (viewModel.isTraining.value == true) {
                layoutEmpty.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
                layoutExercises.visibility = if (exercises.isEmpty()) View.GONE else View.VISIBLE
            }
        }
        
        viewModel.totalSets.observe(viewLifecycleOwner) { sets ->
            if (tvTotalSets.text.toString() != sets.toString()) {
                tvTotalSets.animateNumberChange()
            }
            tvTotalSets.text = sets.toString()
        }
        
        viewModel.totalVolume.observe(viewLifecycleOwner) { volume ->
            if (tvTotalVolume.text.toString() != volume.toLong().toString()) {
                tvTotalVolume.animateNumberChange()
            }
            tvTotalVolume.text = volume.toLong().toString()
        }
    }
    
    private fun updateExerciseViews(exercises: List<WorkoutExerciseWithDetails>) {
        layoutExercises.removeAllViews()
        
        val displayItems = groupExercises(exercises)
        
        displayItems.forEach { item ->
            when (item) {
                is DisplayItem.SingleExercise -> {
                    val exerciseView = createExerciseView(item.exercise)
                    layoutExercises.addView(exerciseView)
                }
                is DisplayItem.Superset -> {
                    val supersetView = createSupersetView(item)
                    layoutExercises.addView(supersetView)
                }
            }
        }
    }

    
    private fun groupExercises(exercises: List<WorkoutExerciseWithDetails>): List<DisplayItem> {
        val result = mutableListOf<DisplayItem>()
        val grouped = mutableMapOf<Long, MutableList<WorkoutExerciseWithDetails>>()

        exercises.forEach { exercise ->
            val groupId = exercise.workoutExercise.supersetGroupId
            if (groupId != null) {
                grouped.getOrPut(groupId) { mutableListOf() }.add(exercise)
            }
        }

        val processed = mutableSetOf<Long>()
        exercises.forEach { exercise ->
            val groupId = exercise.workoutExercise.supersetGroupId
            if (groupId != null) {
                if (!processed.contains(groupId)) {
                    processed.add(groupId)
                    val group = grouped[groupId] ?: emptyList()
                    if (group.size > 1) {
                        result.add(DisplayItem.Superset(groupId, group))
                    } else {
                        result.add(DisplayItem.SingleExercise(group.first()))
                    }
                }
            } else {
                result.add(DisplayItem.SingleExercise(exercise))
            }
        }

        return result
    }
    
    private fun createExerciseView(item: WorkoutExerciseWithDetails): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_workout_exercise, layoutExercises, false)
        
        bindExerciseView(view, item)
        return view
    }
    
    private fun createSupersetView(superset: DisplayItem.Superset): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_superset_container, layoutExercises, false)
        
        val tvSupersetCount = view.findViewById<TextView>(R.id.tv_superset_count)
        val llExercises = view.findViewById<LinearLayout>(R.id.ll_exercises)
        
        tvSupersetCount.text = "${superset.exercises.size}个动作"
        llExercises.removeAllViews()
        
        superset.exercises.forEachIndexed { index, exercise ->
            val exerciseView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_superset_exercise, llExercises, false)
            
            val divider = exerciseView.findViewById<View>(R.id.divider)
            divider.visibility = if (index > 0) View.VISIBLE else View.GONE
            
            bindExerciseView(exerciseView, exercise)
            llExercises.addView(exerciseView)
        }
        
        return view
    }

    
    private fun bindExerciseView(view: View, item: WorkoutExerciseWithDetails) {
        val layoutHeader = view.findViewById<LinearLayout>(R.id.layout_header)
        val layoutContent = view.findViewById<LinearLayout>(R.id.layout_content)
        val tvExpandIcon = view.findViewById<TextView>(R.id.tv_expand_icon)
        val tvExerciseName = view.findViewById<TextView>(R.id.tv_exercise_name)
        val tvExerciseSummary = view.findViewById<TextView>(R.id.tv_exercise_summary)
        val layoutSets = view.findViewById<LinearLayout>(R.id.layout_sets)
        val btnAddSet = view.findViewById<MaterialButton>(R.id.btn_add_set)
        val btnDeleteExercise = view.findViewById<ImageButton>(R.id.btn_delete_exercise)
        
        val isExpanded = expandedItems.contains(item.workoutExercise.id)
        
        tvExerciseName.text = item.exercise.name
        val completedSets = item.sets.count { it.isCompleted }
        val totalSets = item.sets.size
        tvExerciseSummary.text = "${item.exercise.muscleGroup} · $completedSets/$totalSets 组"
        
        tvExpandIcon.text = if (isExpanded) "▼" else "▶"
        layoutContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        
        layoutHeader.setOnClickListener {
            if (expandedItems.contains(item.workoutExercise.id)) {
                expandedItems.remove(item.workoutExercise.id)
            } else {
                expandedItems.add(item.workoutExercise.id)
            }
            viewModel.workoutExercises.value?.let { updateExerciseViews(it) }
        }
        
        // 使用LinearLayout显示组数据
        layoutSets.removeAllViews()
        item.sets.forEach { set ->
            val setView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_workout_set, layoutSets, false)
            bindSetView(setView, set)
            layoutSets.addView(setView)
        }
        
        btnAddSet.setOnClickListener {
            // 先保存所有现有组的 UI 数据到数据库
            for (i in 0 until layoutSets.childCount) {
                val setView = layoutSets.getChildAt(i)
                val weightEdit = setView.findViewById<EditText>(R.id.et_weight)
                val repsEdit = setView.findViewById<EditText>(R.id.et_reps)
                val cbCompleted = setView.findViewById<android.widget.CheckBox>(R.id.cb_completed)
                
                val weight = weightEdit?.text?.toString()?.toDoubleOrNull() ?: 0.0
                val reps = repsEdit?.text?.toString()?.toIntOrNull() ?: 0
                val isCompleted = cbCompleted?.isChecked ?: false
                
                val originalSet = item.sets.getOrNull(i)
                if (originalSet != null && (weight != originalSet.weight || reps != originalSet.reps || isCompleted != originalSet.isCompleted)) {
                    viewModel.updateSet(originalSet.copy(weight = weight, reps = reps, isCompleted = isCompleted))
                }
            }
            
            // 从 LinearLayout 最后一个子 View 获取当前输入的重量和次数
            val lastWeight: Double
            val lastReps: Int
            
            if (layoutSets.childCount > 0) {
                val lastSetView = layoutSets.getChildAt(layoutSets.childCount - 1)
                val weightEdit = lastSetView.findViewById<EditText>(R.id.et_weight)
                val repsEdit = lastSetView.findViewById<EditText>(R.id.et_reps)
                lastWeight = weightEdit?.text?.toString()?.toDoubleOrNull() ?: 0.0
                lastReps = repsEdit?.text?.toString()?.toIntOrNull() ?: 0
            } else {
                lastWeight = 0.0
                lastReps = 0
            }
            
            viewModel.addSetToExercise(item.workoutExercise.id, lastWeight, lastReps)
        }
        
        btnDeleteExercise.setOnClickListener {
            showDeleteExerciseDialog(item)
        }
    }
    
    private fun bindSetView(view: View, set: WorkoutSet) {
        val tvSetNumber = view.findViewById<TextView>(R.id.tv_set_number)
        val etWeight = view.findViewById<EditText>(R.id.et_weight)
        val etReps = view.findViewById<EditText>(R.id.et_reps)
        val cbCompleted = view.findViewById<android.widget.CheckBox>(R.id.cb_completed)
        val btnWeightMinus = view.findViewById<TextView>(R.id.btn_weight_minus)
        val btnWeightPlus = view.findViewById<TextView>(R.id.btn_weight_plus)
        val btnRepsMinus = view.findViewById<TextView>(R.id.btn_reps_minus)
        val btnRepsPlus = view.findViewById<TextView>(R.id.btn_reps_plus)
        val btnDeleteSet = view.findViewById<TextView>(R.id.btn_delete_set)
        
        val weightStep = 2.5
        
        tvSetNumber.text = set.setNumber.toString()
        etWeight.setText(formatWeight(set.weight))
        etReps.setText(if (set.reps == 0) "" else set.reps.toString())
        cbCompleted.isChecked = set.isCompleted
        
        var currentSet = set
        
        cbCompleted.setOnClickListener {
            val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            val reps = etReps.text.toString().toIntOrNull() ?: 0
            currentSet = currentSet.copy(weight = weight, reps = reps, isCompleted = cbCompleted.isChecked)
            viewModel.updateSet(currentSet)
        }
        
        btnWeightMinus.setOnClickListener {
            val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            val newWeight = maxOf(0.0, currentWeight - weightStep)
            etWeight.setText(formatWeight(newWeight))
        }
        
        btnWeightPlus.setOnClickListener {
            val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            etWeight.setText(formatWeight(currentWeight + weightStep))
        }
        
        btnRepsMinus.setOnClickListener {
            val currentReps = etReps.text.toString().toIntOrNull() ?: 0
            val newReps = maxOf(0, currentReps - 1)
            etReps.setText(if (newReps == 0) "" else newReps.toString())
        }
        
        btnRepsPlus.setOnClickListener {
            val currentReps = etReps.text.toString().toIntOrNull() ?: 0
            etReps.setText((currentReps + 1).toString())
        }
        
        btnDeleteSet.setOnClickListener {
            showDeleteSetDialog(currentSet)
        }
        
        etWeight.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val reps = etReps.text.toString().toIntOrNull() ?: 0
                if (weight != currentSet.weight || reps != currentSet.reps) {
                    currentSet = currentSet.copy(weight = weight, reps = reps)
                    viewModel.updateSet(currentSet)
                }
            }
        }
        
        etReps.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val reps = etReps.text.toString().toIntOrNull() ?: 0
                if (weight != currentSet.weight || reps != currentSet.reps) {
                    currentSet = currentSet.copy(weight = weight, reps = reps)
                    viewModel.updateSet(currentSet)
                }
            }
        }
    }
    
    private fun formatWeight(weight: Double): String {
        return if (weight == 0.0) ""
        else if (weight == weight.toLong().toDouble()) weight.toLong().toString()
        else weight.toString()
    }

    
    private fun setupListeners() {
        btnStartEnd.setOnClickListener {
            if (viewModel.isTraining.value == true) {
                showEndTrainingDialog()
            } else {
                viewModel.startTraining()
            }
        }
        
        btnTemplate.setOnClickListener {
            showTemplateListDialog()
        }
        
        btnPause.setOnClickListener {
            if (viewModel.isPaused.value == true) {
                viewModel.resumeTraining()
            } else {
                viewModel.pauseTraining()
            }
        }
        
        btnResetTimer.setOnClickListener {
            showResetTimerDialog()
        }
        
        btnRestTimer.setOnClickListener {
            showRestTimerDialog()
        }
        
        btnNotes.setOnClickListener {
            showNotesDialog()
        }
        
        btnSaveTemplate.setOnClickListener {
            showSaveTemplateDialog()
        }
        
        fabAddExercise.setOnClickListener {
            showExerciseSelectDialog()
        }
    }
    
    private fun updatePauseButton(isPaused: Boolean) {
        if (isPaused) {
            btnPause.text = "开始"
            tvTimer.alpha = 0.5f
        } else {
            btnPause.text = "暂停"
            tvTimer.alpha = 1.0f
        }
    }
    
    private fun updateUIForTrainingState(isTraining: Boolean) {
        if (isTraining) {
            btnStartEnd.text = "结束"
            btnTemplate.visibility = View.GONE
            btnPause.visibility = View.VISIBLE
            btnResetTimer.visibility = View.VISIBLE
            layoutTrainingButtons.visibility = View.VISIBLE
            fabAddExercise.visibility = View.VISIBLE
            layoutEmpty.visibility = View.VISIBLE
            layoutExercises.visibility = View.GONE
            startTimer()
        } else {
            btnStartEnd.text = "开始训练"
            btnTemplate.visibility = View.VISIBLE
            btnPause.visibility = View.GONE
            btnResetTimer.visibility = View.GONE
            layoutTrainingButtons.visibility = View.GONE
            fabAddExercise.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
            layoutExercises.visibility = View.GONE
            stopTimer()
            tvTimer.text = "00:00:00"
            tvTimer.alpha = 1.0f
        }
    }

    
    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (viewModel.isPaused.value == true) {
                    timerHandler.postDelayed(this, 1000)
                    return
                }
                val elapsed = System.currentTimeMillis() - viewModel.getStartTime() - viewModel.getPausedTime()
                val hours = (elapsed / 3600000).toInt()
                val minutes = ((elapsed % 3600000) / 60000).toInt()
                val seconds = ((elapsed % 60000) / 1000).toInt()
                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable!!)
    }
    
    private fun stopTimer() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        timerRunnable = null
    }
    
    private fun showRestTimerDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_rest_timer, null)
        
        val tvRestTimer = dialogView.findViewById<TextView>(R.id.tv_rest_timer)
        val progressRest = dialogView.findViewById<CircularProgressIndicator>(R.id.progress_rest)
        val chip30s = dialogView.findViewById<Chip>(R.id.chip_30s)
        val chip60s = dialogView.findViewById<Chip>(R.id.chip_60s)
        val chip90s = dialogView.findViewById<Chip>(R.id.chip_90s)
        val chip120s = dialogView.findViewById<Chip>(R.id.chip_120s)
        val chip180s = dialogView.findViewById<Chip>(R.id.chip_180s)
        val chipCustom = dialogView.findViewById<Chip>(R.id.chip_custom)
        val layoutCustomTime = dialogView.findViewById<View>(R.id.layout_custom_time)
        val etCustomMinutes = dialogView.findViewById<EditText>(R.id.et_custom_minutes)
        val etCustomSeconds = dialogView.findViewById<EditText>(R.id.et_custom_seconds)
        val btnApplyCustom = dialogView.findViewById<MaterialButton>(R.id.btn_apply_custom)
        val btnDone = dialogView.findViewById<MaterialButton>(R.id.btn_done)
        val btnStart = dialogView.findViewById<MaterialButton>(R.id.btn_start)
        val btnMinusTime = dialogView.findViewById<MaterialButton>(R.id.btn_minus_time)
        val btnAddTime = dialogView.findViewById<MaterialButton>(R.id.btn_add_time)
        
        var isTimerRunning = false
        
        restDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        restDialog?.setOnCancelListener {
            cancelRestTimer()
        }
        
        updateRestTimerDisplay(tvRestTimer, currentRestDuration)
        progressRest.max = currentRestDuration
        progressRest.progress = currentRestDuration
        
        when (currentRestDuration) {
            30 -> chip30s.isChecked = true
            60 -> chip60s.isChecked = true
            90 -> chip90s.isChecked = true
            120 -> chip120s.isChecked = true
            180 -> chip180s.isChecked = true
            else -> {
                chipCustom.isChecked = true
                layoutCustomTime.visibility = View.VISIBLE
                etCustomMinutes.setText((currentRestDuration / 60).toString())
                etCustomSeconds.setText((currentRestDuration % 60).toString())
            }
        }

        
        fun clearChipSelection() {
            chip30s.isChecked = false
            chip60s.isChecked = false
            chip90s.isChecked = false
            chip120s.isChecked = false
            chip180s.isChecked = false
            chipCustom.isChecked = false
        }
        
        val chipClickListener = View.OnClickListener { v ->
            clearChipSelection()
            (v as Chip).isChecked = true
            
            if (v == chipCustom) {
                layoutCustomTime.visibility = View.VISIBLE
            } else {
                layoutCustomTime.visibility = View.GONE
                currentRestDuration = when (v) {
                    chip30s -> 30
                    chip60s -> 60
                    chip90s -> 90
                    chip120s -> 120
                    chip180s -> 180
                    else -> 90
                }
                if (isTimerRunning) {
                    restartRestTimer(tvRestTimer, progressRest)
                } else {
                    updateRestTimerDisplay(tvRestTimer, currentRestDuration)
                    progressRest.max = currentRestDuration
                    progressRest.progress = currentRestDuration
                }
            }
        }
        
        chip30s.setOnClickListener(chipClickListener)
        chip60s.setOnClickListener(chipClickListener)
        chip90s.setOnClickListener(chipClickListener)
        chip120s.setOnClickListener(chipClickListener)
        chip180s.setOnClickListener(chipClickListener)
        chipCustom.setOnClickListener(chipClickListener)
        
        btnApplyCustom.setOnClickListener {
            val minutes = etCustomMinutes.text.toString().toIntOrNull() ?: 0
            val seconds = etCustomSeconds.text.toString().toIntOrNull() ?: 0
            val totalSeconds = minutes * 60 + seconds
            if (totalSeconds > 0) {
                currentRestDuration = totalSeconds
                layoutCustomTime.visibility = View.GONE
                if (isTimerRunning) {
                    restartRestTimer(tvRestTimer, progressRest)
                } else {
                    updateRestTimerDisplay(tvRestTimer, currentRestDuration)
                    progressRest.max = currentRestDuration
                    progressRest.progress = currentRestDuration
                }
            }
        }
        
        btnDone.setOnClickListener {
            cancelRestTimer()
            restDialog?.dismiss()
        }
        
        btnStart.setOnClickListener {
            isTimerRunning = true
            btnStart.visibility = View.GONE
            btnMinusTime.visibility = View.VISIBLE
            btnAddTime.visibility = View.VISIBLE
            startRestTimer(tvRestTimer, progressRest)
        }
        
        btnMinusTime.setOnClickListener {
            addRestTime(-30, tvRestTimer, progressRest)
        }
        
        btnAddTime.setOnClickListener {
            addRestTime(30, tvRestTimer, progressRest)
        }
        
        restDialog?.show()
    }

    
    private fun updateRestTimerDisplay(tvRestTimer: TextView, seconds: Int) {
        val minutes = seconds / 60
        val secs = seconds % 60
        tvRestTimer.text = String.format("%02d:%02d", minutes, secs)
    }
    
    private var remainingRestTime = 0L
    
    private fun startRestTimer(tvRestTimer: TextView, progressRest: CircularProgressIndicator) {
        remainingRestTime = currentRestDuration * 1000L
        progressRest.max = currentRestDuration
        progressRest.progress = currentRestDuration
        
        restCountDownTimer = object : CountDownTimer(remainingRestTime, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingRestTime = millisUntilFinished
                val seconds = (millisUntilFinished / 1000).toInt()
                val minutes = seconds / 60
                val secs = seconds % 60
                tvRestTimer.text = String.format("%02d:%02d", minutes, secs)
                progressRest.progress = seconds
            }
            
            override fun onFinish() {
                tvRestTimer.text = "00:00"
                progressRest.progress = 0
                playAlarmSound()
                Handler(Looper.getMainLooper()).postDelayed({
                    restDialog?.dismiss()
                }, 1500)
            }
        }.start()
    }
    
    private fun restartRestTimer(tvRestTimer: TextView, progressRest: CircularProgressIndicator) {
        restCountDownTimer?.cancel()
        startRestTimer(tvRestTimer, progressRest)
    }
    
    private fun addRestTime(seconds: Int, tvRestTimer: TextView, progressRest: CircularProgressIndicator) {
        restCountDownTimer?.cancel()
        remainingRestTime += seconds * 1000L
        if (remainingRestTime < 1000) {
            remainingRestTime = 1000
        }
        val totalSeconds = (remainingRestTime / 1000).toInt()
        progressRest.max = totalSeconds
        
        restCountDownTimer = object : CountDownTimer(remainingRestTime, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingRestTime = millisUntilFinished
                val secs = (millisUntilFinished / 1000).toInt()
                val minutes = secs / 60
                val s = secs % 60
                tvRestTimer.text = String.format("%02d:%02d", minutes, s)
                progressRest.progress = secs
            }
            
            override fun onFinish() {
                tvRestTimer.text = "00:00"
                progressRest.progress = 0
                playAlarmSound()
                Handler(Looper.getMainLooper()).postDelayed({
                    restDialog?.dismiss()
                }, 1500)
            }
        }.start()
    }
    
    private fun cancelRestTimer() {
        restCountDownTimer?.cancel()
        restCountDownTimer = null
    }
    
    private fun playAlarmSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(requireContext(), notification)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    
    private fun showEndTrainingDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("结束训练")
            .setMessage("确定要结束本次训练吗？")
            .setPositiveButton("结束") { _, _ ->
                viewModel.endTraining()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showResetTimerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("重置计时")
            .setMessage("确定要重置计时器吗？训练数据不会丢失。")
            .setPositiveButton("确定") { _, _ ->
                viewModel.resetTimer()
                viewModel.pauseTraining()
                Toast.makeText(requireContext(), "计时已重置，点击继续开始", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showNotesDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_training_notes, null)
        
        val etNotes = dialogView.findViewById<EditText>(R.id.et_notes)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        
        val currentNotes = viewModel.getNotes()
        etNotes.setText(currentNotes)
        
        btnDelete.visibility = if (currentNotes.isEmpty()) View.GONE else View.VISIBLE
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnDelete.setOnClickListener {
            viewModel.deleteNotes()
            Toast.makeText(requireContext(), "备注已删除", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val notes = etNotes.text.toString().trim()
            viewModel.updateNotes(notes)
            Toast.makeText(requireContext(), "备注已保存", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showDeleteSetDialog(set: WorkoutSet) {
        val exercises = viewModel.workoutExercises.value ?: return
        val exercise = exercises.find { it.sets.any { s -> s.id == set.id } }
        val isLastSet = exercise?.sets?.size == 1
        
        val message = if (isLastSet) {
            "这是最后一组，删除后将移除整个动作「${exercise?.exercise?.name}」，确定删除吗？"
        } else {
            "确定要删除第 ${set.setNumber} 组吗？"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(if (isLastSet) "删除动作" else "删除组")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteSet(set)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showDeleteExerciseDialog(exerciseWithDetails: WorkoutExerciseWithDetails) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除动作")
            .setMessage("确定要删除 ${exerciseWithDetails.exercise.name} 及其所有组数据吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteExercise(exerciseWithDetails)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    
    private fun showExerciseSelectDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_select_exercise, null)
        
        val etSearch = dialogView.findViewById<EditText>(R.id.et_search)
        val chipGroupEquipment = dialogView.findViewById<ChipGroup>(R.id.chip_group_equipment)
        val rvMuscleGroups = dialogView.findViewById<RecyclerView>(R.id.rv_muscle_groups)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_exercises)
        val tvSelectedCount = dialogView.findViewById<TextView>(R.id.tv_selected_count)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("选择动作")
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .create()
        
        var currentMuscleGroup: String? = null
        var currentSubGroup: String? = null
        var currentEquipment: String? = null
        var searchQuery = ""
        
        val adapter = ExerciseSelectAdapter { selectedList ->
            val count = selectedList.size
            tvSelectedCount.text = "已选 $count 个"
            btnConfirm.isEnabled = count > 0
            btnConfirm.text = if (count > 1) "添加超级组" else "确认添加"
        }
        
        btnConfirm.isEnabled = false
        
        fun filterExercises() {
            var exercises = viewModel.exercises.value ?: emptyList()
            
            // 搜索过滤
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
                // 子分类过滤
                if (currentSubGroup != null) {
                    exercises = exercises.filter { it.subMuscleGroup == currentSubGroup }
                }
            }
            
            // 器械过滤
            if (currentEquipment != null) {
                exercises = exercises.filter { it.equipment == currentEquipment }
            }
            
            adapter.submitList(exercises)
        }
        
        // 设置肌群列表
        val muscleGroupAdapter = SelectMuscleGroupAdapter { group, subGroup ->
            currentMuscleGroup = group
            currentSubGroup = subGroup
            filterExercises()
        }
        rvMuscleGroups.layoutManager = LinearLayoutManager(requireContext())
        rvMuscleGroups.adapter = muscleGroupAdapter
        
        // 设置动作列表
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        filterExercises()
        
        // 搜索
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                filterExercises()
            }
        })
        
        // 器械筛选
        chipGroupEquipment.setOnCheckedStateChangeListener { _, checkedIds ->
            currentEquipment = if (checkedIds.isEmpty() || checkedIds.first() == R.id.chip_all_equipment) {
                null
            } else {
                dialogView.findViewById<Chip>(checkedIds.first())?.text?.toString()
            }
            filterExercises()
        }
        
        btnConfirm.setOnClickListener {
            val selectedExercises = adapter.getSelectedExercises()
            if (selectedExercises.isNotEmpty()) {
                if (selectedExercises.size > 1) {
                    viewModel.addSupersetToWorkout(selectedExercises)
                    Toast.makeText(requireContext(), "已添加超级组（${selectedExercises.size}个动作）", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addExerciseToWorkout(selectedExercises.first())
                    Toast.makeText(requireContext(), "已添加动作", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        cancelRestTimer()
    }

    
    private fun showTemplateListDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_template_list, null)
        
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tv_empty)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_templates)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .create()
        
        val adapter = TemplateAdapter(
            onItemClick = { template ->
                dialog.dismiss()
                viewModel.startFromTemplate(template.id)
            },
            onDeleteClick = { template ->
                showDeleteTemplateDialog(template)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        viewModel.templates.observe(viewLifecycleOwner) { templates ->
            if (templates.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                
                lifecycleScope.launch {
                    val items = templates.map { template ->
                        val details = viewModel.getTemplateWithExercises(template.id)
                        TemplateItem(template, details?.exercises?.size ?: 0)
                    }
                    adapter.submitList(items)
                }
            }
        }
        
        dialog.show()
    }
    
    private fun showSaveTemplateDialog() {
        val exercises = viewModel.workoutExercises.value
        if (exercises.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "请先添加动作", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_save_template, null)
        
        val etName = dialogView.findViewById<EditText>(R.id.et_template_name)
        val tvPreview = dialogView.findViewById<TextView>(R.id.tv_exercises_preview)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        
        val exerciseNames = exercises.map { it.exercise.name }
        tvPreview.text = "包含动作：${exerciseNames.joinToString("、")}"
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入模板名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.saveAsTemplate(name) {
                Toast.makeText(requireContext(), "模板已保存", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showDeleteTemplateDialog(template: WorkoutTemplate) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除模板")
            .setMessage("确定要删除「${template.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTemplate(template)
                Toast.makeText(requireContext(), "模板已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}


