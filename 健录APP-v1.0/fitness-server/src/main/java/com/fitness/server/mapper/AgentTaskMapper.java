package com.fitness.server.mapper;

import com.fitness.server.entity.AgentTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Agent任务Mapper
 */
@Mapper
public interface AgentTaskMapper {
    
    /**
     * 插入任务
     */
    @Insert("INSERT INTO agent_tasks (user_id, session_id, user_content, assistant_content, status, failure_reason, created_at, completed_at) " +
            "VALUES (#{userId}, #{sessionId}, #{userContent}, #{assistantContent}, #{status}, #{failureReason}, #{createdAt}, #{completedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentTask task);
    
    /**
     * 根据ID查询任务
     */
    @Select("SELECT * FROM agent_tasks WHERE id = #{id}")
    AgentTask getById(Long id);

    /**
     * 在草案替换事务中串行化同一任务的状态变更。
     */
    @Select("SELECT * FROM agent_tasks WHERE id = #{id} FOR UPDATE")
    AgentTask getByIdForUpdate(@Param("id") Long id);
    
    /**
     * 根据用户ID和任务ID查询（安全检查：确保任务属于该用户）
     */
    @Select("SELECT * FROM agent_tasks WHERE id = #{taskId} AND user_id = #{userId}")
    AgentTask getByIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);
    
    /**
     * 更新任务状态
     */
    @Update("UPDATE agent_tasks SET status = #{status}, completed_at = #{completedAt} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("completedAt") Long completedAt);
    
    /**
     * 更新任务内容和状态
     */
    @Update("UPDATE agent_tasks SET assistant_content = #{assistantContent}, status = #{status}, completed_at = #{completedAt} WHERE id = #{id}")
    int updateContentAndStatus(@Param("id") Long id, 
                               @Param("assistantContent") String assistantContent, 
                               @Param("status") String status, 
                               @Param("completedAt") Long completedAt);
    
    /**
     * 仅更新助手回复内容（不改变状态）
     */
    @Update("UPDATE agent_tasks SET assistant_content = #{assistantContent} WHERE id = #{id}")
    int updateAssistantContent(@Param("id") Long id, @Param("assistantContent") String assistantContent);
    
    /**
     * 更新失败原因
     */
    @Update("UPDATE agent_tasks SET status = 'FAILED', failure_reason = #{failureReason}, completed_at = #{completedAt} WHERE id = #{id}")
    int updateFailure(@Param("id") Long id, @Param("failureReason") String failureReason, @Param("completedAt") Long completedAt);
    
    /**
     * 查询用户的最近任务（分页）
     */
    @Select("SELECT * FROM agent_tasks WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<AgentTask> getRecentTasks(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 根据会话ID查询任务
     */
    @Select("SELECT * FROM agent_tasks WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<AgentTask> getBySessionId(String sessionId);
    
    /**
     * 查询同一session的最近N条已完成任务（用于上下文）
     */
    @Select("SELECT * FROM agent_tasks " +
            "WHERE user_id = #{userId} " +
            "AND session_id = #{sessionId} " +
            "AND status IN ('SUCCEEDED', 'FAILED') " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<AgentTask> getRecentTasksBySession(@Param("userId") Long userId, 
                                           @Param("sessionId") String sessionId, 
                                           @Param("limit") int limit);
    
    /**
     * 原子状态转换（仅当当前状态匹配时才更新）
     */
    @Update("UPDATE agent_tasks SET status = #{newStatus}, completed_at = #{completedAt} " +
            "WHERE id = #{id} AND status = #{currentStatus}")
    int atomicStatusTransition(@Param("id") Long id,
                              @Param("currentStatus") String currentStatus,
                              @Param("newStatus") String newStatus,
                              @Param("completedAt") Long completedAt);
}
