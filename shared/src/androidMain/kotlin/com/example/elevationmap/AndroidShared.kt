@file:Suppress("actual_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(ExperimentalPermissionsApi::class)

package com.example.elevationmap

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine


val moscowLatLng = LatLng(55.7558, 37.6176)

@OptIn(ExperimentalPermissionsApi::class)
actual interface PermissionStateShared : com.google.accompanist.permissions.PermissionState
actual interface ContextShared {
    val context: android.content.Context
}

actual interface FusedLocationProviderClientShared : com.google.android.gms.location.FusedLocationProviderClient

actual typealias LocationShared = Location
actual typealias GoogleMapShared = GoogleMap


actual fun setupMapUI(map: GoogleMapShared) {
    map.uiSettings.isZoomControlsEnabled = true
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(moscowLatLng, zoomRate))
    map.mapType = GoogleMap.MAP_TYPE_TERRAIN
}


@OptIn(ExperimentalPermissionsApi::class)
actual fun handleLocationPermission(
    map: GoogleMapShared,
    locationPermissionState: PermissionStateShared,
    fusedLocationProviderClient: FusedLocationProviderClientShared,
    context: ContextShared
) {
    when (locationPermissionState.status) {
        PermissionStatus.Granted -> {
            CoroutineScope(Dispatchers.Main).launch {
                enableMyLocation(map, fusedLocationProviderClient, context)
            }
        }

        is PermissionStatus.Denied -> {
            locationPermissionState.launchPermissionRequest()
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun enableMyLocation(
    map: GoogleMapShared,
    fusedLocationClient: FusedLocationProviderClientShared,
    context: ContextShared
) {
    if (ContextCompat.checkSelfPermission(
            context.context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        getLastKnownLocation(fusedLocationClient, context)?.let { location: LocationShared ->
            map.isMyLocationEnabled = true
            val latLng = LatLng(location.latitude, location.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
        }
    }
}

actual suspend fun findMyLocation(
    map: GoogleMapShared,
    fusedLocationClient: FusedLocationProviderClientShared,
    context: ContextShared
) {
    getLastKnownLocation(fusedLocationClient, context)?.let { location: Location ->
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
actual suspend fun getLastKnownLocation(
    fusedLocationClient: FusedLocationProviderClientShared,
    context: ContextShared
): LocationShared? =
    if (ContextCompat.checkSelfPermission(
            context.context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        suspendCancellableCoroutine { continuation: CancellableContinuation<Location?> ->
            fusedLocationClient.lastLocation
                .addOnCompleteListener { locationTask: Task<Location> ->
                    continuation.resume(locationTask.result, null)
                }
        }
    } else null
