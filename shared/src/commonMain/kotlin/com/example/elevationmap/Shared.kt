@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package com.example.elevationmap

const val zoomRate = 15f
const val findMe = "Найти меня"

data class MapUiSettings(
    val isFindMeButtonClicked: Boolean = false
)

expect interface PermissionStateShared
expect interface FusedLocationProviderClientShared
expect interface ContextShared

expect class LocationShared
expect class GoogleMapShared

expect fun setupMapUI(map: GoogleMapShared)

expect fun handleLocationPermission(
    map: GoogleMapShared,
    locationPermissionState: PermissionStateShared,
    fusedLocationProviderClient: FusedLocationProviderClientShared,
    context: ContextShared
)

expect suspend fun findMyLocation(
    map: GoogleMapShared,
    fusedLocationClient: FusedLocationProviderClientShared,
    context: ContextShared
)

expect suspend fun getLastKnownLocation(
    fusedLocationClient: FusedLocationProviderClientShared,
    context: ContextShared
): LocationShared?
