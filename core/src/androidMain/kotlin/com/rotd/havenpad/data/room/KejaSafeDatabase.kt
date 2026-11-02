package com.rotd.havenpad.data.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rotd.havenpad.CreateListingRequest
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.data.JsonListingLocalStore
import com.rotd.havenpad.data.ListingLocalStore
import com.rotd.havenpad.data.PendingListingMutation
import com.rotd.havenpad.storage.AndroidAppContext
import kotlinx.serialization.json.Json

@Entity(tableName = "cached_listings")
data class CachedListingEntity(
    @PrimaryKey val id: String,
    val landlordId: String,
    val json: String,
    val updatedAt: Long
)

@Entity(tableName = "pending_listing_mutations")
data class PendingListingMutationEntity(
    @PrimaryKey val id: String,
    val listingId: String,
    val isCreate: Boolean,
    val requestJson: String,
    val createdAt: Long
)

@Dao
interface ListingCacheDao {
    @Query("SELECT * FROM cached_listings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedListingEntity?

    @Query("SELECT * FROM cached_listings WHERE landlordId = :landlordId")
    suspend fun getByLandlord(landlordId: String): List<CachedListingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CachedListingEntity>)

    @Query("SELECT * FROM pending_listing_mutations ORDER BY createdAt ASC")
    suspend fun pendingMutations(): List<PendingListingMutationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMutation(entity: PendingListingMutationEntity)

    @Query("DELETE FROM pending_listing_mutations WHERE id = :id")
    suspend fun deleteMutation(id: String)
}

@Database(
    entities = [CachedListingEntity::class, PendingListingMutationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HavenPadDatabase : RoomDatabase() {
    abstract fun listingCacheDao(): ListingCacheDao

    companion object {
        @Volatile
        private var instance: HavenPadDatabase? = null

        fun get(): HavenPadDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    AndroidAppContext.require(),
                    HavenPadDatabase::class.java,
                    "havenpad_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

class RoomListingLocalStore(
    private val dao: ListingCacheDao = HavenPadDatabase.get().listingCacheDao()
) : ListingLocalStore {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /** One-time seed from the previous JsonCache so offline edit still works after upgrade. */
    private val legacy = JsonListingLocalStore()
    private var seeded = false

    private suspend fun ensureSeeded() {
        if (seeded) return
        seeded = true
        // Seed is best-effort; empty legacy cache is fine.
        runCatching {
            val pending = legacy.pendingMutations()
            pending.forEach { enqueueMutation(it) }
        }
    }

    override suspend fun getListing(id: String): PropertyListing? {
        ensureSeeded()
        val row = dao.getById(id)
        if (row != null) {
            return decodeListing(row.json)
        }
        val legacyListing = legacy.getListing(id) ?: return null
        upsertListing(legacyListing)
        return legacyListing
    }

    override suspend fun upsertListing(listing: PropertyListing) {
        ensureSeeded()
        dao.upsert(listing.toEntity())
        // Keep JsonCache in sync for code paths that still read it.
        legacy.upsertListing(listing)
    }

    override suspend fun upsertListings(listings: List<PropertyListing>) {
        ensureSeeded()
        if (listings.isEmpty()) return
        dao.upsertAll(listings.map { it.toEntity() })
        legacy.upsertListings(listings)
    }

    override suspend fun getListingsForLandlord(landlordId: String): List<PropertyListing> {
        ensureSeeded()
        val rows = dao.getByLandlord(landlordId)
        if (rows.isNotEmpty()) {
            return rows.mapNotNull { decodeListing(it.json) }
        }
        val legacyList = legacy.getListingsForLandlord(landlordId)
        if (legacyList.isNotEmpty()) {
            upsertListings(legacyList)
        }
        return legacyList
    }

    override suspend fun enqueueMutation(mutation: PendingListingMutation) {
        ensureSeeded()
        dao.upsertMutation(
            PendingListingMutationEntity(
                id = mutation.id,
                listingId = mutation.listingId,
                isCreate = mutation.isCreate,
                requestJson = json.encodeToString(CreateListingRequest.serializer(), mutation.request),
                createdAt = mutation.createdAt
            )
        )
        legacy.enqueueMutation(mutation)
    }

    override suspend fun pendingMutations(): List<PendingListingMutation> {
        ensureSeeded()
        val rows = dao.pendingMutations()
        if (rows.isNotEmpty()) {
            return rows.mapNotNull { row ->
                runCatching {
                    PendingListingMutation(
                        id = row.id,
                        listingId = row.listingId,
                        isCreate = row.isCreate,
                        request = json.decodeFromString(
                            CreateListingRequest.serializer(),
                            row.requestJson
                        ),
                        createdAt = row.createdAt
                    )
                }.getOrNull()
            }
        }
        return legacy.pendingMutations()
    }

    override suspend fun removeMutation(id: String) {
        ensureSeeded()
        dao.deleteMutation(id)
        legacy.removeMutation(id)
    }

    private fun PropertyListing.toEntity(): CachedListingEntity =
        CachedListingEntity(
            id = id,
            landlordId = landlordId,
            json = json.encodeToString(PropertyListing.serializer(), this),
            updatedAt = createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        )

    private fun decodeListing(raw: String): PropertyListing? =
        runCatching { json.decodeFromString(PropertyListing.serializer(), raw) }.getOrNull()
}
