package com.example.habitpower.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chant_definitions")
data class ChantDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val text: String,
    val tradition: String? = null,
    @ColumnInfo(defaultValue = "108") val defaultCount: Int = 108,
    @ColumnInfo(defaultValue = "0") val isBuiltIn: Boolean = false,
    @ColumnInfo(name = "audio_uri") val audioUri: String? = null
)
