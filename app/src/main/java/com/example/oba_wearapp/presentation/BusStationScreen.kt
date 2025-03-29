package com.example.oba_wearapp.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Card
import com.example.oba_wearapp.data.BusStation
import com.example.oba_wearapp.data.BusStationFetcher
import kotlinx.coroutines.delay

/**
 * Screen to display bus stations
 */
@Composable
fun BusStationScreen(
    onBackClick: () -> Unit,
    onStationClick: (BusStation) -> Unit
) {
    // State to hold the bus stations
    var stations by remember { mutableStateOf<List<BusStation>>(emptyList()) }
    
    // State to track loading
    var isLoading by remember { mutableStateOf(true) }
    
    // State to track errors
    var error by remember { mutableStateOf<String?>(null) }
    
    // State to trigger retries
    var retryTrigger by remember { mutableStateOf(0) }
    
    // State to track if this is an initial load or a retry
    var isRetry by remember { mutableStateOf(false) }
    
    // Create the fetcher
    val fetcher = remember { BusStationFetcher() }
    
    // Fetch the bus stations when the composable is first created or retry is triggered
    LaunchedEffect(key1 = retryTrigger) {
        try {
            isLoading = true
            error = null
            
            // Add a delay before retrying to help avoid rate limiting
            if (isRetry) {
                delay(3000) // Wait 3 seconds before retry
            }
            
            // The radius parameter doesn't matter anymore since we're using hardcoded IDs
            val fetchedStations = fetcher.fetchBusStations()
            stations = fetchedStations
            isLoading = false
            
            // First load is complete, next will be a retry
            isRetry = true
        } catch (e: Exception) {
            // Special handling for rate limit errors
            if (e.message?.contains("429") == true) {
                error = "Rate limit exceeded. Please try again after a few seconds."
            } else {
                error = "Failed to load stations: ${e.message}"
            }
            isLoading = false
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator()
                    
                    if (isRetry) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Waiting to avoid rate limiting...",
                            style = MaterialTheme.typography.caption2,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading Seattle bus stations...",
                            style = MaterialTheme.typography.caption2,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Add a retry button for rate limit errors
                    if (error?.contains("Rate limit") == true) {
                        Button(
                            onClick = { 
                                // Increment the retry trigger to cause the LaunchedEffect to run again
                                retryTrigger += 1
                            }
                        ) {
                            Text("Retry")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
            stations.isEmpty() -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "No stations found",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
            else -> {
                StationsList(
                    stations = stations, 
                    onBackClick = onBackClick,
                    onStationClick = onStationClick
                )
            }
        }
    }
}

@Composable
private fun StationsList(
    stations: List<BusStation>,
    onBackClick: () -> Unit,
    onStationClick: (BusStation) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Seattle Bus Stations",
                style = MaterialTheme.typography.title1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            )
        }
        
        item {
            Text(
                text = "(Using TEST API key)",
                style = MaterialTheme.typography.caption2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        items(stations) { station ->
            StationItem(
                station = station,
                onClick = { onStationClick(station) }
            )
        }
        
        item {
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun StationItem(
    station: BusStation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.body1
            )
            
            if (station.code != null) {
                Text(
                    text = "Stop #${station.code}",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            if (station.direction != null) {
                Text(
                    text = "Direction: ${station.direction}",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Text(
                text = "ID: ${station.id}",
                style = MaterialTheme.typography.caption2,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Text(
                text = "Location: ${station.lat}, ${station.lon}",
                style = MaterialTheme.typography.caption2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
} 