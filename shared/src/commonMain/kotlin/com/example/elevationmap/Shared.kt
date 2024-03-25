@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.example.elevationmap

expect interface PermissionStateShared
expect interface ContextShared

expect class LocationShared
expect class GoogleMapShared

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

object Common {

    const val ZOOM_RATE = 15f
    const val FIND_ME = "На старт (в Москву)"

    data class MapUiSettings(
        val isFindMeButtonClicked: Boolean = false
    )

    enum class PermissionStatus {
        Granted,
        Denied,
        Restricted,
        Unknown
    }
}
