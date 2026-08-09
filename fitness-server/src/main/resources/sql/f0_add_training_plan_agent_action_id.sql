-- Phase F0 Task 2: 为TrainingPlan添加agentActionId字段
-- 使TrainingPlan的Agent幂等逻辑与DietRecord保持一致

ALTER TABLE training_plans 
ADD COLUMN agent_action_id BIGINT NULL COMMENT 'Agent创建的计划关联的actionId，用于幂等去重';

CREATE UNIQUE INDEX uk_training_plans_user_agent_action
ON training_plans(user_id, agent_action_id);

-- 说明：
-- 1. agent_action_id为NULL表示非Agent创建的计划（用户手动创建或导入）
-- 2. 同一个agent_action_id在同一用户下应该只对应一个训练计划
-- 3. 幂等检查逻辑：WHERE user_id = ? AND agent_action_id = ?
-- 4. 与diet_records表的agent_action_id字段保持一致的语义
