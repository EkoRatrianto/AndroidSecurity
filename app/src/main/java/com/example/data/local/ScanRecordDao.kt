package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records ORDER BY timestampMillis DESC")
    fun observeAllRecords(): Flow<List<ScanRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScanRecordEntity)

    @Query("DELETE FROM scan_records WHERE scanId = :scanId")
    suspend fun deleteRecordById(scanId: String)

    @Query("DELETE FROM scan_records")
    suspend fun clearAllRecords()
}
