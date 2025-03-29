package com.example.oba_wearapp.presentation

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.oba_wearapp.data.BusStation
import com.example.oba_wearapp.data.BusStationFetcher
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

@Composable
fun MapScreen(
    context: Context,
    onNavigateToBusStations: () -> Unit,
    onBusStationSelected: (BusStation) -> Unit
) {
    var hasLocationPermission by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Seattle, Washington coordinates
    val seattleLocation = LatLng(47.6062, -122.3321)
    
    // State to track if the user is in Seattle area
    var isInSeattleArea by remember { mutableStateOf(false) }
    
    // State to track if the relocation dialog is shown
    var showRelocationDialog by remember { mutableStateOf(false) }
    
    // State to control the map camera position
    var mapCenter by remember { mutableStateOf<LatLng?>(null) }
    
    // Remember camera position state outside the when block so we can update it
    val cameraPositionState = rememberCameraPositionState()
    
    // State to hold the bus stations
    var busStations by remember { mutableStateOf<List<BusStation>>(emptyList()) }
    
    // Create the fetcher
    val fetcher = remember { BusStationFetcher() }
    
    val scope = rememberCoroutineScope()
    
    // Function to check if a location is in Seattle area (within ~30km)
    fun isLocationInSeattleArea(location: LatLng): Boolean {
        return abs(location.latitude - seattleLocation.latitude) < 0.3 &&
               abs(location.longitude - seattleLocation.longitude) < 0.3
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            scope.launch {
                getLocation(context) { location, error ->
                    currentLocation = location
                    
                    if (location != null) {
                        isInSeattleArea = isLocationInSeattleArea(location)
                        
                        mapCenter = location
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 14f)
                        
                        if (!isInSeattleArea) {
                            showRelocationDialog = true
                        }
                    }
                    
                    errorMessage = error
                    isLoading = false
                }
            }
        } else {
            errorMessage = "Location permission denied"
            isLoading = false
            
            // Default to Seattle if location permission is denied
            mapCenter = seattleLocation
            cameraPositionState.position = CameraPosition.fromLatLngZoom(seattleLocation, 14f)
        }
    }

    // Function to load bus stations
    suspend fun loadBusStations() {
        try {
            val stations = fetcher.fetchBusStations()
            busStations = stations
        } catch (e: Exception) {
            println("Error loading bus stations: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        loadBusStations()
        
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true
                getLocation(context) { location, error ->
                    currentLocation = location
                    
                    if (location != null) {
                        isInSeattleArea = isLocationInSeattleArea(location)
                        
                        mapCenter = location
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 14f)
                        
                        if (!isInSeattleArea) {
                            showRelocationDialog = true
                        }
                    } else {
                        mapCenter = seattleLocation
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(seattleLocation, 14f)
                    }
                    
                    errorMessage = error
                    isLoading = false
                }
            }
            else -> {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    fun relocateToSeattle() {
        mapCenter = seattleLocation
        cameraPositionState.position = CameraPosition.fromLatLngZoom(seattleLocation, 14f)
        showRelocationDialog = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading map...",
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            mapCenter != null -> {
                LaunchedEffect(mapCenter) {
                    mapCenter?.let {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 14f)
                    }
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission,
                        mapType = MapType.NORMAL
                    )
                ) {
                    currentLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Your Location",
                            snippet = "Current position"
                        )
                    }
                    
                    if (mapCenter == seattleLocation ||
                        (currentLocation != null && !isInSeattleArea)) {
                        Marker(
                            state = MarkerState(position = seattleLocation),
                            title = "Seattle",
                            snippet = "Center of Seattle"
                        )
                    }
                    
                    if (mapCenter == seattleLocation || isInSeattleArea) {
                        busStations.forEach { station ->
                            Marker(
                                state = MarkerState(position = LatLng(station.lat, station.lon)),
                                title = station.name,
                                snippet = "Stop ID: ${station.id}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                                onClick = {
                                    onBusStationSelected(station)
                                    true
                                }
                            )
                        }
                    }
                }
                
                if (currentLocation != null && !isInSeattleArea && mapCenter != seattleLocation) {
                    Text(
                        text = "You're not in Seattle.\nBus stations only visible in Seattle.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            .padding(8.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = errorMessage ?: "Unable to show map",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
        
        if (showRelocationDialog) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    onClick = { /* Prevent clicks passing through */ },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Not in Seattle",
                            style = MaterialTheme.typography.title3,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "View Seattle area?",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = { showRelocationDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("No", style = MaterialTheme.typography.button)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { relocateToSeattle() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Yes", style = MaterialTheme.typography.button)
                            }
                        }
                    }
                }
            }
        }
        
        // Show Bus Stations button
        //This is for debugging the api response
//        Button(
//            onClick = onNavigateToBusStations,
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(bottom = 16.dp)
//        ) {
//            Text("Bus Stations (${busStations.size})")
//        }
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private suspend fun getLocation(
    context: Context,
    callback: (LatLng?, String?) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        val lastLocation = fusedLocationClient.lastLocation.await()

        if (lastLocation != null) {
            callback(LatLng(lastLocation.latitude, lastLocation.longitude), null)
            return
        }

        val cancellationTokenSource = CancellationTokenSource()
        val location = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).await()

        if (location != null) {
            callback(LatLng(location.latitude, location.longitude), null)
        } else {
            callback(null, "Location not available. Please enable location services.")
        }
    } catch (e: Exception) {
        callback(null, "Error getting location: ${e.message}")
    }
}