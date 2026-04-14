package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    /** Human-readable attribution, e.g. "Atomic Habits — James Clear" */
    val source: String = "",
    /** Audible search URL for this book; blank for user-added quotes */
    val sourceUrl: String = ""
)
