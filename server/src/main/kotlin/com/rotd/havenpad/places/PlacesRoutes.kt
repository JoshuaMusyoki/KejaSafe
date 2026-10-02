package com.rotd.havenpad.places

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.placesRoutes() {
    get("/places/autocomplete") {
        val query = call.request.queryParameters["q"]?.trim().orEmpty()
        if (query.length < 2) {
            call.respond(com.rotd.havenpad.PlaceSearchResponse())
            return@get
        }
        call.respond(PlacesService.autocomplete(query))
    }

    get("/places/details") {
        val placeId = call.request.queryParameters["placeId"]?.trim().orEmpty()
        val details = PlacesService.details(placeId)
        if (details == null) {
            call.respond(HttpStatusCode.NotFound, "Place not found")
        } else {
            call.respond(details)
        }
    }
}
