-- ============================================================================
-- Agent 系统数据表
-- ============================================================================

-- Agent 任务表
CREATE TABLE IF NOT EXISTS agent_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(100) COMMENT '会话ID（可选）',
    user_content TEXT NOT NULL COMMENT '用户输入内容',
    assistant_content TEXT COMMENT 'Agent返回内容',
    status VARCHAR(50) NOT NULL COMMENT '任务状态',
    failure_reason TEXT COMMENT '失败原因',
    created_at BIGINT NOT NULL COMMENT '创建时间（时间戳）',
    completed_at BIGINT COMMENT '完成时间（时间戳）',
    
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent任务表';

-- Agent 结构化记忆表。用户显式创建或编辑，不能由模型自动写入。
-- session_id 为空时为用户级记忆；否则仅在对应会话中生效。
CREATE TABLE IF NOT EXISTS agent_memories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记忆ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(100) NULL COMMENT '会话ID；为空表示用户级记忆',
    memory_type VARCHAR(20) NOT NULL COMMENT 'PROFILE/SAFETY/PLAN/SUMMARY',
    content TEXT NOT NULL COMMENT '用户管理的短文本记忆',
    status VARCHAR(20) NOT NULL COMMENT 'ACTIVE/DELETED',
    source_task_id BIGINT NULL COMMENT '预留：来源任务；当前不自动写入',
    expires_at BIGINT NULL COMMENT '过期时间戳',
    created_at BIGINT NOT NULL COMMENT '创建时间戳',
    updated_at BIGINT NOT NULL COMMENT '更新时间戳',
    INDEX idx_agent_memories_context (user_id, session_id, status, expires_at),
    INDEX idx_agent_memories_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent用户可管理的结构化记忆';

-- Agent 操作表（待确认的写入操作）
CREATE TABLE IF NOT EXISTS agent_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作ID',
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    type VARCHAR(50) NOT NULL COMMENT '操作类型',
    payload_json TEXT NOT NULL COMMENT '操作载荷（JSON格式）',
    status VARCHAR(50) NOT NULL COMMENT '操作状态',
    idempotency_key VARCHAR(100) UNIQUE COMMENT '幂等键',
    expires_at BIGINT NOT NULL COMMENT '过期时间（时间戳）',
    executed_at BIGINT COMMENT '执行时间（时间戳）',
    result_json TEXT COMMENT '执行结果（JSON格式）',
    local_reference TEXT COMMENT '客户端本地引用 JSON（类型与有序本地记录ID）',
    failure_reason TEXT COMMENT '失败原因',
    created_at BIGINT NOT NULL COMMENT '创建时间（时间戳）',
    
    INDEX idx_task_id (task_id),
    INDEX idx_task_type_status (task_id, type, status),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    UNIQUE INDEX idx_idempotency_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent操作表';

-- Agent 审计日志表
CREATE TABLE IF NOT EXISTS agent_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    step_type VARCHAR(50) NOT NULL COMMENT '步骤类型',
    tool_name VARCHAR(100) COMMENT '工具名称',
    input_summary TEXT COMMENT '输入摘要（脱敏）',
    output_summary TEXT COMMENT '输出摘要（脱敏）',
    created_at BIGINT NOT NULL COMMENT '创建时间（时间戳）',
    
    INDEX idx_task_id (task_id),
    INDEX idx_step_type (step_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent审计日志表';

-- ============================================================================
-- 说明
-- ============================================================================
-- 1. 所有时间字段使用 BIGINT 存储时间戳（毫秒），便于跨时区处理
-- 2. user_id 来自 JWT，不由客户端传入
-- 3. idempotency_key 用于防止重复执行同一操作
-- 4. payload_json 和 result_json 存储结构化数据，不存储模型的自由文本
-- 5. input_summary 和 output_summary 存储脱敏后的摘要，保护隐私
-- 6. 索引优化：user_id, session_id, status, created_at 用于常见查询
