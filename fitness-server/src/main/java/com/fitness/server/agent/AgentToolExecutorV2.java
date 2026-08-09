package com.fitness.server.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.server.agent.dto.ToolResultDto.*;
import com.fitness.server.entity.*;
import com.fitness.server.mapper.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 工具执行器
 * 
 * 改进：
 * 1. 参数强校验
 * 2. 结构化DTO输出（避免JSON注入）
 * 3. 肌群恢复状态判断
 * 4. 训练摘要包含肌群分布
 */
@Component
public class AgentToolExecutorV2 {
    
    @Autowired
    private WorkoutMapper workoutMapper;
    
    @Autowired
    private WorkoutExerciseMapper workoutExerciseMapper;
    
    @Autowired
    private DietRecordMapper dietRecordMapper;
    
    @Autowired
    private BodyRecordMapper bodyRecordMapper;
    
    @Autowired
    private TrainingPlanMapper trainingPlanMapper;

    @Autowired
    private ToolContractRegistry toolContractRegistry;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final Map<ToolContract.ExecutorBinding, ToolHandler> toolHandlers = createToolHandlers();
    
    /**
     * 执行带请求级授权上下文的工具调用。
     */
    public String executeTool(
            Long userId,
            Collection<String> allowedToolNames,
            String toolName,
            String argumentsJson
    ) throws ToolExecutionException {
        if (!toolContractRegistry.isRegistered(toolName)) {
            throw new ToolExecutionException("工具未注册: " + toolName);
        }
        if (allowedToolNames == null || !allowedToolNames.contains(toolName)) {
            throw new ToolExecutionException("工具不在当前请求的授权范围内: " + toolName);
        }

        try {
            ToolContract contract = toolContractRegistry.getContract(toolName);
            Map<String, Object> params = contract.validateAndNormalize(argumentsJson);
            ToolHandler handler = toolHandlers.get(contract.getExecutorBinding());
            if (handler == null) {
                throw new ToolExecutionException("工具未绑定执行器: " + toolName);
            }
            Object result = handler.execute(userId, params);
            
            // 3. 结构化序列化
            return objectMapper.writeValueAsString(result);
            
        } catch (ToolExecutionException e) {
            throw e;
        } catch (ToolContract.ValidationException e) {
            throw ToolExecutionException.invalidArguments(e.getMessage());
        } catch (Exception e) {
            throw ToolExecutionException.technicalFailure(e.getMessage(), e);
        }
    }
    
    @PostConstruct
    void validateContractBindings() {
        toolContractRegistry.validateExecutorBindings(getExecutorBindings());
    }

    private Map<ToolContract.ExecutorBinding, ToolHandler> createToolHandlers() {
        Map<ToolContract.ExecutorBinding, ToolHandler> handlers = new EnumMap<>(ToolContract.ExecutorBinding.class);
        handlers.put(ToolContract.ExecutorBinding.TRAINING_SUMMARY, (userId, params) -> getTrainingSummary(userId, (Integer) params.get("rangeDays")));
        handlers.put(ToolContract.ExecutorBinding.RECENT_WORKOUTS, (userId, params) -> getRecentWorkouts(userId, (Integer) params.get("rangeDays")));
        handlers.put(ToolContract.ExecutorBinding.BODY_TREND, (userId, params) -> getBodyTrend(userId, (Integer) params.get("rangeDays")));
        handlers.put(ToolContract.ExecutorBinding.TODAY_DIET_SUMMARY, (userId, params) -> getTodayDietSummary(userId));
        handlers.put(ToolContract.ExecutorBinding.RECOVERY_STATUS, (userId, params) -> getRecoveryStatus(userId));
        handlers.put(ToolContract.ExecutorBinding.ACTIVE_TRAINING_PLAN, (userId, params) -> getActiveTrainingPlan(userId));
        handlers.put(ToolContract.ExecutorBinding.CREATE_TRAINING_PLAN_DRAFT, (userId, params) -> createTrainingPlanDraft(userId, (String) params.get("_raw")));
        handlers.put(ToolContract.ExecutorBinding.CREATE_DIET_RECORD_DRAFT, (userId, params) -> createDietRecordDraft(userId, (String) params.get("_raw")));
        handlers.put(ToolContract.ExecutorBinding.DIET_SUMMARY, (userId, params) -> getDietSummary(userId, (String) params.get("date"), (Integer) params.get("rangeDays")));
        handlers.put(ToolContract.ExecutorBinding.TRAINING_SCHEDULE, (userId, params) -> getTrainingSchedule(userId, (String) params.get("date")));
        handlers.put(ToolContract.ExecutorBinding.USER_FITNESS_PROFILE, (userId, params) -> getUserFitnessProfile(userId));
        handlers.put(ToolContract.ExecutorBinding.DAILY_NUTRITION_PROGRESS, (userId, params) -> getDailyNutritionProgress(userId, (String) params.get("date")));
        handlers.put(ToolContract.ExecutorBinding.TRAINING_PROGRESS, (userId, params) -> getTrainingProgress(userId, (Integer) params.get("rangeDays")));
        return Collections.unmodifiableMap(handlers);
    }

    Set<ToolContract.ExecutorBinding> getExecutorBindings() {
        return EnumSet.copyOf(toolHandlers.keySet());
    }

    @FunctionalInterface
    private interface ToolHandler {
        Object execute(Long userId, Map<String, Object> params) throws Exception;
    }

    /**
     * 工具执行异常
     */
    public static class ToolExecutionException extends Exception {
        private final String code;
        private final boolean retryable;

        public ToolExecutionException(String message) {
            this("tool_execution_failed", message, false, null);
        }

        public ToolExecutionException(String message, Throwable cause) {
            this("tool_execution_failed", message, true, cause);
        }

        private ToolExecutionException(String code, String message, boolean retryable, Throwable cause) {
            super(message, cause);
            this.code = code;
            this.retryable = retryable;
        }

        public static ToolExecutionException invalidArguments(String message) {
            return new ToolExecutionException("invalid_arguments", message, false, null);
        }

        public static ToolExecutionException technicalFailure(String message, Throwable cause) {
            return new ToolExecutionException("tool_execution_failed", "工具执行技术错误: " + message, true, cause);
        }

