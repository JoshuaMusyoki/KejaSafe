package com.rotd.kejasafe.places

import com.rotd.kejasafe.LandmarkDetails
import com.rotd.kejasafe.LandmarkSuggestion
import com.rotd.kejasafe.PlaceSearchResponse
import com.rotd.kejasafe.config.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object PlacesService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    suspend fun autocomplete(query: String): PlaceSearchResponse {
        val key = ServerConfig.googlePlacesApiKey
        if (key.isNullOrBlank()) return PlaceSearchResponse(configured = false)
        val response = client.post("https://places.googleapis.com/v1/places:autocomplete") {
            header("X-Goog-Api-Key", key)
            contentType(ContentType.Application.Json)
            setBody(
                GoogleAutocompleteRequest(
                    input = query,
                    includedRegionCodes = listOf("ke"),
                    languageCode = "en"
                )
            )
        }
        if (!response.status.isSuccess()) return PlaceSearchResponse(suggestions = emptyList())
        val body = response.body<GoogleAutocompleteResponse>()
        val suggestions = body.suggestions.mapNotNull { suggestion ->
            val prediction = suggestion.placePrediction ?: return@mapNotNull null
            val name = prediction.structuredFormat?.mainText?.text
                ?.ifBlank { null }
                ?: prediction.text?.text
                ?: return@mapNotNull null
            val secondary = prediction.structuredFormat?.secondaryText?.text.orEmpty()
            val placeId = prediction.placeId.ifBlank { prediction.place.substringAfterLast('/') }
            if (placeId.isBlank()) return@mapNotNull null
            LandmarkSuggestion(
                placeId = placeId,
                name = name,
                description = secondary.ifBlank { prediction.text?.text.orEmpty() }
            )
        }
        return PlaceSearchResponse(suggestions = suggestions)
    }

    suspend fun details(placeId: String): LandmarkDetails? {
        val key = ServerConfig.googlePlacesApiKey ?: return null
        if (placeId.isBlank()) return null
        val response: HttpResponse = client.get("https://places.googleapis.com/v1/places/$placeId") {
            header("X-Goog-Api-Key", key)
            header("X-Goog-FieldMask", "id,displayName,formattedAddress,location")
        }
        if (!response.status.isSuccess()) return null
        val place = response.body<GooglePlace>()
        val location = place.location ?: return null
        val name = place.displayName?.text?.ifBlank { null } ?: place.formattedAddress
        return LandmarkDetails(
            placeId = place.id.ifBlank { placeId },
            name = name,
            address = place.formattedAddress,
            latitude = location.latitude,
            longitude = location.longitude
        )
    }
}

@Serializable
private data class GoogleAutocompleteRequest(
    val input: String,
    val includedRegionCodes: List<String> = emptyList(),
    val languageCode: String = "en"
)

@Serializable
private data class GoogleAutocompleteResponse(
    val suggestions: List<GoogleSuggestion> = emptyList()
)

@Serializable
private data class GoogleSuggestion(
    val placePrediction: GooglePlacePrediction? = null
)

@Serializable
private data class GooglePlacePrediction(
    val placeId: String = "",
    val place: String = "",
    val text: GoogleText? = null,
    val structuredFormat: GoogleStructuredFormat? = null
)

@Serializable
private data class GoogleStructuredFormat(
    val mainText: GoogleText? = null,
    val secondaryText: GoogleText? = null
)

@Serializable
private data class GoogleText(val text: String = "")

@Serializable
private data class GooglePlace(
    val id: String = "",
    val formattedAddress: String = "",
    val displayName: GoogleText? = null,
    val location: GoogleLatLng? = null
)

@Serializable
private data class GoogleLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
