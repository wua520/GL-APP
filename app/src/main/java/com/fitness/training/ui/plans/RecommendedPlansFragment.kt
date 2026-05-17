package com.fitness.training.ui.plans

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fitness.training.R
import com.fitness.training.data.entity.TrainingPlan
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup

class RecommendedPlansFragment : Fragment() {
    
    private lateinit var chipGroupGoal: ChipGroup
    private lateinit var chipGroupExperience: ChipGroup
    private lateinit var chipGroupTargetMuscles: ChipGroup
    private lateinit var chipGroupTrainingDays: ChipGroup
    private lateinit var chipGroupTrainingDuration: ChipGroup
    private lateinit var chipGroupEquipment: ChipGroup
    private lateinit var btnGeneratePlan: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var cardRecommendedPlan: CardView
    private lateinit var tvPlanTitle: TextView
    private lateinit var tvPlanDescription: TextView
    private lateinit var tvPlanDetails: TextView
    private lateinit var btnSavePlan: MaterialButton
    private lateinit var btnRegenerate: MaterialButton
    
    private lateinit var viewModel: TrainingPlanViewModel
    private var currentPlan: PlanData? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_recommended_plans, container, false)
        
        viewModel = ViewModelProvider(this)[TrainingPlanViewModel::class.java]
        
        chipGroupGoal = view.findViewById(R.id.chip_group_goal)
        chipGroupExperience = view.findViewById(R.id.chip_group_experience)
        chipGroupTargetMuscles = view.findViewById(R.id.chip_group_target_muscles)
        chipGroupTrainingDays = view.findViewById(R.id.chip_group_training_days)
        chipGroupTrainingDuration = view.findViewById(R.id.chip_group_training_duration)
        chipGroupEquipment = view.findViewById(R.id.chip_group_equipment)
        btnGeneratePlan = view.findViewById(R.id.btn_generate_plan)
        progressBar = view.findViewById(R.id.progress_bar)
        cardRecommendedPlan = view.findViewById(R.id.card_recommended_plan)
        tvPlanTitle = view.findViewById(R.id.tv_plan_title)
        tvPlanDescription = view.findViewById(R.id.tv_plan_description)
        tvPlanDetails = view.findViewById(R.id.tv_plan_details)
        btnSavePlan = view.findViewById(R.id.btn_save_plan)
        btnRegenerate = view.findViewById(R.id.btn_regenerate)
        
        btnGeneratePlan.setOnClickListener { generatePlan() }
        btnSavePlan.setOnClickListener { savePlanToPersonal() }
        btnRegenerate.setOnClickListener { generatePlan() }
        
        return view
    }

    
    private fun generatePlan() {
        progressBar.visibility = View.VISIBLE
        btnGeneratePlan.isEnabled = false
        cardRecommendedPlan.visibility = View.GONE
        
        Handler(Looper.getMainLooper()).postDelayed({
            val goal = when (chipGroupGoal.checkedChipId) {
                R.id.chip_muscle_gain -> "增肌"
                R.id.chip_fat_loss -> "减脂"
                R.id.chip_maintain -> "维持"
                else -> "增肌"
            }
            
            val experience = when (chipGroupExperience.checkedChipId) {
                R.id.chip_beginner -> "新手"
                R.id.chip_intermediate -> "中级"
                R.id.chip_advanced -> "高级"
                else -> "新手"
            }
            
            val trainingDays = when (chipGroupTrainingDays.checkedChipId) {
                R.id.chip_3_days -> 3
                R.id.chip_4_days -> 4
                R.id.chip_5_days -> 5
                R.id.chip_6_days -> 6
                else -> 3
            }
            
            val trainingDuration = when (chipGroupTrainingDuration.checkedChipId) {
                R.id.chip_short -> "短时"
                R.id.chip_standard -> "标准"
                R.id.chip_long -> "超长"
                else -> "标准"
            }
            
            val equipment = when (chipGroupEquipment.checkedChipId) {
                R.id.chip_home -> "家庭"
                R.id.chip_gym -> "健身房"
                else -> "健身房"
            }
            
            val targetMuscles = mutableListOf<String>()
            for (i in 0 until chipGroupTargetMuscles.childCount) {
                val chip = chipGroupTargetMuscles.getChildAt(i) as? com.google.android.material.chip.Chip
                if (chip?.isChecked == true) {
                    when (chip.id) {
                        R.id.chip_chest -> targetMuscles.add("胸部")
                        R.id.chip_back -> targetMuscles.add("背部")
                        R.id.chip_legs -> targetMuscles.add("腿部")
                        R.id.chip_shoulders -> targetMuscles.add("肩部")
                        R.id.chip_arms -> targetMuscles.add("手臂")
                        R.id.chip_core -> targetMuscles.add("核心")
                    }
                }
            }
            
            val plan = getPlanRecommendation(goal, experience, targetMuscles, trainingDays, trainingDuration, equipment)
            currentPlan = PlanData(
                title = plan.title,
                description = plan.description,
                details = plan.details,
                goal = goal,
                experience = experience,
                targetMuscles = targetMuscles,
                trainingDays = trainingDays,
                trainingDuration = trainingDuration,
                equipment = equipment
            )
            
            tvPlanTitle.text = plan.title
            tvPlanDescription.text = plan.description
            tvPlanDetails.text = plan.details
            
            progressBar.visibility = View.GONE
            btnGeneratePlan.isEnabled = true
            cardRecommendedPlan.visibility = View.VISIBLE
            
            cardRecommendedPlan.post {
                val scrollView = view?.parent as? android.widget.ScrollView
                scrollView?.smoothScrollTo(0, cardRecommendedPlan.top)
            }
        }, 800)
    }
    
    private fun savePlanToPersonal() {
        currentPlan?.let { planData ->
            val trainingPlan = TrainingPlan(
                title = planData.title,
                description = planData.description,
                details = planData.details,
                goal = planData.goal,
                experience = planData.experience,
                targetMuscles = planData.targetMuscles.joinToString(","),
                trainingDays = planData.trainingDays,
                trainingDuration = planData.trainingDuration,
                equipment = planData.equipment,
                isFromRecommendation = true
            )
            viewModel.insertPlan(trainingPlan)
            Toast.makeText(requireContext(), "计划已保存到个人计划", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(requireContext(), "请先生成计划", Toast.LENGTH_SHORT).show()
        }
    }

    
    data class Exercise(
        val name: String,
        val targetMuscle: String,
        val equipment: String,
        val difficulty: String,
        val type: String
    )
    
    private fun getExerciseDatabase(): List<Exercise> {
        return listOf(
            Exercise("杠铃卧推", "胸部", "gym", "beginner", "compound"),
            Exercise("哑铃卧推", "胸部", "home", "beginner", "compound"),
            Exercise("上斜杠铃卧推", "胸部", "gym", "intermediate", "compound"),
            Exercise("上斜哑铃卧推", "胸部", "home", "intermediate", "compound"),
            Exercise("哑铃飞鸟", "胸部", "home", "intermediate", "isolation"),
            Exercise("龙门架夹胸", "胸部", "gym", "intermediate", "isolation"),
            Exercise("俯卧撑", "胸部", "home", "beginner", "compound"),
            Exercise("杠铃硬拉", "背部", "gym", "intermediate", "compound"),
            Exercise("哑铃硬拉", "背部", "home", "intermediate", "compound"),
            Exercise("杠铃划船", "背部", "gym", "intermediate", "compound"),
            Exercise("哑铃单臂划船", "背部", "home", "beginner", "compound"),
            Exercise("引体向上", "背部", "home", "intermediate", "compound"),
            Exercise("高位下拉", "背部", "gym", "beginner", "compound"),
            Exercise("坐姿划船", "背部", "gym", "beginner", "compound"),
            Exercise("直臂下压", "背部", "gym", "intermediate", "isolation"),
            Exercise("杠铃深蹲", "腿部", "gym", "beginner", "compound"),
            Exercise("哑铃深蹲", "腿部", "home", "beginner", "compound"),
            Exercise("腿举", "腿部", "gym", "beginner", "compound"),
            Exercise("保加利亚分腿蹲", "腿部", "home", "intermediate", "compound"),
            Exercise("腿屈伸", "腿部", "gym", "beginner", "isolation"),
            Exercise("腿弯举", "腿部", "gym", "beginner", "isolation"),
            Exercise("箭步蹲", "腿部", "home", "beginner", "compound"),
            Exercise("提踵", "腿部", "home", "beginner", "isolation"),
            Exercise("杠铃推举", "肩部", "gym", "intermediate", "compound"),
            Exercise("哑铃推举", "肩部", "home", "beginner", "compound"),
            Exercise("侧平举", "肩部", "home", "beginner", "isolation"),
            Exercise("前平举", "肩部", "home", "beginner", "isolation"),
            Exercise("俯身飞鸟", "肩部", "home", "intermediate", "isolation"),
            Exercise("面拉", "肩部", "gym", "intermediate", "isolation"),
            Exercise("杠铃弯举", "手臂", "gym", "beginner", "isolation"),
            Exercise("哑铃弯举", "手臂", "home", "beginner", "isolation"),
            Exercise("锤式弯举", "手臂", "home", "beginner", "isolation"),
            Exercise("三头下压", "手臂", "gym", "beginner", "isolation"),
            Exercise("哑铃臂屈伸", "手臂", "home", "beginner", "isolation"),
            Exercise("窄距卧推", "手臂", "gym", "intermediate", "compound"),
            Exercise("平板支撑", "核心", "home", "beginner", "isolation"),
            Exercise("卷腹", "核心", "home", "beginner", "isolation"),
            Exercise("悬垂举腿", "核心", "gym", "intermediate", "isolation"),
            Exercise("俄罗斯转体", "核心", "home", "beginner", "isolation")
        )
    }

    
    private fun getPlanRecommendation(
        goal: String, 
        experience: String, 
        targetMuscles: List<String>,
        trainingDays: Int,
        trainingDuration: String,
        equipment: String
    ): GeneratedPlan {
        val exerciseDb = getExerciseDatabase()
        val equipmentType = if (equipment == "家庭") "home" else "gym"
        val difficultyLevel = when (experience) {
            "新手" -> "beginner"
            "中级" -> "intermediate"
            "高级" -> "advanced"
            else -> "beginner"
        }
        
        val split = when (trainingDays) {
            3 -> "全身训练"
            4 -> "上下肢分化"
            5 -> "推拉腿"
            6 -> "推拉腿x2"
            else -> "全身训练"
        }
        
        val weeklyPlan = generateWeeklyPlan(trainingDays, split, goal, difficultyLevel, equipmentType, targetMuscles, exerciseDb)
        
        val title = when {
            goal == "增肌" && experience == "新手" -> "新手增肌计划 - $split"
            goal == "增肌" && experience == "中级" -> "中级增肌计划 - $split"
            goal == "增肌" && experience == "高级" -> "高级增肌计划 - $split"
            goal == "减脂" && experience == "新手" -> "新手减脂计划 - $split"
            goal == "减脂" && experience == "中级" -> "中级减脂计划 - $split"
            goal == "减脂" && experience == "高级" -> "高级减脂计划 - $split"
            else -> "维持计划 - $split"
        }
        
        val description = "每周${trainingDays}天训练，采用${split}方式"
        
        return GeneratedPlan(title = title, description = description, details = weeklyPlan)
    }
    
    private fun generateWeeklyPlan(
        days: Int, split: String, goal: String, difficulty: String,
        equipment: String, targetMuscles: List<String>, exerciseDb: List<Exercise>
    ): String {
        val plan = StringBuilder()
        plan.append("【训练分化方式】$split\n")
        plan.append("【训练频率】每周${days}天\n\n")
        
        when (split) {
            "全身训练" -> plan.append(generateFullBodyPlan(days, goal, difficulty, equipment, targetMuscles, exerciseDb))
            "上下肢分化" -> plan.append(generateUpperLowerSplit(goal, difficulty, equipment, exerciseDb))
            "推拉腿", "推拉腿x2" -> plan.append(generatePushPullLegsSplit(days, goal, difficulty, equipment, exerciseDb))
        }
        
        plan.append("\n\n")
        plan.append(generatePeriodization(goal, difficulty))
        
        return plan.toString()
    }

    
    private fun generateFullBodyPlan(days: Int, goal: String, difficulty: String, equipment: String, targetMuscles: List<String>, exerciseDb: List<Exercise>): String {
        val plan = StringBuilder()
        val sets = if (goal == "减脂") "3-4" else "4-5"
        val reps = when (goal) { "增肌" -> "8-12"; "减脂" -> "12-15"; else -> "10-12" }
        
        for (day in 1..days) {
            plan.append("═══ 第${day}天：全身训练 ═══\n\n")
            val chest = selectExercise(exerciseDb, "胸部", difficulty, equipment, "compound")
            val back = selectExercise(exerciseDb, "背部", difficulty, equipment, "compound")
            val legs = selectExercise(exerciseDb, "腿部", difficulty, equipment, "compound")
            val shoulders = selectExercise(exerciseDb, "肩部", difficulty, equipment, "compound")
            val core = selectExercise(exerciseDb, "核心", difficulty, equipment, "isolation")
            
            plan.append("1. ${chest?.name ?: "卧推"} - ${sets}组 x ${reps}次\n")
            plan.append("2. ${back?.name ?: "划船"} - ${sets}组 x ${reps}次\n")
            plan.append("3. ${legs?.name ?: "深蹲"} - ${sets}组 x ${reps}次\n")
            plan.append("4. ${shoulders?.name ?: "推举"} - 3组 x ${reps}次\n")
            plan.append("5. ${core?.name ?: "平板支撑"} - 3组 x 30-60秒\n\n")
            
            if (targetMuscles.isNotEmpty()) {
                plan.append("【强化动作】\n")
                targetMuscles.forEach { muscle ->
                    selectExercise(exerciseDb, muscle, difficulty, equipment, "isolation")?.let {
                        plan.append("• ${it.name} - 3组 x 12-15次\n")
                    }
                }
                plan.append("\n")
            }
            if (goal == "减脂") plan.append("【有氧】跑步机/椭圆机 20-30分钟\n\n")
            plan.append("组间休息：${if (goal == "减脂") "45-60秒" else "60-90秒"}\n")
            plan.append("────────────────────\n\n")
        }
        return plan.toString()
    }
    
    private fun generateUpperLowerSplit(goal: String, difficulty: String, equipment: String, exerciseDb: List<Exercise>): String {
        val plan = StringBuilder()
        val sets = if (goal == "减脂") "3-4" else "4-5"
        val reps = when (goal) { "增肌" -> "6-10"; "减脂" -> "12-15"; else -> "8-12" }
        
        plan.append("【训练安排】周一：上肢 | 周二：下肢 | 周四：上肢 | 周五：下肢\n\n")
        plan.append("═══ 上肢训练日 ═══\n")
        plan.append("1. ${selectExercise(exerciseDb, "胸部", difficulty, equipment, "compound")?.name ?: "卧推"} - ${sets}组 x ${reps}次\n")
        plan.append("2. ${selectExercise(exerciseDb, "背部", difficulty, equipment, "compound")?.name ?: "划船"} - ${sets}组 x ${reps}次\n")
        plan.append("3. ${selectExercise(exerciseDb, "肩部", difficulty, equipment, "compound")?.name ?: "推举"} - 4组 x 8-12次\n")
        plan.append("4. ${selectExercise(exerciseDb, "手臂", difficulty, equipment, "isolation")?.name ?: "弯举"} - 3组 x 10-12次\n\n")
        
        plan.append("═══ 下肢训练日 ═══\n")
        plan.append("1. ${selectExercise(exerciseDb, "腿部", difficulty, equipment, "compound")?.name ?: "深蹲"} - ${sets}组 x ${reps}次\n")
        plan.append("2. 腿举 - 4组 x 10-15次\n")
        plan.append("3. 腿弯举 - 4组 x 10-15次\n")
        plan.append("4. 提踵 - 4组 x 15-20次\n")
        plan.append("5. ${selectExercise(exerciseDb, "核心", difficulty, equipment, "isolation")?.name ?: "平板支撑"} - 3组\n")
        if (goal == "减脂") plan.append("\n【有氧】HIIT 15-20分钟\n")
        return plan.toString()
    }
    
    private fun generatePushPullLegsSplit(days: Int, goal: String, difficulty: String, equipment: String, exerciseDb: List<Exercise>): String {
        val plan = StringBuilder()
        val sets = if (goal == "减脂") "3-4" else "4-5"
        val reps = if (goal == "增肌") "6-10" else "8-12"
        
        if (days == 6) plan.append("【训练安排】周一：推 | 周二：拉 | 周三：腿 | 周四：推 | 周五：拉 | 周六：腿\n\n")
        else plan.append("【训练安排】周一：推 | 周三：拉 | 周五：腿\n\n")
        
        plan.append("═══ 推日（胸、肩、三头）═══\n")
        plan.append("1. ${selectExercise(exerciseDb, "胸部", difficulty, equipment, "compound")?.name ?: "卧推"} - ${sets}组 x ${reps}次\n")
        plan.append("2. 上斜卧推 - 4组 x 8-12次\n")
        plan.append("3. ${selectExercise(exerciseDb, "肩部", difficulty, equipment, "compound")?.name ?: "推举"} - 4组 x 8-12次\n")
        plan.append("4. 侧平举 - 4组 x 12-15次\n")
        plan.append("5. 三头下压 - 3组 x 12-15次\n\n")
        
        plan.append("═══ 拉日（背、二头）═══\n")
        plan.append("1. ${selectExercise(exerciseDb, "背部", difficulty, equipment, "compound")?.name ?: "硬拉"} - ${sets}组 x ${reps}次\n")
        plan.append("2. 引体向上/高位下拉 - 4组 x 最大次数\n")
        plan.append("3. 划船 - 4组 x 8-12次\n")
        plan.append("4. ${selectExercise(exerciseDb, "手臂", difficulty, equipment, "isolation")?.name ?: "弯举"} - 4组 x 8-12次\n\n")
        
        plan.append("═══ 腿日（腿部、核心）═══\n")
        plan.append("1. ${selectExercise(exerciseDb, "腿部", difficulty, equipment, "compound")?.name ?: "深蹲"} - ${sets}组 x ${reps}次\n")
        plan.append("2. 腿举 - 4组 x 10-15次\n")
        plan.append("3. 腿弯举 - 4组 x 10-15次\n")
        plan.append("4. 提踵 - 5组 x 15-20次\n")
        plan.append("5. ${selectExercise(exerciseDb, "核心", difficulty, equipment, "isolation")?.name ?: "卷腹"} - 3组\n")
        return plan.toString()
    }
    
    private fun selectExercise(exerciseDb: List<Exercise>, targetMuscle: String, difficulty: String, equipment: String, type: String): Exercise? {
        return exerciseDb.filter { it.targetMuscle == targetMuscle && it.equipment == equipment && it.type == type && (it.difficulty == difficulty || it.difficulty == "beginner") }.randomOrNull()
            ?: exerciseDb.filter { it.targetMuscle == targetMuscle && it.type == type }.randomOrNull()
    }
    
    private fun generatePeriodization(goal: String, difficulty: String): String {
        return when (goal) {
            "增肌" -> "【周期化建议】\n第1-2周：适应期，60-70% 1RM\n第3-4周：力量期，75-80% 1RM\n第5-6周：增肌期，增加组数\n第7-8周：恢复周，减少容量30%\n\n【营养建议】热量盈余+300-500卡/天，蛋白质2.0-2.5g/kg体重"
            "减脂" -> "【周期化建议】\n第1-2周：适应期，热量赤字-300卡\n第3-4周：加速期，热量赤字-400卡，加入HIIT\n第5周：冲刺期，热量赤字-500卡\n第6周：恢复周\n\n【营养建议】高蛋白1.8-2.2g/kg体重，多喝水"
            else -> "【周期化建议】\n第1周：力量周\n第2周：耐力周\n第3周：平衡周\n第4周：活动周\n\n【营养建议】热量平衡，蛋白质1.6-2.0g/kg体重"
        }
    }
    
    data class GeneratedPlan(val title: String, val description: String, val details: String)
    
    data class PlanData(
        val title: String, val description: String, val details: String,
        val goal: String, val experience: String, val targetMuscles: List<String>,
        val trainingDays: Int, val trainingDuration: String, val equipment: String
    )
}
