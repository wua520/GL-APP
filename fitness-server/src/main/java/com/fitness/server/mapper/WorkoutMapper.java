package com.fitness.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.server.entity.Workout;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkoutMapper extends BaseMapper<Workout> {
}
