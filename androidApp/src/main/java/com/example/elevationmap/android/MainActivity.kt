package com.example.elevationmap.android

import android.content.Context
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
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import com.example.elevationmap.Common.MapUiSettings
import com.example.elevationmap.Common.FIND_ME
import com.example.elevationmap.Common.ZOOM_RATE
import com.example.elevationmap.ContextShared
import com.example.elevationmap.PermissionStateShared
import com.example.elevationmap.findMyLocation
import com.example.elevationmap.getLastKnownLocation
import com.example.elevationmap.handleLocationPermission
import com.example.elevationmap.setupMapUI
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MapScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
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
            Text(FIND_ME)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapViewContainer(
    mapView: MapView,
    googleMapState: MutableState<GoogleMap?>,
    locationPermissionState: PermissionState,
    mapUiSettings: MutableState<MapUiSettings>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AndroidView({ mapView }) { mapViewLocal: MapView ->
        coroutineScope.launch {
            initializeMap(mapViewLocal, googleMapState, locationPermissionState, context)
        }
    }

    LaunchedEffect(mapUiSettings.value.isFindMeButtonClicked) {
        if (mapUiSettings.value.isFindMeButtonClicked) {
            googleMapState.value?.let { googleMap: GoogleMap ->
                findMyLocation(
                    googleMap,
                    ContextSharedAdapter(context)
                )
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
    context: Context
) {
    if (googleMapState.value == null) {
        mapView.getMapAsync { googleMap: GoogleMap ->
            googleMapState.value = googleMap
            setupMap(googleMap, locationPermissionState, context)
        }
    } else {
        googleMapState.value?.let { googleMap: GoogleMap ->
            setupMap(googleMap, locationPermissionState, context)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun setupMap(
    map: GoogleMap,
    locationPermissionState: PermissionState,
    context: Context
) {
    setupMapUI(map)
    handleLocationPermission(
        map,
        PermissionStateSharedAdapter(locationPermissionState),
        ContextSharedAdapter(context)
    )

    map.setOnMyLocationButtonClickListener {
        CoroutineScope(Dispatchers.Main).launch {
            getLastKnownLocation(ContextSharedAdapter(context))?.let { location: Location ->
                val latLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_RATE))
            }
        }
        true
    }
}

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

@OptIn(ExperimentalPermissionsApi::class)
class PermissionStateSharedAdapter(
    private val permissionState: PermissionState
) : PermissionStateShared, PermissionState by permissionState

class ContextSharedAdapter(override val context: Context) : ContextShared

