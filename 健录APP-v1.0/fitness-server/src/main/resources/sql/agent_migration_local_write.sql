-- ============================================================================
-- Agent 系统迁移：支持两阶段提交（LOCAL_WRITE_PENDING状态）
-- 执行时间：2025-01-XX
-- ============================================================================

-- 说明：
-- 1. 增加 LOCAL_WRITE_PENDING 状态支持
-- 2. agent_actions 表增加 local_reference 字段，用于记录客户端本地ID
-- 3. 保持向后兼容，不删除现有数据

-- 修改 agent_actions 表：新增或扩容本地引用字段。
-- 此脚本可重复执行：先移除不再使用的旧索引，再按实际列存在性新增或扩容。
-- local_reference 仅作为客户端幂等回执，不参与服务端查询，因此不建立索引。
SET @has_local_reference := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_actions'
      AND column_name = 'local_reference'
);
SET @has_local_reference_index := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_actions'
      AND index_name = 'idx_local_reference'
);
SET @sql := IF(
    @has_local_reference_index > 0,
    'ALTER TABLE agent_actions DROP INDEX idx_local_reference',
    'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql := IF(
    @has_local_reference = 0,
    'ALTER TABLE agent_actions ADD COLUMN local_reference TEXT COMMENT ''客户端本地引用 JSON（类型与有序本地记录ID）'' AFTER result_json',
    'ALTER TABLE agent_actions MODIFY COLUMN local_reference TEXT COMMENT ''客户端本地引用 JSON（类型与有序本地记录ID）'''
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- 状态说明（不需要修改表结构，只是文档说明）：
-- WAITING_CONFIRMATION: 等待用户确认
-- LOCAL_WRITE_PENDING: 用户已确认，等待客户端完成本地写入
-- SUCCEEDED: 客户端已完成本地写入并上报
-- FAILED: 执行失败
-- CANCELLED: 已取消
-- EXPIRED: 已过期
-- REPLACED: 被新草案替换

-- ============================================================================
-- 回滚脚本（如果需要）
-- ============================================================================
-- ALTER TABLE agent_actions DROP COLUMN local_reference;
-- 若迁移前已有旧索引，可按需手动恢复：ALTER TABLE agent_actions ADD INDEX idx_local_reference (local_reference(255));
