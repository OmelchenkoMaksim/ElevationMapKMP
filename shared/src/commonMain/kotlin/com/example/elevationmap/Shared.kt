@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package com.example.elevationmap


const val zoomRate = 15f
const val findMe = "На стартовую позицию\n(в Москву)"

data class MapUiSettings(
    val isFindMeButtonClicked: Boolean = false
)

expect interface PermissionStateShared
expect interface ContextShared

expect class LocationShared
expect class GoogleMapShared

expect fun GoogleMapShared.setCenter(location: LocationShared, animated: Boolean)

enum class PermissionStatus {
    Granted,
    Denied,
    Restricted,
    Unknown
}

expect fun setupMapUI(map: GoogleMapShared)

expect fun handleLocationPermission(
    map: GoogleMapShared,
    locationPermissionState: PermissionStateShared,
    context: ContextShared
)

expect suspend fun findMyLocation(
    map: GoogleMapShared,
    context: ContextShared
)

expect suspend fun getLastKnownLocation(
    context: ContextShared
): LocationShared?
