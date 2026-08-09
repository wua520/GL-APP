package com.fitness.training.ui.exercises

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitness.training.R
import com.fitness.training.data.entity.Exercise
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.File
import java.io.FileOutputStream

class ExercisesFragment : Fragment() {

    private lateinit var viewModel: ExercisesViewModel
    private lateinit var rvMuscleGroups: RecyclerView
    private lateinit var rvExercises: RecyclerView
    private lateinit var muscleGroupAdapter: MuscleGroupAdapter
    private lateinit var exerciseGridAdapter: ExerciseGridAdapter
    private lateinit var etSearch: EditText
    private lateinit var chipGroupEquipment: ChipGroup
    private lateinit var tvCategoryTitle: TextView
    private lateinit var emptyState: View
    private lateinit var tvEmptyMessage: TextView

    private var currentMuscleGroup: String = "胸"
    private var currentSubGroup: String? = null
    private var currentEquipment: String? = null
    
    // 图片选择相关
    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null
    private var dialogPlaceholder: LinearLayout? = null
    private var dialogRemoveBtn: ImageButton? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                updateDialogImagePreview()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_exercises, container, false)
        
        viewModel = ViewModelProvider(this)[ExercisesViewModel::class.java]
        
        initViews(view)
        setupMuscleGroupList()
        setupExerciseGrid()
        setupSearch()
        setupEquipmentFilter()
        setupObservers()
        
        return view
    }
    
    private fun initViews(view: View) {
        rvMuscleGroups = view.findViewById(R.id.rv_muscle_groups)
        rvExercises = view.findViewById(R.id.rv_exercises)
        etSearch = view.findViewById(R.id.et_search)
        chipGroupEquipment = view.findViewById(R.id.chip_group_equipment)
        tvCategoryTitle = view.findViewById(R.id.tv_category_title)
        emptyState = view.findViewById(R.id.empty_state)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)
        
        view.findViewById<ImageButton>(R.id.btn_add_exercise).setOnClickListener {
            showAddExerciseDialog()
        }
    }
    
    private fun setupMuscleGroupList() {
        muscleGroupAdapter = MuscleGroupAdapter { group, subGroup ->
            currentMuscleGroup = group
            currentSubGroup = subGroup
            updateCategoryTitle()
            filterExercises()
        }
        rvMuscleGroups.layoutManager = LinearLayoutManager(requireContext())
        rvMuscleGroups.adapter = muscleGroupAdapter
    }
    
    private fun setupExerciseGrid() {
        exerciseGridAdapter = ExerciseGridAdapter(
            onItemClick = { exercise ->
                showExerciseDetailDialog(exercise)
            },
            onItemLongClick = { exercise ->
                showExerciseOptionsDialog(exercise)
            }
        )
        rvExercises.layoutManager = GridLayoutManager(requireContext(), 2)
        rvExercises.adapter = exerciseGridAdapter
    }
    
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                viewModel.setSearch(query)
                // 搜索时更新标题
                if (query.isNotEmpty()) {
                    tvCategoryTitle.text = "搜索结果"
                } else {
                    updateCategoryTitle()
                }
            }
        })
    }
    
    private fun setupEquipmentFilter() {
        chipGroupEquipment.setOnCheckedStateChangeListener { _, checkedIds ->
            currentEquipment = if (checkedIds.isEmpty() || checkedIds.first() == R.id.chip_all_equipment) {
                null
            } else {
                view?.findViewById<Chip>(checkedIds.first())?.text?.toString()
            }
            filterExercises()
        }
    }
    
    private fun setupObservers() {
        viewModel.filteredExercises.observe(viewLifecycleOwner) { exercises ->
            val filtered = filterByCurrentSelection(exercises)
            exerciseGridAdapter.submitList(filtered)
            updateEmptyState(filtered.isEmpty())
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        val searchQuery = etSearch.text?.toString() ?: ""
        if (isEmpty) {
            emptyState.visibility = View.VISIBLE
            rvExercises.visibility = View.GONE
            tvEmptyMessage.text = if (searchQuery.isNotEmpty()) {
                "没有找到「$searchQuery」相关动作"
            } else if (currentMuscleGroup == "★ 收藏") {
                "暂无收藏的动作"
            } else if (currentMuscleGroup == "★ 自定义") {
                "暂无自定义动作"
            } else {
                "该分类暂无动作"
            }
        } else {
            emptyState.visibility = View.GONE
            rvExercises.visibility = View.VISIBLE
        }
    }
    
    private fun filterByCurrentSelection(exercises: List<Exercise>): List<Exercise> {
        // 如果有搜索词，直接返回ViewModel筛选后的结果（跨所有分类搜索）
        val searchQuery = etSearch.text?.toString() ?: ""
        if (searchQuery.isNotEmpty()) {
            return exercises.filter { exercise ->
                currentEquipment == null || exercise.equipment == currentEquipment
            }
        }
        
        return exercises.filter { exercise ->
            // 收藏筛选
            if (currentMuscleGroup == "★ 收藏") {
                return@filter exercise.isFavorite && 
                    (currentEquipment == null || exercise.equipment == currentEquipment)
            }
            
            // 自定义动作筛选
            if (currentMuscleGroup == "★ 自定义") {
                return@filter exercise.isCustom && 
                    (currentEquipment == null || exercise.equipment == currentEquipment)
            }
            
            val muscleMatch = matchMuscleGroup(exercise)
            val equipmentMatch = currentEquipment == null || exercise.equipment == currentEquipment
            muscleMatch && equipmentMatch
        }
    }
    
    private fun matchMuscleGroup(exercise: Exercise): Boolean {
        // 映射肌群名称
        val groupMap = mapOf(
            "胸" to "胸部",
            "背" to "背部",
            "腿" to "腿部",
            "肩" to "肩部"
        )
        
        val targetGroup = groupMap[currentMuscleGroup] ?: currentMuscleGroup
        
        // 先匹配主分类
        val mainMatch = exercise.muscleGroup.contains(targetGroup) || targetGroup.contains(exercise.muscleGroup)
        
        if (!mainMatch) return false
        
        // 如果选择了子分类，还要匹配子分类
        if (currentSubGroup != null) {
            return exercise.subMuscleGroup == currentSubGroup
        }
        
        return true
    }
    
    private fun filterExercises() {
        viewModel.filteredExercises.value?.let { exercises ->
            val filtered = filterByCurrentSelection(exercises)
            exerciseGridAdapter.submitList(filtered)
        }
    }
    
    private fun updateCategoryTitle() {
        tvCategoryTitle.text = if (currentSubGroup != null) {
            currentSubGroup
        } else {
            when (currentMuscleGroup) {
                "★ 收藏" -> "收藏"
                "★ 自定义" -> "自定义动作"
                "胸" -> "胸部"
                "背" -> "背部"
                "腿" -> "腿部"
                "肩" -> "肩部"
                else -> currentMuscleGroup
            }
        }
    }
    
    private fun showAddExerciseDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_exercise, null)
        
        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val chipGroupMuscle = dialogView.findViewById<ChipGroup>(R.id.chip_group_muscle)
        val chipGroupEquipment = dialogView.findViewById<ChipGroup>(R.id.chip_group_equipment)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_description)
        
        // 图片选择相关
        val imageContainer = dialogView.findViewById<FrameLayout>(R.id.image_container)
        dialogImageView = dialogView.findViewById(R.id.iv_exercise_image)
        dialogPlaceholder = dialogView.findViewById(R.id.placeholder_content)
        dialogRemoveBtn = dialogView.findViewById(R.id.btn_remove_image)
        
        selectedImageUri = null
        updateDialogImagePreview()
        
        imageContainer.setOnClickListener {
            openImagePicker()
        }
        
        dialogRemoveBtn?.setOnClickListener {
            selectedImageUri = null
            updateDialogImagePreview()
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("添加自定义动作")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入动作名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val muscleGroup = when (chipGroupMuscle.checkedChipId) {
                    R.id.chip_chest -> "胸部"
                    R.id.chip_back -> "背部"
                    R.id.chip_legs -> "腿部"
                    R.id.chip_shoulders -> "肩部"
                    R.id.chip_arms -> "手臂"
                    R.id.chip_core -> "核心"
                    else -> "胸部"
                }
                
                val equipment = when (chipGroupEquipment.checkedChipId) {
                    R.id.chip_barbell -> "杠铃"
                    R.id.chip_dumbbell -> "哑铃"
                    R.id.chip_machine -> "器械"
                    R.id.chip_bodyweight -> "自重"
                    R.id.chip_cable -> "绳索"
                    else -> "杠铃"
                }
                
                val description = etDescription.text.toString().trim()
                
                // 保存图片并获取本地路径
                val imageUrl = selectedImageUri?.let { uri ->
                    saveImageToLocal(uri, name)
                } ?: ""
                
                viewModel.addExercise(name, muscleGroup, equipment, description, imageUrl)
                Toast.makeText(requireContext(), "动作已添加", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }
    
    private fun updateDialogImagePreview() {
        if (selectedImageUri != null) {
            dialogImageView?.visibility = View.VISIBLE
            dialogPlaceholder?.visibility = View.GONE
            dialogRemoveBtn?.visibility = View.VISIBLE
            dialogImageView?.let { imageView ->
                Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(imageView)
            }
        } else {
            dialogImageView?.visibility = View.GONE
            dialogPlaceholder?.visibility = View.VISIBLE
            dialogRemoveBtn?.visibility = View.GONE
        }
    }
    
    private fun saveImageToLocal(uri: Uri, exerciseName: String): String {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = "exercise_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            inputStream?.close()
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    private fun showExerciseDetailDialog(exercise: Exercise) {
        val message = buildString {
            append("肌肉群：${exercise.muscleGroup}\n")
            append("器械：${exercise.equipment}\n")
            if (exercise.description.isNotEmpty()) {
                append("\n说明：${exercise.description}")
            }
            if (exercise.isCustom) {
                append("\n\n（自定义动作）")
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(exercise.name)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNeutralButton(if (exercise.isFavorite) "取消收藏" else "收藏") { _, _ ->
                viewModel.toggleFavorite(exercise)
            }
            .show()
    }
    
    private fun showExerciseOptionsDialog(exercise: Exercise) {
        val options = mutableListOf(
            if (exercise.isFavorite) "取消收藏" else "收藏"
        )
        if (exercise.isCustom) {
            options.add("删除")
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(exercise.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "收藏", "取消收藏" -> viewModel.toggleFavorite(exercise)
                    "删除" -> showDeleteConfirmDialog(exercise)
                }
            }
            .show()
    }
    
    private fun showDeleteConfirmDialog(exercise: Exercise) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除动作")
            .setMessage("确定要删除「${exercise.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteExercise(exercise)
                Toast.makeText(requireContext(), "动作已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
