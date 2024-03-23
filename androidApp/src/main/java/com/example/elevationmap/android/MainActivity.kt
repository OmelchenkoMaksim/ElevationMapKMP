@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class)

package com.example.elevationmap.android

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen(fusedLocationClient)
                }
            }
        }
    }
}

@Composable
fun MapScreen(fusedLocationClient: FusedLocationProviderClient) {
    val mapView = rememberMapViewWithLifecycle()
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    val mapUiSettings = remember {
        mutableStateOf(MapUiSettings())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapViewContainer(
            mapView = mapView,
            fusedLocationClient = fusedLocationClient,
            locationPermissionState = locationPermissionState,
            mapUiSettings = mapUiSettings
        )
        Button(
            onClick = {
                mapUiSettings.value = mapUiSettings.value.copy(
                    isFindMeButtonClicked = true
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Найти меня")
        }
    }
}

@Composable
private fun MapViewContainer(
    mapView: MapView,
    fusedLocationClient: FusedLocationProviderClient,
    locationPermissionState: PermissionState,
    mapUiSettings: MutableState<MapUiSettings>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AndroidView({ mapView }) { mapViewLocal: MapView ->
        coroutineScope.launch {
            val googleMap = CompletableDeferred<GoogleMap>()
            mapViewLocal.getMapAsync { googleMap.complete(it) }
            val map = googleMap.await()
            map.uiSettings.isZoomControlsEnabled = true

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(moscowLatLng, zoomRate))

            when (locationPermissionState.status) {
                PermissionStatus.Granted -> enableMyLocation(map, fusedLocationClient, context)
                is PermissionStatus.Denied -> locationPermissionState.launchPermissionRequest()
            }
        }
    }

    LaunchedEffect(mapUiSettings.value.isFindMeButtonClicked) {
        if (mapUiSettings.value.isFindMeButtonClicked) {
            coroutineScope.launch {
                val googleMap = CompletableDeferred<GoogleMap>()
                mapView.getMapAsync { googleMap.complete(it) }
                val map = googleMap.await()
                findMyLocation(map, fusedLocationClient, context)
                mapUiSettings.value = mapUiSettings.value.copy(
                    isFindMeButtonClicked = false
                )
            }
        }
    }
}

private suspend fun enableMyLocation(
    map: GoogleMap,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        getLastKnownLocation(fusedLocationClient, context)?.let { location ->
            map.isMyLocationEnabled = true
            val latLng = LatLng(location.latitude, location.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
        }
    }
}


private suspend fun findMyLocation(
    map: GoogleMap,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    getLastKnownLocation(fusedLocationClient, context)?.let { location ->
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
    }
}


private suspend fun getLastKnownLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
): Location? =
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                continuation.resume(task.result, null)
            }
        }
    } else null


@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context)
    }

    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                ON_CREATE -> mapView.onCreate(Bundle())
                ON_START -> mapView.onStart()
                ON_RESUME -> mapView.onResume()
                ON_PAUSE -> mapView.onPause()
                ON_STOP -> mapView.onStop()
                ON_DESTROY -> mapView.onDestroy()
                else -> throw IllegalStateException()
            }
        }
    }

data class MapUiSettings(
    val isFindMeButtonClicked: Boolean = false
)

const val zoomRate = 15f
val moscowLatLng = LatLng(55.7558, 37.6176)