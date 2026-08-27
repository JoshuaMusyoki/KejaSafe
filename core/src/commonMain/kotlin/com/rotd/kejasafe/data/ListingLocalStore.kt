package com.rotd.kejasafe.data

import com.rotd.kejasafe.CreateListingRequest
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.storage.CacheKeys
import com.rotd.kejasafe.storage.JsonCache
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Local persistence for listings + offline edit queue.
 * Android uses Room; other targets use [JsonCache].
 */
interface ListingLocalStore {
    suspend fun getListing(id: String): PropertyListing?
    suspend fun upsertListing(listing: PropertyListing)
    suspend fun upsertListings(listings: List<PropertyListing>)
    suspend fun getListingsForLandlord(landlordId: String): List<PropertyListing>

    suspend fun enqueueMutation(mutation: PendingListingMutation)
    suspend fun pendingMutations(): List<PendingListingMutation>
    suspend fun removeMutation(id: String)
}

@Serializable
data class PendingListingMutation(
    val id: String,
    /** Existing listing id, or a local placeholder for creates. */
    val listingId: String,
    val isCreate: Boolean,
    val request: CreateListingRequest,
    val createdAt: Long
)

fun newMutationId(): String =
    "mut_${Random.nextLong().toULong().toString(16)}"

/**
 * Settings-backed store — used on non-Android targets and as a migration seed source.
 */
class JsonListingLocalStore : ListingLocalStore {
    override suspend fun getListing(id: String): PropertyListing? =
        JsonCache.get(CacheKeys.listing(id), PropertyListing.serializer())

    override suspend fun upsertListing(listing: PropertyListing) {
        JsonCache.put(CacheKeys.listing(listing.id), listing, PropertyListing.serializer())
        val landlordId = listing.landlordId
        if (landlordId.isNotBlank()) {
            val key = CacheKeys.mineListings(landlordId)
            val current = JsonCache.getList(key, PropertyListing.serializer()).orEmpty()
            val next = listOf(listing) + current.filterNot { it.id == listing.id }
            JsonCache.putList(key, next, PropertyListing.serializer())
        }
    }

    override suspend fun upsertListings(listings: List<PropertyListing>) {
        listings.forEach { listing ->
            JsonCache.put(CacheKeys.listing(listing.id), listing, PropertyListing.serializer())
        }
        val byLandlord = listings.groupBy { it.landlordId }.filterKeys { it.isNotBlank() }
        byLandlord.forEach { (landlordId, group) ->
            JsonCache.putList(
                CacheKeys.mineListings(landlordId),
                group,
                PropertyListing.serializer()
            )
        }
    }

    override suspend fun getListingsForLandlord(landlordId: String): List<PropertyListing> =
        JsonCache.getList(CacheKeys.mineListings(landlordId), PropertyListing.serializer())
            .orEmpty()

    override suspend fun enqueueMutation(mutation: PendingListingMutation) {
        val all = pendingMutations().filterNot { it.id == mutation.id } + mutation
        JsonCache.putList(CacheKeys.PENDING_LISTING_MUTATIONS, all, PendingListingMutation.serializer())
    }

    override suspend fun pendingMutations(): List<PendingListingMutation> =
        JsonCache.getList(CacheKeys.PENDING_LISTING_MUTATIONS, PendingListingMutation.serializer())
            .orEmpty()
            .sortedBy { it.createdAt }

    override suspend fun removeMutation(id: String) {
        val next = pendingMutations().filterNot { it.id == id }
        JsonCache.putList(CacheKeys.PENDING_LISTING_MUTATIONS, next, PendingListingMutation.serializer())
    }
}

expect fun createListingLocalStore(): ListingLocalStore

fun PropertyListing.applyRequest(request: CreateListingRequest): PropertyListing = copy(
    title = request.title,
    address = request.address,
    proximity = request.proximity,
    university = request.university,
    pricePerMonth = request.pricePerMonth,
    propertyType = request.propertyType,
    unitsCount = request.unitsCount,
    charges = request.charges
)
