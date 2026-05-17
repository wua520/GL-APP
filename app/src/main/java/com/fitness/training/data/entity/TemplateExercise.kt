package com.fitness.training.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["exerciseId"])
    ]
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0, // 动作顺序
    val targetSets: Int = 0, // 目标组数
    val targetReps: Int = 0 // 目标次数
)



