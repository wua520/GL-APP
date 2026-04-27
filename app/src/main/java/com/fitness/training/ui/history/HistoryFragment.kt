package com.fitness.training.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.Workout
import com.fitness.training.data.repository.WorkoutRepository
import com.fitness.training.data.repository.WorkoutWithDetails
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var viewModel: HistoryViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotalWorkouts: TextView
    private lateinit var tvThisMonth: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: HistoryAdapter
    
    private lateinit var repository: WorkoutRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        
        val db = FitnessDatabase.getDatabase(requireContext())
        repository = WorkoutRepository(
            db.workoutDao(),
            db.workoutExerciseDao(),
            db.workoutSetDao(),
            db.exerciseDao()
        )
        
        initViews(view)
        setupRecyclerView()
        observeData()
        
        return view
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_history)
        tvTotalWorkouts = view.findViewById(R.id.tv_total_workouts)
        tvThisMonth = view.findViewById(R.id.tv_this_month)
        tvEmpty = view.findViewById(R.id.tv_empty)
    }
    
    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { workout -> showWorkoutDetail(workout) },
            onDeleteClick = { workout -> confirmDelete(workout) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun observeData() {
        viewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            loadHistoryItems(workouts)
        }
        
        viewModel.totalWorkouts.observe(viewLifecycleOwner) { count ->
            tvTotalWorkouts.text = count.toString()
        }
        
        viewModel.thisMonthWorkouts.observe(viewLifecycleOwner) { count ->
            tvThisMonth.text = count.toString()
        }
        
        viewModel.selectedWorkoutDetails.observe(viewLifecycleOwner) { details ->
            details?.let { showWorkoutDetailDialog(it) }
        }
    }
    
    private fun loadHistoryItems(workouts: List<Workout>) {
        if (workouts.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            adapter.submitList(emptyList())
            return
        }
        
        tvEmpty.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val items = workouts.map { workout ->
                val details = repository.getWorkoutWithDetails(workout.id)
                val exerciseCount = details?.exercises?.size ?: 0
                val exerciseNames = details?.exercises?.mapNotNull { it.exercise?.name } ?: emptyList()
                var totalVolume = 0.0
                details?.exercises?.forEach { ex ->
                    ex.sets.forEach { set ->
                        totalVolume += set.weight * set.reps
                    }
                }
                HistoryItem(workout, exerciseCount, totalVolume, exerciseNames)
            }
            adapter.submitList(items)
        }
    }
    
    private fun showWorkoutDetail(workout: Workout) {
        viewModel.loadWorkoutDetails(workout.id)
    }
    
    private fun showWorkoutDetailDialog(details: WorkoutWithDetails) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_workout_detail, null)
        
        val workout = details.workout
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
        
        // 显示备注
        val layoutNotes = dialogView.findViewById<LinearLayout>(R.id.layout_notes)
        val tvNotes = dialogView.findViewById<TextView>(R.id.tv_notes)
        if (workout.notes.isNotEmpty()) {
            layoutNotes.visibility = View.VISIBLE
            tvNotes.text = workout.notes
        } else {
            layoutNotes.visibility = View.GONE
        }
        
        // 填充动作列表
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
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
            viewModel.clearSelectedWorkout()
        }
        
        dialog.setOnDismissListener {
            viewModel.clearSelectedWorkout()
        }
        
        dialog.show()
    }
    
    private fun confirmDelete(workout: Workout) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除训练记录")
            .setMessage("确定要删除这条训练记录吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteWorkout(workout)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}


