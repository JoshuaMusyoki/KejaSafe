package com.rotd.havenpad.places

import com.rotd.havenpad.CampusSearchResponse
import com.rotd.havenpad.CampusSuggestion
import com.rotd.havenpad.KenyaCampuses
import com.rotd.havenpad.LandmarkDetails
import com.rotd.havenpad.LandmarkSuggestion
import com.rotd.havenpad.PlaceSearchResponse
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.utils.calculateDistance
import kotlin.math.roundToInt
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

object PlacesService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    /** Rough Kenya centroid — bias housing search without locking to Nairobi only. */
    private const val KENYA_LON = 37.9062
    private const val KENYA_LAT = -0.0236
    private const val KENYA_BIAS_RADIUS_M = 550_000.0

    /** Mapbox / admin feature types that are towns, wards, or regions — not buildings. */
    private val adminFeatureTypes = setOf(
        "country", "region", "district", "place", "locality", "neighborhood",
        "postcode", "category", "brand",
    )

    suspend fun autocomplete(query: String): PlaceSearchResponse {
        // Apartment/building field only — never return towns like "Chuka, Karingani".
        val googleBuildings = googleAutocomplete(query, preferBuildings = true)
        if (googleBuildings.configured && googleBuildings.suggestions.isNotEmpty()) {
            return googleBuildings
        }
        val mapbox = ServerConfig.mapboxAccessToken
        if (!mapbox.isNullOrBlank()) {
            mapboxSuggest(query, mapbox, types = "poi,address")
                ?.takeIf { it.suggestions.isNotEmpty() }
                ?.let { return it }
        }
        // Street-level Google only if housing types returned nothing. Still no cities/regions.
        val googleStreets = googleAutocomplete(query, preferBuildings = false)
        if (googleStreets.configured && googleStreets.suggestions.isNotEmpty()) {
            return googleStreets.copy(
                suggestions = googleStreets.suggestions.filterNot { looksLikeAdminArea(it) },
            )
        }
        return PlaceSearchResponse(
            suggestions = emptyList(),
            configured = googleBuildings.configured ||
                googleStreets.configured ||
                !mapbox.isNullOrBlank(),
        )
    }

    private fun looksLikeAdminArea(suggestion: LandmarkSuggestion): Boolean {
        val blob = "${suggestion.name} ${suggestion.description}".lowercase()
        if (adminFeatureTypes.any { blob.contains(" · $it") || blob.endsWith(it) }) return true
        // "Chuka, Karingani, Tharaka-Nithi Kenya" style geocoder / locality labels
        val commas = suggestion.name.count { it == ',' }
        if (commas >= 2 && !suggestion.name.any { it.isDigit() }) return true
        val adminWords = listOf("county", "ward", "constituency", "sub-county", "subcounty")
        if (adminWords.any { blob.contains(it) } && !suggestion.name.any { it.isDigit() }) return true
        return false
    }

    suspend fun details(placeId: String): LandmarkDetails? {
        if (placeId.isBlank()) return null
        if (placeId.startsWith("mb|")) {
            val token = ServerConfig.mapboxAccessToken ?: return null
            return mapboxRetrieve(placeId, token)
        }
        val google = googleDetails(placeId) ?: return null
        val token = ServerConfig.mapboxAccessToken
        if (token.isNullOrBlank()) return google
        val road = nearestRoad(google.longitude, google.latitude, token)
        return google.copy(
            distanceToRoadMeters = road?.meters,
            roadName = road?.name,
        )
    }

    private suspend fun mapboxSuggest(
        query: String,
        token: String,
        types: String?,
    ): PlaceSearchResponse? {
        val session = UUID.randomUUID().toString()
        val response = client.get("https://api.mapbox.com/search/searchbox/v1/suggest") {
            parameter("q", query)
            parameter("access_token", token)
            parameter("session_token", session)
            parameter("country", "KE")
            parameter("language", "en")
            parameter("limit", 8)
            if (!types.isNullOrBlank()) {
                parameter("types", types)
            }
            parameter("proximity", "$KENYA_LON,$KENYA_LAT")
        }
        if (!response.status.isSuccess()) return null
        val body = response.body<MapboxSuggestResponse>()
        val suggestions = body.suggestions.mapNotNull { item ->
            val featureType = item.featureType?.lowercase().orEmpty()
            if (featureType in adminFeatureTypes) return@mapNotNull null
            val id = item.mapboxId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = item.name?.takeIf { it.isNotBlank() }
                ?: item.namePreferred?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val description = listOfNotNull(
                item.fullAddress?.takeIf { it.isNotBlank() && !it.equals(name, ignoreCase = true) },
                item.placeFormatted?.takeIf { it.isNotBlank() },
                item.featureType?.takeIf { it.isNotBlank() },
            ).distinct().joinToString(" · ")
            LandmarkSuggestion(
                placeId = "mb|$id|$session",
                name = name,
                description = description,
            )
        }.filterNot { looksLikeAdminArea(it) }
        return PlaceSearchResponse(suggestions = suggestions, configured = true)
    }

    private suspend fun mapboxRetrieve(encodedPlaceId: String, token: String): LandmarkDetails? {
        val parts = encodedPlaceId.split('|')
        if (parts.size < 3 || parts[0] != "mb") return null
        val mapboxId = parts[1]
        val session = parts[2]
        val encodedId = java.net.URLEncoder.encode(mapboxId, Charsets.UTF_8)
            .replace("+", "%20")
        val response = client.get("https://api.mapbox.com/search/searchbox/v1/retrieve/$encodedId") {
            parameter("access_token", token)
            parameter("session_token", session)
            parameter("language", "en")
        }
        if (!response.status.isSuccess()) return null
        val body = response.body<MapboxRetrieveResponse>()
        val feature = body.features.firstOrNull() ?: return null
        val props = feature.properties ?: return null
        val lon = props.coordinates?.longitude
            ?: feature.geometry?.coordinates?.getOrNull(0)
            ?: return null
        val lat = props.coordinates?.latitude
            ?: feature.geometry?.coordinates?.getOrNull(1)
            ?: return null
        val name = props.namePreferred?.takeIf { it.isNotBlank() }
            ?: props.name?.takeIf { it.isNotBlank() }
            ?: return null
        val address = props.fullAddress?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(props.address, props.placeFormatted)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { name }
        val road = nearestRoad(lon, lat, token)
        return LandmarkDetails(
            placeId = encodedPlaceId,
            name = name,
            address = address,
            latitude = lat,
            longitude = lon,
            distanceToRoadMeters = road?.meters,
            roadName = road?.name,
        )
    }

    private data class RoadHit(val meters: Int, val name: String?)

    private suspend fun nearestRoad(lon: Double, lat: Double, token: String): RoadHit? {
        val response = client.get(
            "https://api.mapbox.com/v4/mapbox.mapbox-streets-v8/tilequery/$lon,$lat.json"
        ) {
            parameter("access_token", token)
            parameter("layers", "road")
            parameter("radius", 500)
            parameter("limit", 12)
        }
        if (!response.status.isSuccess()) return null
        val body = response.body<MapboxTilequeryResponse>()
        val preferred = listOf("motorway", "trunk", "primary", "secondary", "tertiary", "street")
        val ranked = body.features.mapNotNull { feature ->
            val klass = feature.properties?.klass?.lowercase().orEmpty()
            val distance = feature.properties?.tilequery?.distance ?: return@mapNotNull null
            val rank = preferred.indexOf(klass).let { if (it < 0) preferred.size else it }
            Triple(rank, distance, feature.properties?.name)
        }.sortedWith(compareBy({ it.first }, { it.second }))
        val best = ranked.firstOrNull() ?: return null
        return RoadHit(meters = best.second.roundToInt().coerceAtLeast(0), name = best.third)
    }

    private suspend fun googleAutocomplete(
        query: String,
        preferBuildings: Boolean,
    ): PlaceSearchResponse {
        val key = ServerConfig.googlePlacesApiKey
        if (key.isNullOrBlank()) {
            return PlaceSearchResponse(configured = false)
        }

        val response = client.post("https://places.googleapis.com/v1/places:autocomplete") {
            header("X-Goog-Api-Key", key)
            contentType(ContentType.Application.Json)
            setBody(
                GoogleAutocompleteRequest(
                    input = query,
                    includedRegionCodes = listOf("KE"),
                    // At most five Table A/B types. Invalid or >5 types make Google reject the call.
                    includedPrimaryTypes = if (preferBuildings) {
                        listOf(
                            "apartment_building",
                            "apartment_complex",
                            "condominium_complex",
                            "housing_complex",
                            "premise",
                        )
                    } else {
                        listOf(
                            "street_address",
                            "route",
                            "premise",
                            "subpremise",
                            "point_of_interest",
                        )
                    },
                    locationBias = GoogleLocationBias(
                        circle = GoogleCircle(
                            center = GoogleLatLng(latitude = KENYA_LAT, longitude = KENYA_LON),
                            radius = KENYA_BIAS_RADIUS_M,
                        )
                    ),
                    languageCode = "en"
                )
            )
        }
        if (!response.status.isSuccess()) {
            return PlaceSearchResponse(configured = false, suggestions = emptyList())
        }
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
        return PlaceSearchResponse(suggestions = suggestions, configured = true)
    }

    private suspend fun googleDetails(placeId: String): LandmarkDetails? {
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

    /**
     * Campuses near a listing pin. Uses Google Places (and Mapbox when set) so
     * towns outside the curated Nairobi list still get real university suggestions.
     */
    suspend fun nearbyCampuses(latitude: Double, longitude: Double): CampusSearchResponse {
        val remote = mutableListOf<CampusSuggestion>()
        remote += googleNearbyUniversities(latitude, longitude)
        val mapbox = ServerConfig.mapboxAccessToken
        if (!mapbox.isNullOrBlank()) {
            remote += mapboxNearbyUniversities(latitude, longitude, mapbox)
        }
        val curated = KenyaCampuses.all.map { campus ->
            val km = calculateDistance(latitude, longitude, campus.latitude, campus.longitude)
            CampusSuggestion(
                name = campus.name,
                latitude = campus.latitude,
                longitude = campus.longitude,
                distanceMeters = (km * 1000).roundToInt(),
            )
        }.filter { (it.distanceMeters ?: Int.MAX_VALUE) <= 45_000 }

        val merged = LinkedHashMap<String, CampusSuggestion>()
        (remote + curated).forEach { campus ->
            val key = campus.name.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
            val existing = merged[key]
            if (existing == null) {
                merged[key] = campus
            } else {
                val existingDist = existing.distanceMeters ?: Int.MAX_VALUE
                val nextDist = campus.distanceMeters ?: Int.MAX_VALUE
                if (nextDist < existingDist) merged[key] = campus
            }
        }
        val campuses = merged.values
            .sortedBy { it.distanceMeters ?: Int.MAX_VALUE }
            .take(10)
        val configured = !ServerConfig.googlePlacesApiKey.isNullOrBlank() ||
            !ServerConfig.mapboxAccessToken.isNullOrBlank()
        return CampusSearchResponse(campuses = campuses, configured = configured || campuses.isNotEmpty())
    }

    private suspend fun googleNearbyUniversities(
        latitude: Double,
        longitude: Double,
    ): List<CampusSuggestion> {
        val key = ServerConfig.googlePlacesApiKey ?: return emptyList()
        val response = client.post("https://places.googleapis.com/v1/places:searchNearby") {
            header("X-Goog-Api-Key", key)
            header(
                "X-Goog-FieldMask",
                "places.id,places.displayName,places.location,places.formattedAddress",
            )
            contentType(ContentType.Application.Json)
            setBody(
                GoogleNearbySearchRequest(
                    includedTypes = listOf("university"),
                    maxResultCount = 10,
                    rankPreference = "DISTANCE",
                    locationRestriction = GoogleLocationRestriction(
                        circle = GoogleCircle(
                            center = GoogleLatLng(latitude = latitude, longitude = longitude),
                            radius = 25_000.0,
                        )
                    ),
                )
            )
        }
        if (!response.status.isSuccess()) return emptyList()
        val body = response.body<GoogleNearbySearchResponse>()
        return body.places.mapNotNull { place ->
            val location = place.location ?: return@mapNotNull null
            val name = place.displayName?.text?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val km = calculateDistance(latitude, longitude, location.latitude, location.longitude)
            CampusSuggestion(
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
                distanceMeters = (km * 1000).roundToInt(),
                placeId = place.id.takeIf { it.isNotBlank() },
            )
        }
    }

    private suspend fun mapboxNearbyUniversities(
        latitude: Double,
        longitude: Double,
        token: String,
    ): List<CampusSuggestion> {
        val response = client.get("https://api.mapbox.com/search/searchbox/v1/forward") {
            parameter("q", "university")
            parameter("access_token", token)
            parameter("country", "KE")
            parameter("language", "en")
            parameter("limit", 8)
            parameter("types", "poi")
            parameter("proximity", "$longitude,$latitude")
        }
        if (!response.status.isSuccess()) return emptyList()
        val body = response.body<MapboxRetrieveResponse>()
        return body.features.mapNotNull { feature ->
            val props = feature.properties ?: return@mapNotNull null
            val lon = props.coordinates?.longitude
                ?: feature.geometry?.coordinates?.getOrNull(0)
                ?: return@mapNotNull null
            val lat = props.coordinates?.latitude
                ?: feature.geometry?.coordinates?.getOrNull(1)
                ?: return@mapNotNull null
            val name = props.namePreferred?.takeIf { it.isNotBlank() }
                ?: props.name?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            if (!name.contains("university", ignoreCase = true) &&
                !name.contains("campus", ignoreCase = true) &&
                !name.contains("college", ignoreCase = true)
            ) {
                return@mapNotNull null
            }
            val km = calculateDistance(latitude, longitude, lat, lon)
            CampusSuggestion(
                name = name,
                latitude = lat,
                longitude = lon,
                distanceMeters = (km * 1000).roundToInt(),
            )
        }
    }
}

