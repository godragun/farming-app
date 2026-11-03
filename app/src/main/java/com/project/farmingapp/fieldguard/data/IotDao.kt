package com.project.farmingapp.fieldguard.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<IotReading>)

    @Query("DELETE FROM iot_readings WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("DELETE FROM iot_readings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM iot_readings")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM iot_readings
        WHERE nodeId = :nodeId AND timestampMs >= :fromMs
        ORDER BY timestampMs ASC
        """
    )
    suspend fun readingsForNodeSince(nodeId: String, fromMs: Long): List<IotReading>

    @Query(
        """
        SELECT * FROM iot_readings
        WHERE nodeId = :nodeId
        ORDER BY timestampMs DESC
        LIMIT 1
        """
    )
    suspend fun latestForNode(nodeId: String): IotReading?

    @Query(
        """
        SELECT r.* FROM iot_readings r
        INNER JOIN (
            SELECT nodeId, MAX(timestampMs) AS maxTs FROM iot_readings GROUP BY nodeId
        ) latest ON r.nodeId = latest.nodeId AND r.timestampMs = latest.maxTs
        """
    )
    fun observeLatestPerNode(): LiveData<List<IotReading>>

    @Query(
        """
        SELECT r.* FROM iot_readings r
        INNER JOIN (
            SELECT nodeId, MAX(timestampMs) AS maxTs FROM iot_readings GROUP BY nodeId
        ) latest ON r.nodeId = latest.nodeId AND r.timestampMs = latest.maxTs
        """
    )
    suspend fun latestPerNode(): List<IotReading>
}
