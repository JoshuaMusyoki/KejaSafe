package com.rotd.kejasafe.data

import com.rotd.kejasafe.CreateListingRequest
import com.rotd.kejasafe.IncomingRequest
import com.rotd.kejasafe.ListingBooking
import com.rotd.kejasafe.ListingPerformance
import com.rotd.kejasafe.ListingStatus
import com.rotd.kejasafe.PendingListingPhoto
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.RentBill
import com.rotd.kejasafe.User
import com.rotd.kejasafe.error.AppError
import com.rotd.kejasafe.error.AppException
import com.rotd.kejasafe.error.ErrorMessages
import com.rotd.kejasafe.error.toAppError
import com.rotd.kejasafe.network.ListingClient
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.storage.CacheKeys
import com.rotd.kejasafe.storage.JsonCache
import kotlin.random.Random

data class LandlordHomeSnapshot(
    val listings: List<PropertyListing>,
    val performance: ListingPerformance,
    val requests: List<IncomingRequest>,
    val confirmedBookings: List<ListingBooking>
)

data class ListingSaveResult(
    val listing: PropertyListing,
    /** True when the change is only on-device and waiting for network sync. */
    val pendingSync: Boolean,
    val message: String? = null
)

class ListingRepository(
    private val localStore: ListingLocalStore = createListingLocalStore()
) {
    suspend fun getBrowseListings(
        lat: Double? = null,
        lng: Double? = null,
        onCacheHit: ((List<PropertyListing>) -> Unit)? = null
    ): Result<OfflineData<List<PropertyListing>>> =
        offlineFirst(
            readCache = { JsonCache.getList(CacheKeys.BROWSE_LISTINGS, PropertyListing.serializer()) },
            writeCache = { list ->
                JsonCache.putList(CacheKeys.BROWSE_LISTINGS, list, PropertyListing.serializer())
                localStore.upsertListings(list)
            },
            fetch = { ListingClient.getBrowseListings(lat, lng) },
            onCacheHit = onCacheHit
        )

    suspend fun getMyListings(
        onCacheHit: ((List<PropertyListing>) -> Unit)? = null
    ): Result<OfflineData<List<PropertyListing>>> {
        val userId = requireUserId() ?: return Result.failure(AppException.notSignedIn())
        val key = CacheKeys.mineListings(userId)
        return offlineFirst(
            readCache = {
                val fromRoom = localStore.getListingsForLandlord(userId)
                if (fromRoom.isNotEmpty()) fromRoom
                else JsonCache.getList(key, PropertyListing.serializer())
            },
            writeCache = { list ->
                JsonCache.putList(key, list, PropertyListing.serializer())
                localStore.upsertListings(list)
            },
            fetch = { ListingClient.getMyListings() },
            onCacheHit = onCacheHit
        )
    }

    suspend fun getMyPerformance(
        onCacheHit: ((ListingPerformance) -> Unit)? = null
    ): Result<OfflineData<ListingPerformance>> {
        val userId = requireUserId() ?: return Result.failure(AppException.notSignedIn())
        val key = CacheKeys.performance(userId)
        return offlineFirst(
            readCache = { JsonCache.get(key, ListingPerformance.serializer()) },
            writeCache = { JsonCache.put(key, it, ListingPerformance.serializer()) },
            fetch = { ListingClient.getMyPerformance() },
            onCacheHit = onCacheHit
        )
    }

    suspend fun getIncomingRequests(
        onCacheHit: ((List<IncomingRequest>) -> Unit)? = null
    ): Result<OfflineData<List<IncomingRequest>>> {
        val userId = requireUserId() ?: return Result.failure(AppException.notSignedIn())
        val key = CacheKeys.incomingRequests(userId)
        return offlineFirst(
            readCache = { JsonCache.getList(key, IncomingRequest.serializer()) },
            writeCache = { JsonCache.putList(key, it, IncomingRequest.serializer()) },
            fetch = { ListingClient.getIncomingRequests() },
            onCacheHit = onCacheHit
        )
    }

    suspend fun getMyBookings(
        onCacheHit: ((List<ListingBooking>) -> Unit)? = null
    ): Result<OfflineData<List<ListingBooking>>> {
        val userId = requireUserId() ?: return Result.failure(AppException.notSignedIn())
        val key = CacheKeys.myBookings(userId)
        return offlineFirst(
            readCache = { JsonCache.getList(key, ListingBooking.serializer()) },
            writeCache = { JsonCache.putList(key, it, ListingBooking.serializer()) },
            fetch = { ListingClient.getMyBookings() },
            onCacheHit = onCacheHit
        )
    }

    /** Other confirmed guests on listings you also stay in (tenant↔tenant connect). */
    suspend fun getApartmentMates(): Result<List<ListingBooking>> {
        requireUserId() ?: return Result.failure(AppException.notSignedIn())
        return ListingClient.getApartmentMates()
    }

    suspend fun getListing(
        id: String,
        onCacheHit: ((PropertyListing) -> Unit)? = null
    ): Result<OfflineData<PropertyListing>> {
        val userId = requireUserId()
        return offlineFirst(
            readCache = {
                localStore.getListing(id)
                    ?: userId?.let { uid ->
                        localStore.getListingsForLandlord(uid).firstOrNull { it.id == id }
                    }
                    ?: JsonCache.get(CacheKeys.listing(id), PropertyListing.serializer())
                    ?: userId?.let { uid ->
                        JsonCache.getList(CacheKeys.mineListings(uid), PropertyListing.serializer())
                            ?.firstOrNull { it.id == id }
                    }
            },
            writeCache = { listing ->
                localStore.upsertListing(listing)
                JsonCache.put(CacheKeys.listing(listing.id), listing, PropertyListing.serializer())
            },
            fetch = { ListingClient.getListing(id) },
            onCacheHit = onCacheHit
        )
    }

    /**
     * Create/update a listing. On network failure, persists locally and queues a sync.
     * New photos still require connectivity.
     */
    suspend fun saveListing(
        listingId: String?,
        request: CreateListingRequest,
        photos: List<PendingListingPhoto>
    ): Result<ListingSaveResult> {
        val userId = requireUserId() ?: return Result.failure(AppException.notSignedIn())
        val editing = !listingId.isNullOrBlank()

        if (photos.isNotEmpty()) {
            val remote = if (editing) {
                ListingClient.updateListingWithPhotos(listingId!!, request, photos)
            } else {
                ListingClient.createListingWithPhotos(request, photos)
            }
            val listing = remote.getOrNull()
            if (listing != null) {
                localStore.upsertListing(listing)
                return Result.success(ListingSaveResult(listing = listing, pendingSync = false))
            }
            val error = remote.exceptionOrNull() ?: AppException(AppError.Unknown())
            return if (isQueueableOffline(error)) {
                Result.failure(AppException(AppError.Validation(ErrorMessages.PHOTOS_NEED_CONNECTION)))
            } else {
                Result.failure(AppException.from(error))
            }
        }

        val remote = if (editing) {
            ListingClient.updateListing(listingId!!, request)
        } else {
            ListingClient.createListing(request)
        }

        val listing = remote.getOrNull()
        if (listing != null) {
            localStore.upsertListing(listing)
            return Result.success(ListingSaveResult(listing = listing, pendingSync = false))
        }

        val error = remote.exceptionOrNull() ?: AppException(AppError.Unknown())
        if (!isQueueableOffline(error)) {
            return Result.failure(AppException.from(error))
        }

        val timestamp = platformEpochMillis()
        if (editing) {
            val existing = localStore.getListing(listingId!!)
                ?: PropertyListing(
                    id = listingId,
                    title = request.title,
                    address = request.address,
                    landlordId = userId,
                    status = ListingStatus.PENDING
                )
            val updated = existing.applyRequest(request)
                .copy(landlordId = existing.landlordId.ifBlank { userId })
            localStore.upsertListing(updated)
            localStore.pendingMutations()
                .filter { it.listingId == listingId && !it.isCreate }
                .forEach { localStore.removeMutation(it.id) }
            localStore.enqueueMutation(
                PendingListingMutation(
                    id = newMutationId(),
                    listingId = listingId,
                    isCreate = false,
                    request = request,
                    createdAt = timestamp
                )
            )
            return Result.success(
                ListingSaveResult(
                    listing = updated,
                    pendingSync = true,
                    message = ErrorMessages.SAVED_OFFLINE
                )
            )
        }

        val localId = "local_${Random.nextLong().toULong().toString(16)}"
        val created = PropertyListing(
            id = localId,
            title = request.title,
            address = request.address,
            landlordId = userId,
            pricePerMonth = request.pricePerMonth,
            proximity = request.proximity,
            university = request.university,
            propertyType = request.propertyType,
            unitsCount = request.unitsCount,
            charges = request.charges,
            latitude = request.latitude ?: -1.2921,
            longitude = request.longitude ?: 36.8219,
            status = ListingStatus.PENDING,
            createdAt = timestamp
        )
        localStore.upsertListing(created)
        localStore.enqueueMutation(
            PendingListingMutation(
                id = newMutationId(),
                listingId = localId,
                isCreate = true,
                request = request,
                createdAt = timestamp
            )
        )
        return Result.success(
            ListingSaveResult(
                listing = created,
                pendingSync = true,
                message = ErrorMessages.SAVED_OFFLINE
            )
        )
    }

    /** Push queued create/update mutations when connectivity is back. */
    suspend fun syncPendingMutations(): Int {
        var synced = 0
        val pending = localStore.pendingMutations()
        for (mutation in pending) {
            val result = if (mutation.isCreate) {
                ListingClient.createListing(mutation.request)
            } else {
                ListingClient.updateListing(mutation.listingId, mutation.request)
            }
            val listing = result.getOrNull()
            if (listing != null) {
                localStore.upsertListing(listing)
                localStore.removeMutation(mutation.id)
                synced++
                continue
            }
            val failure = result.exceptionOrNull() ?: continue
            if (isQueueableOffline(failure)) return synced
            if (!failure.toAppError().canRetry) {
                localStore.removeMutation(mutation.id)
            } else {
                return synced
            }
        }
        return synced
    }

    /**
     * Landlord home: seed from cache immediately when possible, then refresh each feed.
     * Listings drive the hard error; other feeds degrade gracefully.
     */
    suspend fun loadLandlordHome(
        onCacheHit: ((LandlordHomeSnapshot) -> Unit)? = null
    ): Result<OfflineData<LandlordHomeSnapshot>> {
        try {
            syncPendingMutations()
        } catch (_: Exception) {
            // Best-effort sync; home load continues from cache/network.
        }

        var seededListings: List<PropertyListing>? = null
        var seededPerformance: ListingPerformance? = null
        var seededRequests: List<IncomingRequest>? = null
        var seededBookings: List<ListingBooking>? = null

        fun maybeEmitCacheSeed() {
            val listings = seededListings ?: return
            onCacheHit?.invoke(
                LandlordHomeSnapshot(
                    listings = listings,
                    performance = seededPerformance ?: listings.asPerformanceFallback(),
                    requests = seededRequests.orEmpty(),
                    confirmedBookings = seededBookings.orEmpty()
                        .filter { it.status == com.rotd.kejasafe.BookingStatus.CONFIRMED }
                )
            )
        }

        val listings = getMyListings(onCacheHit = {
            seededListings = it
            maybeEmitCacheSeed()
        }).getOrElse { return Result.failure(it) }

        val performanceResult = getMyPerformance(onCacheHit = {
            seededPerformance = it
            maybeEmitCacheSeed()
        })
        val requestsResult = getIncomingRequests(onCacheHit = {
            seededRequests = it
            maybeEmitCacheSeed()
        })
        val bookingsResult = getMyBookings(onCacheHit = {
            seededBookings = it
            maybeEmitCacheSeed()
        })

        val performance = performanceResult.getOrNull()?.value
            ?: listings.value.asPerformanceFallback()
        val requests = requestsResult.getOrNull()?.value.orEmpty()
        val confirmed = bookingsResult.getOrNull()?.value
            .orEmpty()
            .filter { it.status == com.rotd.kejasafe.BookingStatus.CONFIRMED }

        val warning = listOfNotNull(
            listings.syncWarning,
            performanceResult.getOrNull()?.syncWarning,
            requestsResult.getOrNull()?.syncWarning,
            bookingsResult.getOrNull()?.syncWarning
        ).firstOrNull()

        return Result.success(
            OfflineData(
                value = LandlordHomeSnapshot(
                    listings = listings.value,
                    performance = performance,
                    requests = requests,
                    confirmedBookings = confirmed
                ),
                fromCache = listings.fromCache,
                syncWarning = warning
            )
        )
    }

    private fun List<PropertyListing>.asPerformanceFallback(): ListingPerformance {
        val totalViews = sumOf { it.totalViews }
        val inquiries = sumOf { it.inquiries }
        val activeBookings = sumOf { it.activeBookings }
        val units = sumOf { it.unitsCount ?: 0 }.coerceAtLeast(activeBookings).coerceAtLeast(1)
        return ListingPerformance(
            totalViews = totalViews,
            viewsDeltaPercent = 0,
            inquiries = inquiries,
            hasNewInquiries = false,
            activeBookings = activeBookings,
            unitsCount = units
        )
    }

    private fun requireUserId(): String? {
        SessionManager.restore()
        return SessionManager.currentUser.value?.id
    }
}

