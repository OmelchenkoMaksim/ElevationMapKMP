@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.example.elevationmap

import com.example.elevationmap.Common.PermissionStatus
import com.example.elevationmap.Common.PermissionStatus.Denied
import com.example.elevationmap.Common.PermissionStatus.Granted
import com.example.elevationmap.Common.PermissionStatus.Restricted
import com.example.elevationmap.Common.PermissionStatus.Unknown
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpan
import platform.MapKit.MKMapView
import platform.MapKit.MKUserTrackingModeFollow
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual typealias LocationShared = CLLocation

actual interface ContextShared

actual class GoogleMapShared(val mapView: MKMapView)

actual interface PermissionStateShared {
    val status: PermissionStatus
    fun requestPermission()
}

@OptIn(ExperimentalForeignApi::class)
fun GoogleMapShared.setCenter(location: LocationShared, animated: Boolean) {
    val coordinate =
        cValue<CLLocationCoordinate2D> {
            this.latitude = 55.7558
            this.longitude = 37.6176
        }

    val span = cValue<MKCoordinateSpan> {
        this.latitudeDelta = 0.01
        this.longitudeDelta = 0.01
    }

    val region = MKCoordinateRegionMake(coordinate, span)
    mapView.setRegion(region, animated)
}

class PermissionStateSharedImpl(private val locationManager: CLLocationManager) : PermissionStateShared {
    override val status: PermissionStatus
        get() =
            when (CLLocationManager.authorizationStatus()) {
                kCLAuthorizationStatusNotDetermined -> Unknown
                kCLAuthorizationStatusRestricted -> Restricted
                kCLAuthorizationStatusDenied -> Denied
                kCLAuthorizationStatusAuthorizedAlways,
                kCLAuthorizationStatusAuthorizedWhenInUse -> Granted

                else -> Unknown
            }

    override fun requestPermission() {
        locationManager.requestWhenInUseAuthorization()
    }
}

actual fun setupMapUI(map: GoogleMapShared) {
    map.mapView.showsUserLocation = true
    map.mapView.userTrackingMode = MKUserTrackingModeFollow
}

actual fun handleLocationPermission(
    map: GoogleMapShared,
    locationPermissionState: PermissionStateShared,
    context: ContextShared
) {
    when (locationPermissionState.status) {
        Granted -> map.mapView.showsUserLocation = true

        Denied, Restricted -> Unit

        Unknown -> locationPermissionState.requestPermission()
    }
}

actual suspend fun findMyLocation(
    map: GoogleMapShared,
    context: ContextShared
) {
    getLastKnownLocation(context)?.let { location: LocationShared ->
        map.setCenter(location, animated = true)
    }
}

actual suspend fun getLastKnownLocation(
    context: ContextShared
): LocationShared? =
    suspendCancellableCoroutine { continuation: CancellableContinuation<LocationShared?> ->
        val locationManager = CLLocationManager()
        locationManager.delegate =
            object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager, didUpdateLocations: List<*>
                ) {
                    val lastLocation =
                        didUpdateLocations.lastOrNull() as? CLLocation
                    continuation.resume(lastLocation)
                    manager.delegate = null
                }

                override fun locationManager(
                    manager: CLLocationManager, didFailWithError: NSError
                ) {
                    continuation.resume(null)
                    manager.delegate = null
                }
            }
        locationManager.requestLocation()
    }
