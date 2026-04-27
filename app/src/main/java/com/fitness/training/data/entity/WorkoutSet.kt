package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int, // 组号
    val weight: Double, // 重量
    val reps: Int, // 次数
    val isCompleted: Boolean = true, // 是否完成
    val restTime: Int = 0 // 休息时间（秒）
)



