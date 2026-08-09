package com.fitness.training.data.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.room.*
import com.fitness.training.data.entity.DietRecord

@Dao
interface DietRecordDao {
    @Insert
    suspend fun insert(record: DietRecord): Long
    
    @Update
    suspend fun update(record: DietRecord)
    
    @Delete
    suspend fun delete(record: DietRecord)
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay ORDER BY id ASC")
    fun getRecordsByDate(userId: Long, startOfDay: Long, endOfDay: Long): LiveData<List<DietRecord>>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay ORDER BY id ASC")
    suspend fun getRecordsByDateSync(userId: Long, startOfDay: Long, endOfDay: Long): List<DietRecord>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecords(userId: Long): LiveData<List<DietRecord>>
    
    @Query("SELECT SUM(calories) FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay")
    suspend fun getTotalCaloriesByDate(userId: Long, startOfDay: Long, endOfDay: Long): Int?
    
    @Query("SELECT SUM(protein) FROM diet_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay")
    suspend fun getTotalProteinByDate(userId: Long, startOfDay: Long, endOfDay: Long): Float?
    
    @Query("SELECT DISTINCT date FROM diet_records WHERE userId = :userId")
    suspend fun getAllRecordDates(userId: Long): List<Long>
    
    @Query("SELECT EXISTS(SELECT 1 FROM diet_records WHERE userId = :userId AND strftime('%Y-%m-%d', date/1000, 'unixepoch', 'localtime') = :date)")
    suspend fun hasRecordOnDate(userId: Long, date: String): Boolean
    
    @Query("DELETE FROM diet_records WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND strftime('%Y-%m-%d', date/1000, 'unixepoch', 'localtime') = :date")
    suspend fun getByDateSync(userId: Long, date: String): List<DietRecord>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecordsSync(userId: Long): List<DietRecord>
    
    @Query("SELECT * FROM diet_records WHERE userId = :userId ORDER BY date DESC")
    suspend fun getDietRecordsByUserId(userId: Long): List<DietRecord>
    
    // Agent 相关方法
    
    /**
     * 根据 agentActionId 查询所有饮食记录（一个actionId对应多条记录）
     */
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND agentActionId = :actionId ORDER BY id ASC")
    suspend fun getAllByActionId(userId: Long, actionId: Long): List<DietRecord>
    
    /**
     * 根据 recordKey 查询单条记录
     */
    @Query("SELECT * FROM diet_records WHERE userId = :userId AND agentActionId = :actionId AND recordKey = :recordKey LIMIT 1")
    suspend fun getByRecordKey(userId: Long, actionId: Long, recordKey: String): DietRecord?
    
    /**
     * 幂等插入饮食记录（批量，完整草案对比）
     * 
     * 改进的幂等模型：
     * 1. 检查草案中的每一条记录是否已存在（通过 recordKey）
     * 2. 已存在的记录跳过，未存在的记录插入
     * 3. 返回所有记录的 IDs（既有 + 新增）
     * 
     * 这样可以确保部分失败后恢复时不会丢失数据：
     * - 如果草案有 3 条记录，第一次只保存了 1 条
     * - 恢复时会发现还有 2 条未保存，自动补齐
     * 
     * @param userId 用户ID
     * @param actionId Agent动作ID
     * @param records 待插入的记录列表（必须包含 recordKey）
     * @return 类型化的本地引用 JSON；记录 ID 按草案输入顺序排列
     */
    @Transaction
    suspend fun insertBatchIdempotent(userId: Long, actionId: Long, records: List<DietRecord>): String {
        val allRecordIds = mutableListOf<Long>()
        
        for (record in records) {
            // 校验：Agent 创建的记录必须有 recordKey
            if (actionId > 0 && record.recordKey.isNullOrBlank()) {
                throw IllegalArgumentException("Agent 创建的饮食记录必须包含 recordKey")
            }
            
            if (actionId > 0 && !record.recordKey.isNullOrBlank()) {
                // Agent 创建，检查是否已存在该 recordKey
                val existing = getByRecordKey(userId, actionId, record.recordKey)
                if (existing != null) {
                    // 已存在，使用既有ID
                    allRecordIds.add(existing.id)
                } else {
                    // 唯一索引是并发恢复的最终防线；冲突后读取已落库的记录。
                    val id = try {
                        insert(record)
                    } catch (e: SQLiteConstraintException) {
                        getByRecordKey(userId, actionId, record.recordKey)?.id ?: throw e
                    }
                    allRecordIds.add(id)
                }
            } else {
                // 手动创建（actionId <= 0 或 recordKey 为空），直接插入
                val newId = insert(record)
                allRecordIds.add(newId)
            }
        }
        
        return com.fitness.training.network.dto.LocalWriteReference.dietRecords(allRecordIds)
    }
}