        public String getCode() {
            return code;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
    
    /**
     * 获取训练摘要（含肌群分布）
     */
    private TrainingSummaryResult getTrainingSummary(Long userId, int rangeDays) {
        long cutoffTime = System.currentTimeMillis() - (rangeDays * 24L * 60 * 60 * 1000);
        
        QueryWrapper<Workout> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("date", cutoffTime)
               .orderByDesc("date");
        
        List<Workout> workouts = workoutMapper.selectList(wrapper);
        
        TrainingSummaryResult result = new TrainingSummaryResult();
        result.setTotalWorkouts(workouts.size());
        
        if (workouts.isEmpty()) {
            result.setTotalDurationMinutes(0);
            result.setMuscleGroupDistribution(new HashMap<>());
            result.setRecentWorkouts(new ArrayList<>());
            return result;
        }
        
        // 计算总时长（过滤null值）
        long totalDuration = workouts.stream()
            .filter(w -> w.getDuration() != null)
            .mapToLong(Workout::getDuration)
            .sum();
        result.setTotalDurationMinutes(totalDuration / 60000);
        
        // 计算肌群分布
        Map<String, Integer> muscleGroupDistribution = new HashMap<>();
        for (Workout workout : workouts) {
            QueryWrapper<WorkoutExercise> exWrapper = new QueryWrapper<>();
            exWrapper.eq("workout_id", workout.getId());
            List<WorkoutExercise> exercises = workoutExerciseMapper.selectList(exWrapper);
            
            for (WorkoutExercise exercise : exercises) {
                String muscleGroup = getMuscleGroupFromExerciseName(exercise.getExerciseName());
                muscleGroupDistribution.merge(muscleGroup, 1, Integer::sum);
            }
        }
        result.setMuscleGroupDistribution(muscleGroupDistribution);
        
        // 最近训练
        List<RecentWorkoutItem> recentItems = workouts.stream()
            .limit(3)
            .map(w -> {
                RecentWorkoutItem item = new RecentWorkoutItem();
                item.setDate(formatDate(w.getDate()));
                item.setName(w.getName());
                item.setDurationMinutes(w.getDuration() / 60000);
                
                QueryWrapper<WorkoutExercise> exWrapper = new QueryWrapper<>();
                exWrapper.eq("workout_id", w.getId());
                List<String> exerciseNames = workoutExerciseMapper.selectList(exWrapper)
                    .stream()
                    .map(WorkoutExercise::getExerciseName)
                    .collect(Collectors.toList());
                item.setExercises(exerciseNames);
                
                return item;
            })
            .collect(Collectors.toList());
        result.setRecentWorkouts(recentItems);
        
        return result;
    }
    
    /**
     * 获取指定最近天数内的训练列表
     */
    private RecentWorkoutsResult getRecentWorkouts(Long userId, int rangeDays) {
        long cutoffTime = System.currentTimeMillis() - (rangeDays * 24L * 60 * 60 * 1000);
        QueryWrapper<Workout> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("date", cutoffTime)
               .orderByDesc("date");
        
        List<Workout> workouts = workoutMapper.selectList(wrapper);
        
        List<WorkoutDetail> details = workouts.stream()
            .map(w -> {
                WorkoutDetail detail = new WorkoutDetail();
                detail.setDate(formatDate(w.getDate()));
                detail.setName(w.getName());
                detail.setDurationMinutes(w.getDuration() / 60000);
                detail.setNotes(w.getNotes());
                
                QueryWrapper<WorkoutExercise> exWrapper = new QueryWrapper<>();
                exWrapper.eq("workout_id", w.getId())
                         .orderByAsc("exercise_order");
                List<WorkoutExercise> exercises = workoutExerciseMapper.selectList(exWrapper);
                
                List<ExerciseSummary> exerciseSummaries = exercises.stream()
                    .map(ex -> {
                        ExerciseSummary summary = new ExerciseSummary();
                        summary.setName(ex.getExerciseName());
                        summary.setMuscleGroup(getMuscleGroupFromExerciseName(ex.getExerciseName()));
                        
                        // 统计组数
                        QueryWrapper<WorkoutSet> setWrapper = new QueryWrapper<>();
                        setWrapper.eq("exercise_id", ex.getId());
                        int setCount = workoutExerciseMapper.selectSetsByExerciseId(ex.getId()).size();
                        summary.setSets(setCount);
                        
                        return summary;
                    })
                    .collect(Collectors.toList());
                detail.setExercises(exerciseSummaries);
                
                return detail;
            })
            .collect(Collectors.toList());
        
        RecentWorkoutsResult result = new RecentWorkoutsResult();
        result.setWorkouts(details);
        return result;
    }
    
    /**
     * 获取身体数据趋势
     */
    private BodyTrendResult getBodyTrend(Long userId, int rangeDays) {
        long cutoffTime = System.currentTimeMillis() - (rangeDays * 24L * 60 * 60 * 1000);
        
        QueryWrapper<BodyRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("date", cutoffTime)
               .orderByAsc("date");
        
        List<BodyRecord> records = bodyRecordMapper.selectList(wrapper);
        
        BodyTrendResult result = new BodyTrendResult();
        
        if (records.isEmpty()) {
            result.setWeight(createEmptyMetric());
            result.setBodyFat(createEmptyMetric());
            result.setMuscleMass(createEmptyMetric());
            return result;
        }
        
        BodyRecord earliest = records.get(0);
        BodyRecord latest = records.get(records.size() - 1);
        
        result.setWeight(calculateMetric(earliest.getWeight(), latest.getWeight()));
        result.setBodyFat(calculateMetric(earliest.getBodyFat(), latest.getBodyFat()));
        result.setMuscleMass(calculateMetric(earliest.getMuscleMass(), latest.getMuscleMass()));
        
        return result;
    }
    
    /**
     * 获取今日饮食摘要
     */
    private TodayDietResult getTodayDietSummary(Long userId) {
        String today = LocalDate.now().format(dateFormatter);
        
        QueryWrapper<DietRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .apply("DATE_FORMAT(FROM_UNIXTIME(date/1000), '%Y-%m-%d') = {0}", today);
        
        List<DietRecord> records = dietRecordMapper.selectList(wrapper);
        
        TodayDietResult result = new TodayDietResult();
        result.setTotalCalories(records.stream().mapToInt(DietRecord::getCalories).sum());
        result.setTotalProtein((float) records.stream().mapToDouble(DietRecord::getProtein).sum());
        result.setTotalCarbs((float) records.stream().mapToDouble(DietRecord::getCarbs).sum());
        result.setTotalFat((float) records.stream().mapToDouble(DietRecord::getFat).sum());
        result.setMealCount(records.size());
        
        return result;
    }
    
    /**
     * 获取恢复状态（基于肌群）
     */
    private RecoveryStatusResult getRecoveryStatus(Long userId) {
        QueryWrapper<Workout> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("date")
               .last("LIMIT 10");
        
        List<Workout> recentWorkouts = workoutMapper.selectList(wrapper);
        
        RecoveryStatusResult result = new RecoveryStatusResult();
        
        if (recentWorkouts.isEmpty()) {
            result.setDaysSinceLastWorkout(-1);
            result.setMuscleGroups(new HashMap<>());
            result.setOverallStatus("无训练记录");
            result.setRecommendations(List.of("开始你的第一次训练吧！"));
            return result;
        }
        
        // 计算距离上次训练天数
        long lastWorkoutTime = recentWorkouts.get(0).getDate();
        long daysSince = (System.currentTimeMillis() - lastWorkoutTime) / (24 * 60 * 60 * 1000);
        result.setDaysSinceLastWorkout((int) daysSince);
        
        // 计算各肌群最后训练时间
        Map<String, MuscleGroupRecovery> muscleGroupRecovery = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (Workout workout : recentWorkouts) {
            QueryWrapper<WorkoutExercise> exWrapper = new QueryWrapper<>();
            exWrapper.eq("workout_id", workout.getId());
            List<WorkoutExercise> exercises = workoutExerciseMapper.selectList(exWrapper);
            
            for (WorkoutExercise exercise : exercises) {
                String muscleGroup = getMuscleGroupFromExerciseName(exercise.getExerciseName());
                
                if (!muscleGroupRecovery.containsKey(muscleGroup)) {
                    int daysSinceGroup = (int) ((now - workout.getDate()) / (24 * 60 * 60 * 1000));
                    MuscleGroupRecovery recovery = new MuscleGroupRecovery();
                    recovery.setDaysSinceLastTrained(daysSinceGroup);
                    recovery.setLastTrainedDate(formatDate(workout.getDate()));
                    recovery.setStatus(getRecoveryStatusForDays(daysSinceGroup));
                    muscleGroupRecovery.put(muscleGroup, recovery);
                }
            }
        }
        
        result.setMuscleGroups(muscleGroupRecovery);
        result.setOverallStatus(calculateOverallStatus(daysSince));
        result.setRecommendations(generateRecommendations(muscleGroupRecovery, daysSince));
        
        return result;
    }
    
    /**
     * 查询服务端计划：最近更新的三条与全部置顶计划共用去重实体列表。
     */
    private ActivePlanResult getActiveTrainingPlan(Long userId) {
        List<TrainingPlan> recentPlans = trainingPlanMapper.selectList(new QueryWrapper<TrainingPlan>()
            .eq("user_id", userId)
            .orderByDesc("updated_at")
            .last("LIMIT 3"));
        List<TrainingPlan> pinnedPlans = trainingPlanMapper.selectList(new QueryWrapper<TrainingPlan>()
            .eq("user_id", userId)
            .eq("is_pinned", true)
            .orderByDesc("updated_at"));

        LinkedHashMap<Long, PlanInfo> distinctPlans = new LinkedHashMap<>();
        List<Long> recentPlanIds = new ArrayList<>();
        List<Long> pinnedPlanIds = new ArrayList<>();
        for (TrainingPlan plan : recentPlans) {
            recentPlanIds.add(plan.getId());
            distinctPlans.putIfAbsent(plan.getId(), toPlanInfo(plan));
        }
        for (TrainingPlan plan : pinnedPlans) {
            pinnedPlanIds.add(plan.getId());
            distinctPlans.putIfAbsent(plan.getId(), toPlanInfo(plan));
        }

        ActivePlanResult result = new ActivePlanResult();
        result.setHasPlan(!pinnedPlans.isEmpty());
        result.setPlan(pinnedPlans.isEmpty() ? null : toPlanInfo(pinnedPlans.get(0)));
        result.setPlans(new ArrayList<>(distinctPlans.values()));
        result.setRecentPlanIds(recentPlanIds);
        result.setPinnedPlanIds(pinnedPlanIds);
        return result;
    }

    private PlanInfo toPlanInfo(TrainingPlan plan) {
        PlanInfo info = new PlanInfo();
        info.setId(plan.getId());
        info.setTitle(plan.getTitle());
        info.setDescription(plan.getDescription());
        info.setDetails(plan.getDetails());
        info.setGoal(plan.getGoal());
        info.setExperience(plan.getExperience());
        info.setTrainingDays(plan.getTrainingDays() == null ? 0 : plan.getTrainingDays());
        info.setTargetMuscles(plan.getTargetMuscles());
        info.setTrainingDuration(plan.getTrainingDuration());
        info.setEquipment(plan.getEquipment());
        info.setPinned(Boolean.TRUE.equals(plan.getIsPinned()));
        info.setUpdatedAt(plan.getUpdatedAt());
        return info;
    }
    
    // ===== 辅助方法 =====
    
    private String getMuscleGroupFromExerciseName(String exerciseName) {
        // 空值保护
        if (exerciseName == null || exerciseName.isEmpty()) {
            return "未知";
        }
        
        // 简单的关键词匹配，实际应该查询exercise表
        if (exerciseName.contains("卧推") || exerciseName.contains("飞鸟") || exerciseName.contains("胸")) {
            return "胸部";
        } else if (exerciseName.contains("深蹲") || exerciseName.contains("腿举") || exerciseName.contains("腿")) {
            return "腿部";
        } else if (exerciseName.contains("硬拉") || exerciseName.contains("划船") || exerciseName.contains("背")) {
            return "背部";
        } else if (exerciseName.contains("推举") || exerciseName.contains("侧平举") || exerciseName.contains("肩")) {
            return "肩部";
        } else if (exerciseName.contains("弯举") || exerciseName.contains("二头")) {
            return "手臂";
        } else if (exerciseName.contains("臂屈伸") || exerciseName.contains("三头")) {
            return "手臂";
        } else if (exerciseName.contains("卷腹") || exerciseName.contains("腹")) {
            return "腹部";
        }
        return "其他";
    }
    
    private String getRecoveryStatusForDays(int days) {
        if (days == 0) return "今天刚练";
        if (days == 1) return "恢复中";
        if (days <= 2) return "基本恢复";
        if (days <= 4) return "完全恢复";
        return "休息过久";
    }
    
    private String calculateOverallStatus(long days) {
        if (days == 0) return "今天刚训练";
        if (days == 1) return "恢复中";
        if (days <= 2) return "可以训练";
        if (days <= 4) return "建议训练";
        return "休息过久，尽快恢复训练";
    }
    
    private List<String> generateRecommendations(Map<String, MuscleGroupRecovery> muscleGroups, long daysSince) {
        List<String> recommendations = new ArrayList<>();
        
        if (daysSince >= 3) {
            recommendations.add("距离上次训练已" + daysSince + "天，建议尽快恢复训练");
        }
        
        muscleGroups.forEach((group, recovery) -> {
            if (recovery.getDaysSinceLastTrained() >= 4) {
                recommendations.add(group + "已休息" + recovery.getDaysSinceLastTrained() + "天，可以安排训练");
            }
        });
        
        if (recommendations.isEmpty()) {
            recommendations.add("继续保持训练节奏");
        }
        
        return recommendations;
    }
    
    private BodyMetric createEmptyMetric() {
        BodyMetric metric = new BodyMetric();
        metric.setLatest(null);
        metric.setEarliest(null);
        metric.setChange(null);
        metric.setTrend("无数据");
        return metric;
    }
    
    private BodyMetric calculateMetric(Float earliest, Float latest) {
        BodyMetric metric = new BodyMetric();
        metric.setEarliest(earliest);
        metric.setLatest(latest);
        
        if (earliest != null && latest != null) {
            float change = latest - earliest;
            metric.setChange(change);
            metric.setTrend(change > 0 ? "上升" : (change < 0 ? "下降" : "持平"));
        } else {
            metric.setChange(null);
            metric.setTrend("数据不足");
        }
        
        return metric;
    }
    
    private String formatDate(long timestamp) {
        return LocalDate.ofInstant(
            new Date(timestamp).toInstant(),
            ZoneId.systemDefault()
        ).format(dateFormatter);
    }
    
    // ===== 阶段C：写入工具 =====
    
    /**
     * 创建训练计划草案
     * 注意：这只是生成草案数据，不实际写入数据库
     * 实际写入由confirmAction接口完成
     */
    private Map<String, Object> createTrainingPlanDraft(Long userId, String argumentsJson) throws Exception {
        // 解析草案内容
        com.fitness.server.agent.dto.TrainingPlanDraftDto draft = 
            objectMapper.readValue(argumentsJson, com.fitness.server.agent.dto.TrainingPlanDraftDto.class);
        
        // 数据规范化（确保格式统一）
        draft = normalizeTrainingPlanDraft(draft);
        
        // 校验必填字段
        if (draft.getTitle() == null || draft.getTitle().isBlank()) {
            throw new ToolExecutionException("训练计划标题不能为空");
        }
        if (draft.getDescription() != null && draft.getDescription().length() > 80) {
            throw new ToolExecutionException("训练计划描述不能超过80字");
        }
        if (draft.getGoal() == null || draft.getGoal().isBlank()) {
            throw new ToolExecutionException("训练目标不能为空");
        }
        if (draft.getTrainingDays() == null || draft.getTrainingDays() < 1 || draft.getTrainingDays() > 7) {
            throw new ToolExecutionException("训练天数必须在1-7之间");
        }
        if (draft.getDays() == null || draft.getDays().isEmpty()) {
            throw new ToolExecutionException("训练安排不能为空");
        }
        
        // 校验天数与训练安排匹配
        if (draft.getDays().size() != draft.getTrainingDays()) {
            throw new ToolExecutionException("训练天数与训练安排天数不匹配");
        }
        
        // 验证每天的训练动作
        for (com.fitness.server.agent.dto.TrainingPlanDraftDto.DayPlan day : draft.getDays()) {
            if (day.getName() == null || day.getName().isBlank() || day.getFocus() == null || day.getFocus().isBlank()) {
                throw new ToolExecutionException("每天训练必须包含名称和重点");
            }
            if (day.getExercises() == null || day.getExercises().isEmpty()) {
                throw new ToolExecutionException("每天训练必须包含至少一个动作");
            }
            if (day.getExercises().size() > 5) {
                throw new ToolExecutionException("每天训练最多包含5个动作");
            }
            
            for (com.fitness.server.agent.dto.TrainingPlanDraftDto.ExerciseItem exercise : day.getExercises()) {
                if (exercise.getName() == null || exercise.getName().isBlank()) {
                    throw new ToolExecutionException("动作名称不能为空");
                }
                if (exercise.getSets() == null || exercise.getSets() < 1 || exercise.getSets() > 10) {
                    throw new ToolExecutionException("组数必须在1-10之间");
                }
                if (exercise.getReps() == null || !exercise.getReps().matches("\\d+(?:-\\d+)?次")) {
                    throw new ToolExecutionException("次数必须是数字或数字范围并带“次”单位");
                }
                if (exercise.getRestTime() == null || !exercise.getRestTime().matches("\\d+(?:秒|分钟)")) {
                    throw new ToolExecutionException("休息时间必须是数字并带“秒”或“分钟”单位");
                }
                if (exercise.getNotes() != null && exercise.getNotes().length() > 15) {
                    throw new ToolExecutionException("动作要点不能超过15字");
                }
            }
        }
        
        // 验证experience字段（Android端要求）
        if (draft.getExperience() == null || draft.getExperience().isEmpty()) {
            throw new ToolExecutionException("训练经验(experience)不能为空");
        }
        
        // 返回草案已创建的确认信息（包含规范化后的JSON）
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "训练计划草案已创建，等待用户确认");
        result.put("draftSummary", Map.of(
            "title", draft.getTitle(),
            "goal", draft.getGoal(),
            "trainingDays", draft.getTrainingDays(),
            "totalExercises", draft.getDays().stream()
                .mapToInt(day -> day.getExercises().size())
                .sum()
        ));
        // 添加规范化后的完整JSON，供createPendingAction使用
        result.put("normalized_draft_json", objectMapper.writeValueAsString(draft));
        
        return result;
    }
    
