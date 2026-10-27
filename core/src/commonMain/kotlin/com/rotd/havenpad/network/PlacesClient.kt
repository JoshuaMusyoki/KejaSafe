package com.rotd.havenpad.network

import com.rotd.havenpad.CampusSearchResponse
import com.rotd.havenpad.CampusSuggestion
import com.rotd.havenpad.LandmarkDetails
import com.rotd.havenpad.LandmarkSuggestion
import com.rotd.havenpad.LocationProvider
import com.rotd.havenpad.PlaceSearchResponse
import com.rotd.havenpad.error.AppException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object PlacesClient {
    private val client = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun autocomplete(query: String): Result<PlaceSearchResponse> = try {
        val token = SessionManager.token.value?.let { "Bearer $it" }
        val response = client.get("${AuthClient.BASE_URL}/places/autocomplete") {
            if (token != null) header(HttpHeaders.Authorization, token)
            parameter("q", query)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.success(PlaceSearchResponse(configured = false))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun details(placeId: String): Result<LandmarkDetails> = try {
        val token = SessionManager.token.value?.let { "Bearer $it" }
            ?: return Result.failure(AppException.notSignedIn())
        val response = client.get("${AuthClient.BASE_URL}/places/details") {
            header(HttpHeaders.Authorization, token)
            parameter("placeId", placeId)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.failure(AppException.from(IllegalStateException("Could not load that place")))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun nearbyCampuses(latitude: Double, longitude: Double): Result<List<CampusSuggestion>> = try {
        val token = SessionManager.token.value?.let { "Bearer $it" }
            ?: return Result.failure(AppException.notSignedIn())
        val response = client.get("${AuthClient.BASE_URL}/places/campuses") {
            header(HttpHeaders.Authorization, token)
            parameter("lat", latitude)
            parameter("lng", longitude)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body<CampusSearchResponse>().campuses)
        } else {
            Result.success(emptyList())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    /** Device geocoder suggestions when Google Places is not configured. */
    suspend fun deviceSuggestions(query: String): List<LandmarkSuggestion> {
        return runCatching { LocationProvider().findPlaces(query) }.getOrNull().orEmpty()
            .map { place ->
                LandmarkSuggestion(
                    placeId = "geo:${place.latitude},${place.longitude}",
                    name = place.name,
                    description = place.name,
                    latitude = place.latitude,
                    longitude = place.longitude
                )
            }
    }
}
