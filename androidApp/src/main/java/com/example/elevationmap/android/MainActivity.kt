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
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MapScreen(fusedLocationClient)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(fusedLocationClient: FusedLocationProviderClient) {
    val mapView = rememberMapViewWithLifecycle()
    val googleMapState = remember { mutableStateOf<GoogleMap?>(null) }
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    val mapUiSettings = remember {
        mutableStateOf(MapUiSettings())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapViewContainer(
            mapView = mapView,
            googleMapState = googleMapState,
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
            Text(findMe)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapViewContainer(
    mapView: MapView,
    googleMapState: MutableState<GoogleMap?>,
    fusedLocationClient: FusedLocationProviderClient,
    locationPermissionState: PermissionState,
    mapUiSettings: MutableState<MapUiSettings>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AndroidView({ mapView }) { mapViewLocal: MapView ->
        coroutineScope.launch {
            initializeMap(mapViewLocal, googleMapState, locationPermissionState, fusedLocationClient, context)
        }
    }

    LaunchedEffect(mapUiSettings.value.isFindMeButtonClicked) {
        if (mapUiSettings.value.isFindMeButtonClicked) {
            googleMapState.value?.let { googleMap: GoogleMap ->
                findMyLocation(googleMap, fusedLocationClient, context)
                mapUiSettings.value = mapUiSettings.value.copy(isFindMeButtonClicked = false)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun initializeMap(
    mapView: MapView,
    googleMapState: MutableState<GoogleMap?>,
    locationPermissionState: PermissionState,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    if (googleMapState.value == null) {
        mapView.getMapAsync { googleMap: GoogleMap ->
            googleMapState.value = googleMap
            setupMap(googleMap, locationPermissionState, fusedLocationClient, context)
        }
    } else {
        googleMapState.value?.let { googleMap: GoogleMap ->
            setupMap(googleMap, locationPermissionState, fusedLocationClient, context)
        }
    }
}

private suspend fun enableMyLocation(
    map: GoogleMap,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    if (ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        getLastKnownLocation(fusedLocationClient, context)?.let { location: Location ->
            map.isMyLocationEnabled = true
            val latLng = LatLng(location.latitude, location.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun setupMap(
    map: GoogleMap,
    locationPermissionState: PermissionState,
    fusedLocationProviderClient: FusedLocationProviderClient,
    context: Context
) {
    setupMapUI(map)
    handleLocationPermission(map, locationPermissionState, fusedLocationProviderClient, context)
}

private fun setupMapUI(map: GoogleMap) {
    map.uiSettings.isZoomControlsEnabled = true
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(moscowLatLng, zoomRate))
    map.mapType = GoogleMap.MAP_TYPE_TERRAIN
}

@OptIn(ExperimentalPermissionsApi::class)
private fun handleLocationPermission(
    map: GoogleMap,
    locationPermissionState: PermissionState,
    fusedLocationProviderClient: FusedLocationProviderClient,
    context: Context
) {
    when (locationPermissionState.status) {
        PermissionStatus.Granted ->
            CoroutineScope(Dispatchers.Main).launch {
                enableMyLocation(map, fusedLocationProviderClient, context)
            }

        is PermissionStatus.Denied ->
            locationPermissionState.launchPermissionRequest()
    }
}

private suspend fun findMyLocation(
    map: GoogleMap,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    getLastKnownLocation(fusedLocationClient, context)?.let { location: Location ->
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomRate))
    }
}


@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun getLastKnownLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
): Location? =
    if (ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        suspendCancellableCoroutine { continuation: CancellableContinuation<Location?> ->
            fusedLocationClient.lastLocation
                .addOnCompleteListener { locationTask: Task<Location> ->
                    continuation.resume(locationTask.result, null)
                }
        }
    } else null

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView: MapView = remember { MapView(context) }

    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycle.removeObserver(lifecycleObserver) }
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

const val zoomRate = 15f
const val findMe = "Найти меня"
val moscowLatLng = LatLng(55.7558, 37.6176)

data class MapUiSettings(
    val isFindMeButtonClicked: Boolean = false
)
