package com.fitness.training.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fitness.training.data.dao.*
import com.fitness.training.data.entity.*

@Database(
    entities = [
        Exercise::class,
        Workout::class,
        WorkoutExercise::class,
        WorkoutSet::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
        TrainingPlan::class,
        DietRecord::class,
        Food::class,
        User::class,
        BodyRecord::class,
        Achievement::class,
        AiChatMessage::class
    ],
    version = 24,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun templateExerciseDao(): TemplateExerciseDao
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun dietRecordDao(): DietRecordDao
    abstract fun foodDao(): FoodDao
    abstract fun userDao(): UserDao
    abstract fun bodyRecordDao(): BodyRecordDao
    abstract fun achievementDao(): AchievementDao
    abstract fun aiChatMessageDao(): AiChatMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: FitnessDatabase? = null
        
        /**
         * Room 数据库迁移：版本 16 → 17
         * 添加 TrainingPlan.agentActionId 字段
         * 
         * 注意：不在这里创建索引，因为 Room 无法验证带 WHERE 条件的索引
         * 索引会在应用逻辑中按需创建
         */
        private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 添加 agentActionId 字段（允许为空，因为旧数据没有这个字段）
                database.execSQL(
                    "ALTER TABLE training_plans ADD COLUMN agentActionId INTEGER DEFAULT NULL"
                )
            }
        }
        
        /**
         * Room 数据库迁移：版本 17 → 18
         * 添加 DietRecord.agentActionId 字段
         * 
         * 注意：不在这里创建索引，因为 Room 无法验证带 WHERE 条件的索引
         * 索引会在应用逻辑中按需创建
         */
        private val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 添加 agentActionId 字段（允许为空，因为旧数据没有这个字段）
                database.execSQL(
                    "ALTER TABLE diet_records ADD COLUMN agentActionId INTEGER DEFAULT NULL"
                )
            }
        }
        
        /**
         * Room 数据库迁移：版本 22 → 23
         * 添加 DietRecord.recordKey 字段，实现完整草案对比
         * 
         * 背景：批量写入没有事务边界，导致数据丢失风险
         * - 草案有 3 条记录，第一条成功、第二条失败
         * - 恢复时发现存在第一条，直接标记为成功
         * - 结果：第二、三条记录永久丢失
         * 
         * 修复方案：
         * 1. 添加 recordKey 字段（格式："mealType_foodName_index"）
         * 2. 建立 (userId, agentActionId, recordKey) 唯一约束
         * 3. 恢复时逐条对比草案，补齐缺失的记录
         * 
         * 迁移策略：
         * - 旧数据（agentActionId为null）：recordKey设为null，不影响既有数据
         * - 新数据（Agent创建）：必须包含recordKey，实现完整幂等性
         */
        private val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. 添加 recordKey 字段（允许为空，兼容旧数据）
                database.execSQL(
                    "ALTER TABLE diet_records ADD COLUMN recordKey TEXT DEFAULT NULL"
                )
                
                // 2. 删除旧的普通索引（如果存在）
                database.execSQL("DROP INDEX IF EXISTS index_diet_records_userId_agentActionId")
                database.execSQL("DROP INDEX IF EXISTS idx_user_action_diet")
                
                // 3. 创建新的复合唯一索引 (userId, agentActionId, recordKey)
                // 注意：SQLite 的唯一索引会忽略 NULL 值，因此不会影响旧数据
                database.execSQL("""
                    CREATE UNIQUE INDEX idx_user_action_recordkey_diet 
                    ON diet_records(userId, agentActionId, recordKey)
                """.trimIndent())
                
                // 4. 保留普通索引 (userId, agentActionId) 用于快速查询同一action下的所有记录
                database.execSQL("""
                    CREATE INDEX idx_user_action_diet 
                    ON diet_records(userId, agentActionId)
                """.trimIndent())
            }
        }
        
        private val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_training_plans_userId_agentActionId
                    ON training_plans(userId, agentActionId)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): FitnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness_database"
                )
                    .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_22_23, MIGRATION_23_24)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


