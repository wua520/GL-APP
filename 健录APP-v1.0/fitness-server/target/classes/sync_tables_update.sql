-- ============================================
-- 健录 App - 云端同步功能扩展
-- 新增表：身体档案、自定义动作、训练模板、收藏动作
-- 执行时间：2026-05-13
-- ============================================

USE fitness_db;

-- ============================================
-- 1. 身体档案表 (Body Profiles)
-- ============================================
CREATE TABLE IF NOT EXISTS body_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID，每个用户只有一条记录',
    gender INT DEFAULT 0 COMMENT '性别：0=未设置, 1=男, 2=女',
    height INT DEFAULT 0 COMMENT '身高（cm）',
    birth_year INT DEFAULT 0 COMMENT '出生年份',
    updated_at BIGINT NOT NULL COMMENT '最后更新时间（毫秒时间戳）',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户身体档案表';

-- ============================================
-- 2. 自定义动作表 (Custom Exercises)
-- ============================================
CREATE TABLE IF NOT EXISTS custom_exercises (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    local_id BIGINT NOT NULL COMMENT '本地动作ID',
    name VARCHAR(100) NOT NULL COMMENT '动作名称',
    muscle_group VARCHAR(50) NOT NULL COMMENT '主要肌群',
    sub_muscle_group VARCHAR(50) DEFAULT '' COMMENT '次要肌群',
    equipment VARCHAR(50) NOT NULL COMMENT '器械类型',
    description TEXT COMMENT '动作描述',
    image_url VARCHAR(500) DEFAULT '' COMMENT '动作图片URL',
    is_favorite BOOLEAN DEFAULT FALSE COMMENT '是否收藏',
    created_at BIGINT NOT NULL COMMENT '创建时间',
    updated_at BIGINT NOT NULL COMMENT '最后更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_local (user_id, local_id),
    INDEX idx_user_id (user_id),
    INDEX idx_muscle_group (muscle_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义动作表';

-- ============================================
-- 3. 训练模板表 (Workout Templates)
-- ============================================
CREATE TABLE IF NOT EXISTS workout_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    local_id BIGINT NOT NULL COMMENT '本地模板ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    created_at BIGINT NOT NULL COMMENT '创建时间',
    updated_at BIGINT NOT NULL COMMENT '最后更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_local (user_id, local_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户训练模板表';

-- ============================================
-- 4. 模板动作表 (Template Exercises)
-- ============================================
CREATE TABLE IF NOT EXISTS template_exercises (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL COMMENT '模板ID（云端ID）',
    local_id BIGINT NOT NULL COMMENT '本地记录ID',
    exercise_name VARCHAR(100) NOT NULL COMMENT '动作名称',
    sort_order INT NOT NULL COMMENT '动作顺序',
    target_sets INT DEFAULT 0 COMMENT '目标组数',
    target_reps INT DEFAULT 0 COMMENT '目标次数',
    FOREIGN KEY (template_id) REFERENCES workout_templates(id) ON DELETE CASCADE,
    INDEX idx_template_id (template_id),
    INDEX idx_sort_order (template_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练模板动作表';

-- ============================================
-- 5. 收藏动作表 (Favorite Exercises)
-- ============================================
CREATE TABLE IF NOT EXISTS favorite_exercises (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    exercise_name VARCHAR(100) NOT NULL COMMENT '动作名称（系统预设动作）',
    created_at BIGINT NOT NULL COMMENT '收藏时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_exercise (user_id, exercise_name),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏的系统动作表';

-- ============================================
-- 验证表是否创建成功
-- ============================================
SHOW TABLES LIKE '%profile%';
SHOW TABLES LIKE '%custom_exercises%';
SHOW TABLES LIKE '%workout_templates%';
SHOW TABLES LIKE '%template_exercises%';
SHOW TABLES LIKE '%favorite_exercises%';

-- ============================================
-- 查看表结构
-- ============================================
-- DESC body_profiles;
-- DESC custom_exercises;
-- DESC workout_templates;
-- DESC template_exercises;
-- DESC favorite_exercises;
