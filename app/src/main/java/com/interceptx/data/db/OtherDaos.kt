package com.interceptx.data.db

import androidx.room.*
import com.interceptx.data.model.Project
import com.interceptx.data.model.ProxySettings
import com.interceptx.data.model.RepeaterTab
import com.interceptx.data.model.ScopeRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Query("UPDATE projects SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE projects SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)
}

@Dao
interface ScopeRuleDao {
    @Insert
    suspend fun insert(rule: ScopeRule): Long

    @Delete
    suspend fun delete(rule: ScopeRule)

    @Query("SELECT * FROM scope_rules WHERE projectId = :projectId")
    fun observeForProject(projectId: Long): Flow<List<ScopeRule>>
}

@Dao
interface RepeaterTabDao {
    @Insert
    suspend fun insert(tab: RepeaterTab): Long

    @Update
    suspend fun update(tab: RepeaterTab)

    @Delete
    suspend fun delete(tab: RepeaterTab)

    @Query("SELECT * FROM repeater_tabs WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun observeForProject(projectId: Long): Flow<List<RepeaterTab>>
}

@Dao
interface ProxySettingsDao {
    @Query("SELECT * FROM proxy_settings WHERE id = 1")
    fun observe(): Flow<ProxySettings?>

    @Query("SELECT * FROM proxy_settings WHERE id = 1")
    suspend fun get(): ProxySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ProxySettings)
}
