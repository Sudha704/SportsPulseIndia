package com.sportspulse.india.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sportspulse.india.core.data.db.entity.VenueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `venues` table.
 * Entries have a 2-hour TTL enforced by [deleteStaleVenues].
 */
@Dao
interface VenueDao {

    /** Emits all cached venues ordered by travel time ascending. */
    @Query("SELECT * FROM venues ORDER BY travelTimeMinutes ASC")
    fun observeAllVenues(): Flow<List<VenueEntity>>

    /** One-shot fetch of all venues within a bounding box (approx radius filter). */
    @Query(
        """
        SELECT * FROM venues
        WHERE lat BETWEEN :minLat AND :maxLat
          AND lng BETWEEN :minLng AND :maxLng
        ORDER BY travelTimeMinutes ASC
        """
    )
    suspend fun getVenuesInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<VenueEntity>

    /** Fetches a single venue by place ID. */
    @Query("SELECT * FROM venues WHERE placeId = :placeId LIMIT 1")
    suspend fun getVenueByPlaceId(placeId: String): VenueEntity?

    /** Upserts venues (replace on conflict). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(venues: List<VenueEntity>)

    /** Updates only the travel time for a specific venue (after Distance Matrix refresh). */
    @Query("UPDATE venues SET travelTimeMinutes = :minutes WHERE placeId = :placeId")
    suspend fun updateTravelTime(placeId: String, minutes: Int)

    /** Deletes venue entries older than 2 hours. */
    @Query("DELETE FROM venues WHERE cachedAt < :expiryMs")
    suspend fun deleteStaleVenues(expiryMs: Long)

    /** Clears all venue entries. */
    @Query("DELETE FROM venues")
    suspend fun clearAll()
}
