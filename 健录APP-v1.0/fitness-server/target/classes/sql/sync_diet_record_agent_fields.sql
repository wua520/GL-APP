-- 为 diet_records 表添加 Agent 相关字段以支持同步保留元数据
-- 这些字段用于防止 AI 创建的记录在跨设备同步时重复下载

ALTER TABLE diet_records 
ADD COLUMN agent_action_id BIGINT NULL COMMENT 'Agent创建的记录关联的actionId，用于区分AI创建和用户手动创建',
ADD COLUMN record_key VARCHAR(200) NULL COMMENT '记录级别的幂等键，格式: mealType_foodName_index，用于批量写入的幂等性';

-- 同一用户、同一 Agent 草案中的同一记录只能同步落库一次。
CREATE UNIQUE INDEX uk_diet_records_user_agent_record
ON diet_records(user_id, agent_action_id, record_key);
