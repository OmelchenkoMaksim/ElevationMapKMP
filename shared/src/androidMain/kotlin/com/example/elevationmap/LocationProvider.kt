package com.example.elevationmap

//import android.content.Context
//import android.location.Location
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//import com.google.maps.model.LatLng
//
//actual class LocationProvider(context: Context) {
//    private val fusedLocationClient: FusedLocationProviderClient =
//        LocationServices.getFusedLocationProviderClient(context)
//
//    actual suspend fun getLastKnownLocation(): Location? {
//        // Реализация, использующая FusedLocationProviderClient
//    }
//
//    actual suspend fun isPermissionGranted(): Boolean {
//        // Проверка разрешений
//    }
//}
//
//actual fun provideMapUiSettings(): MapUiSettings = MapUiSettings()
//
//actual val defaultLocation = LatLng(55.7558, 37.6176)
//