    /**
     * 规范化训练计划草案数据
     * 统一格式，确保数据一致性
     */
    private com.fitness.server.agent.dto.TrainingPlanDraftDto normalizeTrainingPlanDraft(
            com.fitness.server.agent.dto.TrainingPlanDraftDto draft) {
        
        if (draft.getDays() != null) {
            for (com.fitness.server.agent.dto.TrainingPlanDraftDto.DayPlan day : draft.getDays()) {
                if (day.getExercises() == null) {
                    continue;
                }
                for (com.fitness.server.agent.dto.TrainingPlanDraftDto.ExerciseItem exercise : day.getExercises()) {
                    if (exercise.getReps() != null) {
                        exercise.setReps(exercise.getReps().trim());
                    }
                    if (exercise.getRestTime() != null) {
                        exercise.setRestTime(exercise.getRestTime().trim());
                    }
                    if (exercise.getNotes() != null) {
                        exercise.setNotes(exercise.getNotes().trim());
                    }
                }
            }
        }
        
        return draft;
    }
    
    /**
     * 创建饮食记录草案
     * 注意：这只是生成草案数据，不实际写入数据库
     * 实际写入由Android端本地保存完成
     */
    private Map<String, Object> createDietRecordDraft(Long userId, String argumentsJson) throws Exception {
        // 解析草案内容
        com.fitness.server.agent.dto.DietRecordDraftDto draft = 
            objectMapper.readValue(argumentsJson, com.fitness.server.agent.dto.DietRecordDraftDto.class);
        
        // 日期处理：优先使用AI指定的日期，如果为空则使用当前日期
        if (draft.getDate() == null || draft.getDate().isEmpty()) {
            String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            draft.setDate(currentDate);
        }
        
        // 校验必填字段
        if (draft.getDate() == null || draft.getDate().isEmpty()) {
            throw new ToolExecutionException("日期不能为空");
        }
        
        // 验证日期格式
        if (!draft.getDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new ToolExecutionException("日期格式错误，应为yyyy-MM-dd");
        }
        
        // 验证日期合理性：不能是未来日期，不能超过1年前
        java.time.LocalDate recordDate = java.time.LocalDate.parse(draft.getDate());
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate oneYearAgo = today.minusYears(1);
        
        if (recordDate.isAfter(today)) {
            throw new ToolExecutionException("日期不能是未来日期");
        }
        if (recordDate.isBefore(oneYearAgo)) {
            throw new ToolExecutionException("日期不能超过一年前");
        }
        
        // 校验记录列表
        if (draft.getRecords() == null || draft.getRecords().isEmpty()) {
            throw new ToolExecutionException("饮食记录不能为空");
        }
        
        // 校验每条记录（统一校验标准）
        int totalCalories = 0;
        float totalProtein = 0;
        
        for (int i = 0; i < draft.getRecords().size(); i++) {
            com.fitness.server.agent.dto.DietRecordDraftDto.RecordItem record = draft.getRecords().get(i);
            String prefix = "第" + (i + 1) + "条记录：";
            
            // 餐次校验
            if (record.getMealType() == null || record.getMealType().isEmpty()) {
                throw new ToolExecutionException(prefix + "餐次类型不能为空");
            }
            if (!List.of("早餐", "午餐", "晚餐", "加餐").contains(record.getMealType())) {
                throw new ToolExecutionException(prefix + "餐次类型必须是：早餐、午餐、晚餐、加餐");
            }
            
            // 食物名称校验
            if (record.getFoodName() == null || record.getFoodName().isEmpty()) {
                throw new ToolExecutionException(prefix + "食物名称不能为空");
            }
            
            // 份量校验
            if (record.getAmount() == null || record.getAmount().isBlank()) {
                throw new ToolExecutionException(prefix + "份量不能为空");
            }
            
            // 营养值校验：必须存在且非负
            if (record.getCalories() == null) {
                throw new ToolExecutionException(prefix + "热量不能为空");
            }
            if (record.getCalories() < 0) {
                throw new ToolExecutionException(prefix + "热量不能为负数");
            }
            
            if (record.getProtein() == null) {
                throw new ToolExecutionException(prefix + "蛋白质不能为空");
            }
            if (record.getProtein() < 0) {
                throw new ToolExecutionException(prefix + "蛋白质不能为负数");
            }
            
            if (record.getCarbs() == null) {
                throw new ToolExecutionException(prefix + "碳水化合物不能为空");
            }
            if (record.getCarbs() < 0) {
                throw new ToolExecutionException(prefix + "碳水化合物不能为负数");
            }
            
            if (record.getFat() == null) {
                throw new ToolExecutionException(prefix + "脂肪不能为空");
            }
            if (record.getFat() < 0) {
                throw new ToolExecutionException(prefix + "脂肪不能为负数");
            }
            
            totalCalories += record.getCalories();
            totalProtein += record.getProtein();
        }
        
        // 序列化修正后的draft（包含正确的日期）
        String normalizedDraftJson = objectMapper.writeValueAsString(draft);
        
        // 返回草案已创建的确认信息
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "饮食记录草案已创建，等待用户确认");
        result.put("draftSummary", Map.of(
            "date", draft.getDate(),
            "recordCount", draft.getRecords().size(),
            "totalCalories", totalCalories,
            "totalProtein", totalProtein
        ));
        // 添加规范化后的完整JSON（包含修正的日期），供createPendingAction使用
        result.put("normalized_draft_json", normalizedDraftJson);
        