private fun isQueueableOffline(error: Throwable): Boolean {
    val appError = error.toAppError()
    return appError is AppError.NoInternet ||
        appError is AppError.Timeout ||
        appError is AppError.ServerUnavailable
}

class RentRepository {
    suspend fun getBills(
        onCacheHit: ((List<RentBill>) -> Unit)? = null
    ): Result<OfflineData<List<RentBill>>> {
        val userId = SessionManager.currentUser.value?.id
            ?: return Result.failure(AppException.notSignedIn())
        val key = CacheKeys.bills(userId)
        return offlineFirst(
            readCache = { JsonCache.getList(key, RentBill.serializer()) },
            writeCache = { JsonCache.putList(key, it, RentBill.serializer()) },
            fetch = { ListingClient.getBills() },
            onCacheHit = onCacheHit
        )
    }
}

class ProfileRepository(
    private val authRepository: com.rotd.kejasafe.domain.repository.AuthRepository,
) {
    suspend fun getProfile(
        onCacheHit: ((User) -> Unit)? = null
    ): Result<OfflineData<User>> {
        val userId = SessionManager.currentUser.value?.id
        val sessionUser = SessionManager.currentUser.value
        val key = userId?.let { CacheKeys.profile(it) }

        return offlineFirst(
            readCache = {
                when {
                    key != null -> JsonCache.get(key, User.serializer()) ?: sessionUser
                    else -> sessionUser
                }
            },
            writeCache = { user ->
                SessionManager.updateUser(user)
                JsonCache.put(CacheKeys.profile(user.id), user, User.serializer())
            },
            fetch = { authRepository.refreshProfile() },
            onCacheHit = onCacheHit
        )
    }
}
