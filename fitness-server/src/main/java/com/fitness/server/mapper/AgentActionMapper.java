package com.fitness.server.mapper;

import com.fitness.server.entity.AgentAction;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Agent操作Mapper
 */
@Mapper
public interface AgentActionMapper {
    
    /**
     * 插入操作
     */
    @Insert("INSERT INTO agent_actions (task_id, type, payload_json, status, idempotency_key, expires_at, executed_at, result_json, failure_reason, created_at) " +
            "VALUES (#{taskId}, #{type}, #{payloadJson}, #{status}, #{idempotencyKey}, #{expiresAt}, #{executedAt}, #{resultJson}, #{failureReason}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentAction action);
    
    /**
     * 根据ID查询操作
     */
    @Select("SELECT * FROM agent_actions WHERE id = #{id}")
    AgentAction getById(Long id);
    
    /**
     * 根据任务ID查询操作列表
     */
    @Select("SELECT * FROM agent_actions WHERE task_id = #{taskId} ORDER BY created_at ASC")
    List<AgentAction> getByTaskId(Long taskId);
    
    /**
     * 根据幂等键查询（防止重复执行）
     */
    @Select("SELECT * FROM agent_actions WHERE idempotency_key = #{idempotencyKey}")
    AgentAction getByIdempotencyKey(String idempotencyKey);
    
    /**
     * 更新操作状态
     */
    @Update("UPDATE agent_actions SET status = #{status}, executed_at = #{executedAt} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("executedAt") Long executedAt);
    
    /**
     * 更新执行结果
     */
    @Update("UPDATE agent_actions SET status = #{status}, executed_at = #{executedAt}, result_json = #{resultJson} WHERE id = #{id}")
    int updateResult(@Param("id") Long id, 
                     @Param("status") String status, 
                     @Param("executedAt") Long executedAt, 
                     @Param("resultJson") String resultJson);
    
    /**
     * 更新失败原因
     */
    @Update("UPDATE agent_actions SET status = 'FAILED', failure_reason = #{failureReason} WHERE id = #{id}")
    int updateFailure(@Param("id") Long id, @Param("failureReason") String failureReason);
    
    /**
     * 查询待确认的操作（未过期）- 用于向客户端展示
     */
    @Select("SELECT * FROM agent_actions WHERE task_id = #{taskId} AND status = 'WAITING_CONFIRMATION' AND expires_at > #{currentTime}")
    List<AgentAction> getPendingActions(@Param("taskId") Long taskId, @Param("currentTime") Long currentTime);
    
    /**
     * 查询用户所有 LOCAL_WRITE_PENDING 状态的操作（用于恢复）
     */
    @Select("SELECT a.* FROM agent_actions a " +
            "INNER JOIN agent_tasks t ON a.task_id = t.id " +
            "WHERE t.user_id = #{userId} AND a.status = 'LOCAL_WRITE_PENDING' " +
            "ORDER BY a.created_at ASC")
    List<AgentAction> getPendingLocalWrites(@Param("userId") Long userId);
    
    /**
     * 查询待确认的操作（不检查过期）- 用于替换旧草案
     */
    @Select("SELECT * FROM agent_actions WHERE task_id = #{taskId} AND status = 'WAITING_CONFIRMATION'")
    List<AgentAction> getWaitingActions(@Param("taskId") Long taskId);
    
    /**
     * 将同一任务中同类型的待确认草案全部替换为新版本。
     */
    @Update("UPDATE agent_actions SET status = 'REPLACED', executed_at = #{currentTime} " +
            "WHERE task_id = #{taskId} AND type = #{type} AND status = 'WAITING_CONFIRMATION'")
    int replaceWaitingActionsByType(
        @Param("taskId") Long taskId,
        @Param("type") String type,
        @Param("currentTime") Long currentTime
    );

    /**
     * 原子化状态转换 - 防止并发重复执行
     * 
     * @return 更新行数，0表示状态已改变或记录不存在
     */
    @Update("UPDATE agent_actions SET status = #{newStatus}, executed_at = #{currentTime} " +
            "WHERE id = #{id} AND status = #{oldStatus} AND expires_at > #{currentTime}")
    int atomicStatusTransition(
        @Param("id") Long id,
        @Param("oldStatus") String oldStatus,
        @Param("newStatus") String newStatus,
        @Param("currentTime") Long currentTime
    );
    
    /**
     * 原子状态转换（不检查过期时间）- 用于取消等操作
     */
    @Update("UPDATE agent_actions SET status = #{newStatus}, executed_at = #{currentTime} " +
            "WHERE id = #{id} AND status = #{oldStatus}")
    int atomicStatusTransitionNoExpiry(
        @Param("id") Long id,
        @Param("oldStatus") String oldStatus,
        @Param("newStatus") String newStatus,
        @Param("currentTime") Long currentTime
    );
    
    /**
     * 原子完成本地写入（状态转换 + 更新引用）
     * 一次性完成状态和 local_reference 的更新，保证原子性
     */
    @Update("UPDATE agent_actions " +
            "SET status = 'SUCCEEDED', local_reference = #{localReference}, executed_at = #{currentTime} " +
            "WHERE id = #{id} AND status = 'LOCAL_WRITE_PENDING'")
    int atomicCompleteLocalWrite(
        @Param("id") Long id,
        @Param("localReference") String localReference,
        @Param("currentTime") Long currentTime
    );
    
    /**
     * 更新本地写入完成信息
     */
    @Update("UPDATE agent_actions SET local_reference = #{localReference} WHERE id = #{id}")
    int updateLocalReference(@Param("id") Long id, @Param("localReference") String localReference);
    
    /**
     * 更新操作的 payload JSON
     */
    @Update("UPDATE agent_actions SET payload_json = #{payloadJson} WHERE id = #{id}")
    int updatePayloadJson(@Param("id") Long id, @Param("payloadJson") String payloadJson);
}