@Serializable
private data class MapboxSuggestResponse(
    val suggestions: List<MapboxSuggestion> = emptyList(),
)

@Serializable
private data class MapboxSuggestion(
    @SerialName("mapbox_id") val mapboxId: String? = null,
    val name: String? = null,
    @SerialName("name_preferred") val namePreferred: String? = null,
    @SerialName("full_address") val fullAddress: String? = null,
    @SerialName("place_formatted") val placeFormatted: String? = null,
    @SerialName("feature_type") val featureType: String? = null,
)

@Serializable
private data class MapboxRetrieveResponse(
    val features: List<MapboxFeature> = emptyList(),
)

@Serializable
private data class MapboxFeature(
    val geometry: MapboxGeometry? = null,
    val properties: MapboxFeatureProperties? = null,
)

@Serializable
private data class MapboxGeometry(
    val coordinates: List<Double> = emptyList(),
)

@Serializable
private data class MapboxFeatureProperties(
    val name: String? = null,
    @SerialName("name_preferred") val namePreferred: String? = null,
    val address: String? = null,
    @SerialName("full_address") val fullAddress: String? = null,
    @SerialName("place_formatted") val placeFormatted: String? = null,
    val coordinates: MapboxCoords? = null,
)

@Serializable
private data class MapboxCoords(
    val longitude: Double = 0.0,
    val latitude: Double = 0.0,
)

