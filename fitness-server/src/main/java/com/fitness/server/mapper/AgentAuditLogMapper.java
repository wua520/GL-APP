package com.fitness.server.mapper;

import com.fitness.server.entity.AgentAuditLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Agent审计日志Mapper
 */
@Mapper
public interface AgentAuditLogMapper {
    
    /**
     * 插入审计日志
     */
    @Insert("INSERT INTO agent_audit_logs (task_id, step_type, tool_name, input_summary, output_summary, created_at) " +
            "VALUES (#{taskId}, #{stepType}, #{toolName}, #{inputSummary}, #{outputSummary}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentAuditLog log);
    
    /**
     * 根据任务ID查询审计日志
     */
    @Select("SELECT * FROM agent_audit_logs WHERE task_id = #{taskId} ORDER BY created_at ASC")
    List<AgentAuditLog> getByTaskId(Long taskId);
    
    /**
     * 查询最近的审计日志（用于监控）
     */
    @Select("SELECT * FROM agent_audit_logs ORDER BY created_at DESC LIMIT #{limit}")
    List<AgentAuditLog> getRecentLogs(int limit);
}
