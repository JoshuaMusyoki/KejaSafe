# Google Maps Location Flow & Proximity-based Housing Discovery Plan

Implement a proximity discovery flow on the Tenant Home Screen. When a student lands on the homepage, the app prompts for their location or preferred target center (e.g. coordinates or local campus hub coordinates), sends the chosen location context to the backend API, calculates distance using the Haversine formula, and updates the listings display to show properties sorted by proximity.

## User Review Required

> [!NOTE]
> KejaSafe is a Kotlin Multiplatform (KMP) project targeting Android, Desktop, Web, and iOS. To deliver location-aware features robustly without adding complex multiplatform hardware interop dependencies right away, we will introduce a **hybrid strategy**:
> 1. A beautiful interactive bottom sheet dialog overlay asking for location preferences.
> 2. Dual fallback choice vectors:
>    - **Automatic on-device retrieval** (using a common expect/actual hook, fully wired up using native Android GPS fused location provider contexts).
>    - **Manual search input/Campus drop-down override** (ideal for web/desktop or if a tenant explicitly denies system GPS prompt dialog permissions).

---

## Proposed Changes

### Database Layer

#### [MODIFY] [Database.kt](file:///C:/TranssionProjects/KejaSafe/server/src/main/kotlin/com/rotd/kejasafe/db/Database.kt)
- Add double precision float columns to `ListingsTable` to persist precise geolocation contexts for verification and sorting:
  - `latitude = double("latitude").default(-1.2921)` (Defaults to Nairobi central coordinates context)
  - `longitude = double("longitude").default(36.8219)`

### Core Shared Layer

#### [MODIFY] [Models.kt](file:///C:/TranssionProjects/KejaSafe/core/src/commonMain/kotlin/com/rotd/kejasafe/Models.kt)
- Enhance the core `PropertyListing` data class object to include `latitude: Double` and `longitude: Double` properties.

### Multiplatform Platform Layer

#### [NEW] [LocationProvider.kt](file:///C:/TranssionProjects/KejaSafe/app/shared/src/commonMain/kotlin/com/rotd/kejasafe/location/LocationProvider.kt)
- Add an `expect` declaration interface modeling an asynchronous cross-platform location provider context:
  ```kotlin
  package com.rotd.kejasafe.location

  data class GeoCoordinates(val latitude: Double, val longitude: Double)

  expect class LocationProvider() {
      suspend fun getCurrentLocation(): GeoCoordinates?
  }
  ```

#### [NEW] [LocationProvider.android.kt](file:///C:/TranssionProjects/KejaSafe/app/shared/src/androidMain/kotlin/com/rotd/kejasafe/location/LocationProvider.android.kt)
- Add the `actual` class implementation utilizing Android's Fused Location Provider client context. If permission isn't available or fails, returns null gracefully to let the UI seamlessly fall back to manual campus hub parameters.

#### [NEW] [LocationProvider.jvm.kt](file:///C:/TranssionProjects/KejaSafe/app/shared/src/jvmMain/kotlin/com/rotd/kejasafe/location/LocationProvider.jvm.kt) / [LocationProvider.js.kt](file:///C:/TranssionProjects/KejaSafe/app/shared/src/jsMain/kotlin/com/rotd/kejasafe/location/LocationProvider.js.kt)
- Stub standard defaults returning null gracefully to satisfy KMP targets.

### Backend Application Layer

#### [MODIFY] [ListingService.kt](file:///C:/TranssionProjects/KejaSafe/server/src/main/kotlin/com/rotd/kejasafe/listing/ListingService.kt)
- Update `getBrowseListings()` to optionally accept `latitude: Double?` and `longitude: Double?`.
- Implement a math utility implementing the Haversine proximity evaluation equation. Sort listings by distance in kilometers if location is provided.

#### [MODIFY] [Application.kt](file:///C:/TranssionProjects/KejaSafe/server/src/main/kotlin/com/rotd/kejasafe/Application.kt)
- Update `/listings/browse` route registration endpoint block to optional query parameters: `?lat=...&lng=...`.

### API Frontend Layer

#### [MODIFY] [ListingClient.kt](file:///C:/TranssionProjects/KejaSafe/core/src/commonMain/kotlin/com/rotd/kejasafe/network/ListingClient.kt)
- Update multiplatform HTTP call wrapper to inject chosen latitude and longitude query parameters whenever available.

### Mobile Feature Layer

#### [MODIFY] [ListingViewModels.kt](file:///C:/TranssionProjects/KejaSafe/feature/listing/src/commonMain/kotlin/com/rotd/kejasafe/listing/ListingViewModels.kt)
- Enhance `TenantHomeState` to hold current location context and prompt dialog state visibility.
- Inject `LocationProvider` to initiate location fetches and relay data seamlessly into the refresh task.

#### [MODIFY] [TenantHomeScreen.kt](file:///C:/TranssionProjects/KejaSafe/feature/listing/src/commonMain/kotlin/com/rotd/kejasafe/listing/TenantHomeScreen.kt)
- Implement a location onboarding sheet or permission rationale prompt banner.
- Display a proximity-labeled card tag overlay (e.g. `1.2 km away`) if geolocation is loaded.

---

## Verification Plan

### Automated Tests
- Run `:server:compileKotlin` and `:compileKotlin` to ensure multiplatform expectations align cleanly.
- Verify Haversine computation correctness via local unit testing arrays.

### Manual Verification
- Launch Android App emulation.
- Trigger location dialog popup, grant access, verify properties align sorted by closest range parameters context.
