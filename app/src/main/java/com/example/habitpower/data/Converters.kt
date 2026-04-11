package com.example.habitpower.data

import androidx.room.TypeConverter
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.TargetOperator
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun fromHabitType(value: String?): HabitType? {
        return value?.let { HabitType.valueOf(it) }
    }

    @TypeConverter
    fun habitTypeToString(type: HabitType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromTargetOperator(value: String?): TargetOperator? {
        return value?.let {
            runCatching { TargetOperator.valueOf(it) }.getOrDefault(TargetOperator.GREATER_THAN_OR_EQUAL)
        }
    }

    @TypeConverter
    fun targetOperatorToString(op: TargetOperator?): String? {
        return op?.name
    }
}