        return result;
    }
    
    // ===== Phase E P0: 新增查询工具 =====
    
    /**
     * 获取指定日期或日期范围的饮食摘要
     * 
     * @param userId 用户ID
     * @param date 起始日期 (yyyy-MM-dd)
     * @param rangeDays 查询天数范围
     * @return 饮食摘要结果
     */
    private DietSummaryResult getDietSummary(Long userId, String date, int rangeDays) {
        DietSummaryResult result = new DietSummaryResult();
        
        // 计算日期范围
        LocalDate startDate = LocalDate.parse(date);
        LocalDate endDate = startDate.plusDays(rangeDays - 1);
        
        // 修复：截断到今天，防止返回未来日期
        LocalDate today = LocalDate.now();
        if (endDate.isAfter(today)) {
            endDate = today;
        }
        
        DateRange dateRange = new DateRange();
        dateRange.setStart(startDate.format(dateFormatter));
        dateRange.setEnd(endDate.format(dateFormatter));
        result.setDateRange(dateRange);
        
        // 转换为时间戳（使用服务器时区统一处理）
        long startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        // 查询日期范围内的所有饮食记录
        QueryWrapper<DietRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("date", startTimestamp)
               .lt("date", endTimestamp)
               .orderByAsc("date");
        
        List<DietRecord> allRecords = dietRecordMapper.selectList(wrapper);
        
        // 按日期分组
        Map<String, List<DietRecord>> recordsByDate = allRecords.stream()
            .collect(Collectors.groupingBy(r -> formatDate(r.getDate())));
        
        // 构建每天的摘要
        List<DaySummary> days = new ArrayList<>();
        
        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            String dateStr = currentDate.format(dateFormatter);
            List<DietRecord> dayRecords = recordsByDate.getOrDefault(dateStr, new ArrayList<>());
            
            DaySummary daySummary = new DaySummary();
            daySummary.setDate(dateStr);
            daySummary.setMealCount(dayRecords.size());
            
            // 计算总营养值
            int totalCalories = dayRecords.stream().mapToInt(DietRecord::getCalories).sum();
            float totalProtein = (float) dayRecords.stream().mapToDouble(DietRecord::getProtein).sum();
            float totalCarbs = (float) dayRecords.stream().mapToDouble(DietRecord::getCarbs).sum();
            float totalFat = (float) dayRecords.stream().mapToDouble(DietRecord::getFat).sum();
            
            daySummary.setTotalCalories(totalCalories);
            daySummary.setTotalProtein(totalProtein);
            daySummary.setTotalCarbs(totalCarbs);
            daySummary.setTotalFat(totalFat);
            
            // 构建详细记录列表
            List<MealRecord> mealRecords = dayRecords.stream()
                .map(r -> {
                    MealRecord meal = new MealRecord();
                    meal.setMealType(r.getMealType());
                    meal.setFoodName(r.getFoodName());
                    meal.setAmount(r.getAmount());
                    meal.setCalories(r.getCalories());
                    meal.setProtein(r.getProtein());
                    meal.setCarbs(r.getCarbs());
                    meal.setFat(r.getFat());
                    return meal;
                })
                .collect(Collectors.toList());
            
            daySummary.setRecords(mealRecords);
            days.add(daySummary);
        }
        
