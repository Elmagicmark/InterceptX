package com.interceptx.data.db

import androidx.room.TypeConverter
import com.interceptx.data.model.ScopeType
import com.interceptx.data.model.TransactionState

class Converters {
    @TypeConverter
    fun fromTransactionState(state: TransactionState): String = state.name

    @TypeConverter
    fun toTransactionState(value: String): TransactionState = TransactionState.valueOf(value)

    @TypeConverter
    fun fromScopeType(type: ScopeType): String = type.name

    @TypeConverter
    fun toScopeType(value: String): ScopeType = ScopeType.valueOf(value)
}