@Serializable
private data class MapboxTilequeryResponse(
    val features: List<MapboxTileFeature> = emptyList(),
)

@Serializable
private data class MapboxTileFeature(
    val properties: MapboxTileProperties? = null,
)

@Serializable
private data class MapboxTileProperties(
    val name: String? = null,
    @SerialName("class") val klass: String? = null,
    val tilequery: MapboxTilequeryMeta? = null,
)

@Serializable
private data class MapboxTilequeryMeta(
    val distance: Double? = null,
)

@Serializable
private data class GoogleCircle(
    val center: GoogleLatLng,
    val radius: Double
)

@Serializable
private data class GoogleLocationBias(
    val circle: GoogleCircle
)

@Serializable
private data class GoogleAutocompleteRequest(
    val input: String,
    val includedRegionCodes: List<String> = listOf("KE", "ke"),
    val includedPrimaryTypes: List<String>? = null,
    val locationBias: GoogleLocationBias,
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

@Serializable
private data class GoogleNearbySearchRequest(
    val includedTypes: List<String>,
    val maxResultCount: Int = 10,
    val rankPreference: String = "DISTANCE",
    val locationRestriction: GoogleLocationRestriction,
)

@Serializable
private data class GoogleLocationRestriction(
    val circle: GoogleCircle,
)

@Serializable
private data class GoogleNearbySearchResponse(
    val places: List<GoogleNearbyPlace> = emptyList(),
)

@Serializable
private data class GoogleNearbyPlace(
    val id: String = "",
    val formattedAddress: String = "",
    val displayName: GoogleText? = null,
    val location: GoogleLatLng? = null,
)
