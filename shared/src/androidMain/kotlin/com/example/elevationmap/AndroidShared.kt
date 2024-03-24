@file:Suppress("actual_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(ExperimentalPermissionsApi::class)

package com.example.elevationmap

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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

actual typealias LocationShared = Location
actual typealias GoogleMapShared = GoogleMap

actual fun GoogleMapShared.setCenter(location: LocationShared, animated: Boolean) {
    val latLng = LatLng(location.latitude, location.longitude)
    if (animated) {
        this.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
    } else {
        this.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
    }
}

actual fun setupMapUI(map: GoogleMapShared) {
    map.uiSettings.isZoomControlsEnabled = true
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(moscowLatLng, zoomRate))
    map.mapType = GoogleMap.MAP_TYPE_TERRAIN
}

@OptIn(ExperimentalPermissionsApi::class)
actual fun handleLocationPermission(
    map: GoogleMapShared,
    locationPermissionState: PermissionStateShared,
    context: ContextShared
) {
    when (locationPermissionState.status) {
        PermissionStatus.Granted -> {
            CoroutineScope(Dispatchers.Main).launch {
                enableMyLocation(map, context)
            }
        }

        is PermissionStatus.Denied -> {
            locationPermissionState.launchPermissionRequest()
        }
    }
}

@SuppressLint("MissingPermission")
private fun enableMyLocation(
    map: GoogleMapShared,
    context: ContextShared
) {
    if (ContextCompat.checkSelfPermission(
            context.context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        map.isMyLocationEnabled = true
    }
}

actual suspend fun findMyLocation(
    map: GoogleMapShared,
    context: ContextShared
) {
    getLastKnownLocation(context)?.let { location: Location ->
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
actual suspend fun getLastKnownLocation(
    context: ContextShared
): LocationShared? {
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.context)
    return if (ContextCompat.checkSelfPermission(
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
}
