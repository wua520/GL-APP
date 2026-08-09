-- 健录 App 数据库初始化脚本
-- 在 MySQL 中执行此脚本创建数据库和表

CREATE DATABASE IF NOT EXISTS fitness_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fitness_db;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 训练记录表
CREATE TABLE IF NOT EXISTS workouts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    local_id BIGINT NOT NULL,
    name VARCHAR(100),
    date BIGINT NOT NULL,
    duration BIGINT DEFAULT 0,
    notes TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_local (user_id, local_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 训练动作表
CREATE TABLE IF NOT EXISTS workout_exercises (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workout_id BIGINT NOT NULL,
    local_id BIGINT NOT NULL,
    exercise_name VARCHAR(100) NOT NULL,
    exercise_order INT NOT NULL,
    superset_group_id BIGINT,
    FOREIGN KEY (workout_id) REFERENCES workouts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 训练组数表
CREATE TABLE IF NOT EXISTS workout_sets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workout_exercise_id BIGINT NOT NULL,
    local_id BIGINT NOT NULL,
    set_number INT NOT NULL,
    weight DOUBLE NOT NULL,
    reps INT NOT NULL,
    is_completed BOOLEAN DEFAULT TRUE,
    rest_time INT DEFAULT 0,
    FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 饮食记录表
CREATE TABLE IF NOT EXISTS diet_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    local_id BIGINT NOT NULL,
    date BIGINT NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    calories INT DEFAULT 0,
    protein FLOAT DEFAULT 0,
    carbs FLOAT DEFAULT 0,
    fat FLOAT DEFAULT 0,
    amount VARCHAR(50),
    agent_action_id BIGINT NULL,
    record_key VARCHAR(200) NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_local (user_id, local_id),
    UNIQUE KEY uk_user_agent_record (user_id, agent_action_id, record_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 身体记录表
CREATE TABLE IF NOT EXISTS body_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    local_id BIGINT NOT NULL,
    date BIGINT NOT NULL,
    weight FLOAT,
    body_fat FLOAT,
    muscle_mass FLOAT,
    note TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_local (user_id, local_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 训练计划表
CREATE TABLE IF NOT EXISTS training_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    local_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    details TEXT,
    goal VARCHAR(50),
    experience VARCHAR(50),
    target_muscles VARCHAR(200),
    training_days INT DEFAULT 3,
    training_duration VARCHAR(50),
    equipment VARCHAR(50),
    is_pinned BOOLEAN DEFAULT FALSE,
    is_from_recommendation BOOLEAN DEFAULT TRUE,
    agent_action_id BIGINT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_local (user_id, local_id),
    UNIQUE KEY uk_user_agent_action (user_id, agent_action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