        result.setDays(days);
        return result;
    }
    
    /**
     * 获取指定日期的训练计划安排
     * 
     * @param userId 用户ID
     * @param date 查询日期 (yyyy-MM-dd)
     * @return 训练安排结果
     */
    private TrainingScheduleResult getTrainingSchedule(Long userId, String date) {
        TrainingScheduleResult result = new TrainingScheduleResult();
        result.setDate(date);
        
        // 查询用户的活跃训练计划（isPinned = true 表示正在使用）
        QueryWrapper<TrainingPlan> planWrapper = new QueryWrapper<>();
        planWrapper.eq("user_id", userId)
                   .eq("is_pinned", true)
                   .orderByDesc("updated_at")
                   .last("LIMIT 1");
        
        TrainingPlan activePlan = trainingPlanMapper.selectOne(planWrapper);
        
        if (activePlan == null) {
            // 没有活跃计划
            result.setHasWorkout(false);
            result.setPlanTitle(null);
            result.setDayName(null);
            result.setFocus(null);
            result.setExercises(new ArrayList<>());
            return result;
        }
        
        // 解析计划详情（details字段存储的是训练安排）
        result.setPlanTitle(activePlan.getTitle());
        
        try {
            // 计算是第几天（基于计划创建时间和查询日期）
            LocalDate planStartDate = LocalDate.ofInstant(
                new Date(activePlan.getCreatedAt()).toInstant(),
                ZoneId.systemDefault()
            );
            LocalDate queryDate = LocalDate.parse(date);
            
            // 修复：检查查询日期是否早于计划生效日
            if (queryDate.isBefore(planStartDate)) {
                // 查询日期早于计划创建日，该日期没有训练安排
                result.setHasWorkout(false);
                result.setDayName(null);
                result.setFocus("该日期早于当前计划生效时间（" + planStartDate.format(dateFormatter) + "）");
                result.setExercises(new ArrayList<>());
                return result;
            }
            
            // 计算从计划开始到查询日期经过了多少天（保证非负）
            long daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(planStartDate, queryDate);
            
            // 修复：正确实现周循环逻辑
            // 1. 计算是一周内的第几天（0-6）
            int dayInWeek = (int) (daysSinceStart % 7);
            
            // 2. 判断该天是否在训练日范围内
            if (dayInWeek < activePlan.getTrainingDays()) {
                // 在训练日范围内，计算是训练计划的第几天
                int dayIndex = (int) (daysSinceStart / 7) * activePlan.getTrainingDays() + dayInWeek;
                // 对训练天数取模，实现计划内容循环
                dayIndex = dayIndex % activePlan.getTrainingDays();
            
                // 解析details JSON字段获取训练安排
                // details存储格式：可能是格式化文本或JSON字符串
                String details = activePlan.getDetails();
            
                // 尝试从格式化文本中提取信息
                // 格式示例："═══ 第1天：胸部+三头 ═══\n\n1. 杠铃卧推 - 4组 x 8-12次\n..."
                if (details != null && !details.isEmpty()) {
                    String[] sections = details.split("═══");
                
                    if (sections.length > dayIndex * 2 + 1) {
                        String daySection = sections[dayIndex * 2 + 1].trim();
                    
                        // 提取日期名称和重点
                        String[] headerParts = daySection.split("\n", 2);
                        if (headerParts.length > 0) {
                            String header = headerParts[0].trim();
                            String[] headerDetails = header.split("：", 2);
                        
                            if (headerDetails.length == 2) {
                                result.setHasWorkout(true);
                                result.setDayName(headerDetails[0].trim());
                                result.setFocus(headerDetails[1].trim());
                            
                                // 解析动作列表
                                if (headerParts.length > 1) {
                                    List<ScheduledExercise> exercises = parseExercisesFromText(headerParts[1]);
                                    result.setExercises(exercises);
                                } else {
                                    result.setExercises(new ArrayList<>());
                                }
                            } else {
                                // 格式不符合预期，返回无训练
                                result.setHasWorkout(false);
                                result.setExercises(new ArrayList<>());
                            }
                        } else {
                            result.setHasWorkout(false);
                            result.setExercises(new ArrayList<>());
                        }
                    } else {
                        // 索引超出范围
                        result.setHasWorkout(false);
                        result.setExercises(new ArrayList<>());
                    }
                } else {
                    // 没有详细信息
                    result.setHasWorkout(false);
                    result.setExercises(new ArrayList<>());
                }
            } else {
                // 修复：当天是休息日（周内位置超出训练天数）
                result.setHasWorkout(false);
                result.setDayName("休息日");
                result.setFocus("今天是休息日，可以进行轻度拉伸或散步");
                result.setExercises(new ArrayList<>());
            }
            
        } catch (Exception e) {
            // 解析失败，返回基本信息
            result.setHasWorkout(false);
            result.setDayName(null);
            result.setFocus(null);
            result.setExercises(new ArrayList<>());
        }
        
        return result;
    }
    
    /**
     * 从格式化文本中解析动作列表
     * 
     * 格式示例：
     * 1. 杠铃卧推 - 4组 x 8-12次
     *    提示：注意控制节奏
     *    组间休息：90秒
     */
    private List<ScheduledExercise> parseExercisesFromText(String text) {
        List<ScheduledExercise> exercises = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return exercises;
        }
        
        String[] lines = text.split("\n");
        ScheduledExercise currentExercise = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("────")) {
                continue;
            }
            
            // 匹配动作行：数字. 动作名 - 组数组 x 次数
            if (line.matches("^\\d+\\..*")) {
                // 保存前一个动作
                if (currentExercise != null) {
                    exercises.add(currentExercise);
                }
                
                // 开始新动作
                currentExercise = new ScheduledExercise();
                
                // 解析动作行
                String exerciseLine = line.substring(line.indexOf('.') + 1).trim();
                String[] parts = exerciseLine.split(" - ", 2);
                
                if (parts.length >= 1) {
                    currentExercise.setName(parts[0].trim());
                }
                
                if (parts.length >= 2) {
                    String setsReps = parts[1].trim();
                    String[] setsRepsParts = setsReps.split(" x ", 2);
                    
                    if (setsRepsParts.length >= 1) {
                        try {
                            String setsStr = setsRepsParts[0].replace("组", "").trim();
                            currentExercise.setSets(Integer.parseInt(setsStr));
                        } catch (Exception e) {
                            currentExercise.setSets(3); // 默认3组
                        }
                    }
                    
                    if (setsRepsParts.length >= 2) {
                        currentExercise.setReps(setsRepsParts[1].trim());
                    }
                }
                
            } else if (currentExercise != null) {
                // 解析提示或休息时间
                if (line.startsWith("提示：")) {
                    String notes = line.substring("提示：".length()).trim();
                    currentExercise.setNotes(notes);
                } else if (line.startsWith("组间休息：")) {
                    String restTime = line.substring("组间休息：".length()).trim();
                    currentExercise.setRestTime(restTime);
                }
            }
        }
        
        // 添加最后一个动作
        if (currentExercise != null) {
            exercises.add(currentExercise);
        }
        
        return exercises;
    }
    
    // ===== Phase E P1: 用户档案与分析工具 =====
    
    /**
     * 获取用户健身档案
     * 
     * 从用户的活跃训练计划和最近身体数据中提取档案信息
     * 
     * @param userId 用户ID
     * @return 用户健身档案
     */
    private UserFitnessProfileResult getUserFitnessProfile(Long userId) {
        UserFitnessProfileResult result = new UserFitnessProfileResult();
        
        // 查询用户的活跃训练计划（从中提取目标、经验等信息）
        QueryWrapper<TrainingPlan> planWrapper = new QueryWrapper<>();
        planWrapper.eq("user_id", userId)
                   .eq("is_pinned", true)
                   .orderByDesc("updated_at")
                   .last("LIMIT 1");
        
        TrainingPlan activePlan = trainingPlanMapper.selectOne(planWrapper);
        
        if (activePlan != null) {
            result.setHasProfile(true);
            result.setGoal(activePlan.getGoal());
            result.setExperience(activePlan.getExperience());
            result.setWeeklyTrainingDays(activePlan.getTrainingDays());
            result.setEquipment(activePlan.getEquipment());
            
            // 目标肌群转换为限制列表（如果有的话）
            result.setLimitations(new ArrayList<>());
            
            // 查询最近的身体数据
            QueryWrapper<BodyRecord> bodyWrapper = new QueryWrapper<>();
            bodyWrapper.eq("user_id", userId)
                       .orderByDesc("date")
                       .last("LIMIT 1");
            
            BodyRecord latestBody = bodyRecordMapper.selectOne(bodyWrapper);
            if (latestBody != null) {
                result.setWeightKg(latestBody.getWeight());
                result.setHeightCm(null); // BodyRecord中没有身高字段
            } else {
                result.setHeightCm(null);
                result.setWeightKg(null);
            }
            
            result.setMessage(null);
        } else {
            // 没有活跃计划，档案信息缺失
            result.setHasProfile(false);
            result.setGoal(null);
            result.setExperience(null);
            result.setHeightCm(null);
            result.setWeightKg(null);
            result.setWeeklyTrainingDays(null);
            result.setEquipment(null);
            result.setLimitations(new ArrayList<>());
            result.setMessage("用户尚未设置健身档案，建议询问目标、经验等级和训练频率");
        }
        
        return result;
    }
    
    /**
     * 查询每日营养目标完成度
     * 
     * 对比实际摄入与目标，计算差值和完成百分比
     * 
     * @param userId 用户ID
     * @param date 查询日期 (yyyy-MM-dd)
     * @return 营养进度结果
     */
    private DailyNutritionProgressResult getDailyNutritionProgress(Long userId, String date) {
        DailyNutritionProgressResult result = new DailyNutritionProgressResult();
        result.setDate(date);
        
        // 查询当天的饮食摄入
        LocalDate queryDate = LocalDate.parse(date);
        long startTimestamp = queryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTimestamp = queryDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        QueryWrapper<DietRecord> dietWrapper = new QueryWrapper<>();
        dietWrapper.eq("user_id", userId)
                   .ge("date", startTimestamp)
                   .lt("date", endTimestamp);
        
        List<DietRecord> records = dietRecordMapper.selectList(dietWrapper);
        
        // 计算总摄入
        NutritionValues consumed = new NutritionValues();
        consumed.setCalories(records.stream().mapToInt(DietRecord::getCalories).sum());
        consumed.setProtein((float) records.stream().mapToDouble(DietRecord::getProtein).sum());
        consumed.setCarbs((float) records.stream().mapToDouble(DietRecord::getCarbs).sum());
        consumed.setFat((float) records.stream().mapToDouble(DietRecord::getFat).sum());
        result.setConsumed(consumed);
        
        // 查询营养目标（从活跃训练计划或单独的目标表）
        // 当前简化实现：基于用户体重估算基础目标
        // 实际应用中应该有专门的营养目标设置
        QueryWrapper<BodyRecord> bodyWrapper = new QueryWrapper<>();
        bodyWrapper.eq("user_id", userId)
                   .orderByDesc("date")
                   .last("LIMIT 1");
        
        BodyRecord latestBody = bodyRecordMapper.selectOne(bodyWrapper);
        
        if (latestBody != null && latestBody.getWeight() != null) {
            // 有身体数据，估算基础目标（非用户设置）
            result.setHasTarget(true);
            result.setTargetSource("ESTIMATED"); // 明确标记为估算值
            
            // 简化的目标估算（实际应该由用户设置或系统根据目标计算）
            float weight = latestBody.getWeight();
            NutritionValues target = new NutritionValues();
            target.setCalories((int) (weight * 30)); // 30 kcal/kg 维持体重
            target.setProtein(weight * 1.8f); // 1.8g/kg 增肌
            target.setCarbs(weight * 3.5f); // 3.5g/kg 碳水
            target.setFat(weight * 0.8f); // 0.8g/kg 脂肪
            result.setTarget(target);
            
            // 计算差值
            NutritionValues difference = new NutritionValues();
            difference.setCalories(consumed.getCalories() - target.getCalories());
            difference.setProtein(consumed.getProtein() - target.getProtein());
            difference.setCarbs(consumed.getCarbs() - target.getCarbs());
            difference.setFat(consumed.getFat() - target.getFat());
            result.setDifference(difference);
            
            // 计算完成百分比
            NutritionPercentage percentage = new NutritionPercentage();
            percentage.setCalories(target.getCalories() > 0 ? (consumed.getCalories() * 100.0f / target.getCalories()) : 0);
            percentage.setProtein(target.getProtein() > 0 ? (consumed.getProtein() * 100.0f / target.getProtein()) : 0);
            percentage.setCarbs(target.getCarbs() > 0 ? (consumed.getCarbs() * 100.0f / target.getCarbs()) : 0);
            percentage.setFat(target.getFat() > 0 ? (consumed.getFat() * 100.0f / target.getFat()) : 0);
            result.setPercentage(percentage);
            
            result.setMessage("目标为基于体重的估算值，仅供参考。建议用户设置个性化营养目标");
        } else {
            // 没有身体数据，无法设置目标
            result.setHasTarget(false);
            result.setTargetSource("NONE");
            result.setTarget(null);
            result.setDifference(null);
            result.setPercentage(null);
            result.setMessage("用户尚未设置营养目标，只能描述摄入事实，不能评价是否达标");
        }
        
        return result;
    }
    
    /**
     * 查询训练计划执行进度
     * 
     * 统计计划训练天数和实际完成天数，计算完成率
     * 
     * @param userId 用户ID
     * @param rangeDays 统计天数范围
     * @return 训练进度结果
     */
    private TrainingProgressResult getTrainingProgress(Long userId, int rangeDays) {
        TrainingProgressResult result = new TrainingProgressResult();
        result.setRangeDays(rangeDays);
        
        // 查询活跃训练计划
        QueryWrapper<TrainingPlan> planWrapper = new QueryWrapper<>();
        planWrapper.eq("user_id", userId)
                   .eq("is_pinned", true)
                   .orderByDesc("updated_at")
                   .last("LIMIT 1");
        
        TrainingPlan activePlan = trainingPlanMapper.selectOne(planWrapper);
        
        if (activePlan == null) {
            // 没有活跃计划
            result.setHasActivePlan(false);
            result.setPlannedWorkoutDays(0);
            result.setCompletedWorkoutDays(0);
            result.setCompletionRate(0);
            result.setCurrentStreakDays(0);
            result.setLastWorkoutDate(null);
            result.setMessage("用户当前没有活跃的训练计划");
            return result;
        }
        
        result.setHasActivePlan(true);
        
        // 修复：计算有效统计范围（与计划生效期对齐）
        
        // 1. 计算请求的统计范围（自然日）
        LocalDate today = LocalDate.now();
        LocalDate requestedStartDate = today.minusDays(rangeDays - 1);
        
        // 2. 计算计划生效范围
        LocalDate planStartDate = LocalDate.ofInstant(
            new Date(activePlan.getCreatedAt()).toInstant(),
            ZoneId.systemDefault()
        );
        
        // 3. 取交集作为有效统计范围（不早于计划开始日）
        LocalDate effectiveStartDate = requestedStartDate.isAfter(planStartDate) 
            ? requestedStartDate 
            : planStartDate;
        
        // 4. 计算有效范围的实际天数
        int effectiveDays = (int) java.time.temporal.ChronoUnit.DAYS.between(effectiveStartDate, today) + 1;
        
        // 5. 基于有效范围计算计划训练天数
        int weeksInRange = effectiveDays / 7;
        int remainingDays = effectiveDays % 7;
        int plannedDays = weeksInRange * activePlan.getTrainingDays();
        
        // 对于剩余的天数，取实际天数与每周训练天数的较小值
        if (remainingDays > 0) {
            plannedDays += Math.min(remainingDays, activePlan.getTrainingDays());
        }
        
        result.setPlannedWorkoutDays(plannedDays);
        
        // 6. 只统计有效范围内的训练记录（使用自然日边界）
        long effectiveStartTimestamp = effectiveStartDate.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli();
        
        QueryWrapper<Workout> workoutWrapper = new QueryWrapper<>();
        workoutWrapper.eq("user_id", userId)
                      .ge("date", effectiveStartTimestamp)
                      .orderByDesc("date");
        
        List<Workout> workouts = workoutMapper.selectList(workoutWrapper);
        
        // 修复：按日期去重计算完成天数
        Set<String> uniqueDates = workouts.stream()
            .map(w -> formatDate(w.getDate()))
            .collect(Collectors.toSet());
        int completedDays = uniqueDates.size();
        
        result.setCompletedWorkoutDays(completedDays);
        
        // 计算完成率（限制最大100%）
        if (plannedDays > 0) {
            float rate = (completedDays * 100.0f) / plannedDays;
            result.setCompletionRate(Math.min(rate, 100.0f));
        } else {
            result.setCompletionRate(0);
        }
        
        // 计算连续训练天数
        int streakDays = 0;
        if (!workouts.isEmpty()) {
            result.setLastWorkoutDate(formatDate(workouts.get(0).getDate()));
            
            // 计算从今天往前连续训练的天数（复用已定义的today变量）
            LocalDate lastWorkoutDate = LocalDate.ofInstant(
                new Date(workouts.get(0).getDate()).toInstant(),
                ZoneId.systemDefault()
            );
            
            // 如果最后一次训练是今天或昨天，才算连续
            long daysSinceLastWorkout = java.time.temporal.ChronoUnit.DAYS.between(lastWorkoutDate, today);
            
            if (daysSinceLastWorkout <= 1) {
                // 往前遍历计算连续天数
                Set<LocalDate> workoutDates = workouts.stream()
                    .map(w -> LocalDate.ofInstant(
                        new Date(w.getDate()).toInstant(),
                        ZoneId.systemDefault()
                    ))
                    .collect(Collectors.toSet());
                
                LocalDate checkDate = today;
                while (workoutDates.contains(checkDate) || workoutDates.contains(checkDate.minusDays(1))) {
                    if (workoutDates.contains(checkDate)) {
                        streakDays++;
                        checkDate = checkDate.minusDays(1);
                    } else {
                        checkDate = checkDate.minusDays(1);
                    }
                }
            }
        } else {
            result.setLastWorkoutDate(null);
        }
        
        result.setCurrentStreakDays(streakDays);
        result.setMessage(null);
        
        return result;
    }
}
