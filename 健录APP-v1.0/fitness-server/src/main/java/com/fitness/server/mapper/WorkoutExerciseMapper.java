package com.fitness.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.server.entity.WorkoutExercise;
import com.fitness.server.entity.WorkoutSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkoutExerciseMapper extends BaseMapper<WorkoutExercise> {
    
    /**
     * 查询训练动作的所有组
     */
    @Select("SELECT * FROM workout_sets WHERE exercise_id = #{exerciseId}")
    List<WorkoutSet> selectSetsByExerciseId(Long exerciseId);
}
