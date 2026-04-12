package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_life_area_assignments",
    primaryKeys = ["userId", "lifeAreaId"],
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LifeArea::class,
            parentColumns = ["id"],
            childColumns = ["lifeAreaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lifeAreaId")]
)
data class UserLifeAreaAssignment(
    val userId: Long,
    val lifeAreaId: Long,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)
