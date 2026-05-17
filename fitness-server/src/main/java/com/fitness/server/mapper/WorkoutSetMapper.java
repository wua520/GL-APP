package com.fitness.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.server.entity.WorkoutSet;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkoutSetMapper extends BaseMapper<WorkoutSet> {
}
