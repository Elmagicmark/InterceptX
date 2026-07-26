package com.interceptx.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.interceptx.data.model.HttpTransaction
import com.interceptx.data.model.Project
import com.interceptx.data.model.ProxySettings
import com.interceptx.data.model.RepeaterTab
import com.interceptx.data.model.ScopeRule

@Database(
    entities = [
        HttpTransaction::class,
        Project::class,
        ScopeRule::class,
        RepeaterTab::class,
        ProxySettings::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun projectDao(): ProjectDao
    abstract fun scopeRuleDao(): ScopeRuleDao
    abstract fun repeaterTabDao(): RepeaterTabDao
    abstract fun proxySettingsDao(): ProxySettingsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "interceptx.db"
                )
                    // Requirement: Room DB migration fallback enabled.
                    // Any future schema bump without a matching Migration object
                    // destructively recreates tables instead of crashing at runtime.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
