package com.interceptx.data.db

import androidx.room.*
import com.interceptx.data.model.HttpTransaction
import com.interceptx.data.model.TransactionState
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: HttpTransaction): Long

    @Update
    suspend fun update(transaction: HttpTransaction)

    @Delete
    suspend fun delete(transaction: HttpTransaction)

    @Query("DELETE FROM http_transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM http_transactions WHERE projectId = :projectId")
    suspend fun clearProject(projectId: Long)

    @Query("SELECT * FROM http_transactions WHERE id = :id")
    suspend fun getById(id: Long): HttpTransaction?

    @Query("SELECT * FROM http_transactions WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(projectId: Long, limit: Int = 20): Flow<List<HttpTransaction>>

    @Query("SELECT * FROM http_transactions WHERE state = 'INTERCEPTED' AND projectId = :projectId ORDER BY timestamp ASC")
    fun observeIntercepted(projectId: Long): Flow<List<HttpTransaction>>

    // Full history with search + method/status filters and dynamic sort.
    // Built with a single flexible query using COALESCE-style optional filters.
    @Query(
        """
        SELECT * FROM http_transactions
        WHERE projectId = :projectId
        AND (:searchText = '' OR url LIKE '%' || :searchText || '%')
        AND (:methodFilter = '' OR method = :methodFilter)
        AND (
            :statusClass = '' OR
            (:statusClass = '2xx' AND responseStatusCode BETWEEN 200 AND 299) OR
            (:statusClass = '3xx' AND responseStatusCode BETWEEN 300 AND 399) OR
            (:statusClass = '4xx' AND responseStatusCode BETWEEN 400 AND 499) OR
            (:statusClass = '5xx' AND responseStatusCode BETWEEN 500 AND 599)
        )
        ORDER BY
            CASE WHEN :sortMode = 'NEWEST' THEN timestamp END DESC,
            CASE WHEN :sortMode = 'OLDEST' THEN timestamp END ASC,
            CASE WHEN :sortMode = 'SLOWEST' THEN responseTimeMs END DESC,
            CASE WHEN :sortMode = 'STATUS' THEN responseStatusCode END ASC
        """
    )
    fun observeHistory(
        projectId: Long,
        searchText: String,
        methodFilter: String,
        statusClass: String,
        sortMode: String
    ): Flow<List<HttpTransaction>>

    @Query("SELECT COUNT(*) FROM http_transactions WHERE projectId = :projectId")
    fun observeTotalCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM http_transactions WHERE projectId = :projectId AND state = 'INTERCEPTED'")
    fun observeInterceptedCount(projectId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM http_transactions WHERE projectId = :projectId")
    fun observeTotalBandwidth(projectId: Long): Flow<Long>

    @Query("SELECT * FROM http_transactions WHERE projectId = :projectId")
    suspend fun getAllForExport(projectId: Long): List<HttpTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<HttpTransaction>)
}
