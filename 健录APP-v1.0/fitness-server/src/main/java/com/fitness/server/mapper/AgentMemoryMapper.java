package com.fitness.server.mapper;

import com.fitness.server.entity.AgentMemory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AgentMemoryMapper {
    @Insert("INSERT INTO agent_memories (user_id, session_id, memory_type, content, status, source_task_id, expires_at, created_at, updated_at) " +
            "VALUES (#{userId}, #{sessionId}, #{memoryType}, #{content}, #{status}, #{sourceTaskId}, #{expiresAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentMemory memory);

    @Select("SELECT * FROM agent_memories WHERE id = #{id} AND user_id = #{userId}")
    AgentMemory getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM agent_memories WHERE user_id = #{userId} " +
            "AND (session_id = #{sessionId} OR session_id IS NULL) " +
            "AND status = 'ACTIVE' AND (expires_at IS NULL OR expires_at > #{now}) " +
            "ORDER BY CASE memory_type WHEN 'SAFETY' THEN 0 WHEN 'PROFILE' THEN 1 WHEN 'PLAN' THEN 2 WHEN 'SUMMARY' THEN 3 ELSE 4 END, updated_at DESC LIMIT #{limit}")
    List<AgentMemory> getActiveForContext(@Param("userId") Long userId, @Param("sessionId") String sessionId,
                                          @Param("now") Long now, @Param("limit") int limit);

    @Select("SELECT * FROM agent_memories WHERE user_id = #{userId} " +
            "AND (#{sessionId} IS NULL OR session_id = #{sessionId} OR session_id IS NULL) " +
            "ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<AgentMemory> listByUser(@Param("userId") Long userId, @Param("sessionId") String sessionId,
                                 @Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE agent_memories SET content = #{content}, memory_type = #{memoryType}, expires_at = #{expiresAt}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 'ACTIVE'")
    int updateManagedMemory(@Param("id") Long id, @Param("userId") Long userId, @Param("content") String content,
                            @Param("memoryType") String memoryType, @Param("expiresAt") Long expiresAt,
                            @Param("updatedAt") Long updatedAt);

    @Update("UPDATE agent_memories SET status = 'DELETED', updated_at = #{updatedAt} WHERE id = #{id} AND user_id = #{userId} AND status = 'ACTIVE'")
    int softDelete(@Param("id") Long id, @Param("userId") Long userId, @Param("updatedAt") Long updatedAt);
}
