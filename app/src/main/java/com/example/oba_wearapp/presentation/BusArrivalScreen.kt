package com.example.oba_wearapp.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.oba_wearapp.data.BusArrival
import com.example.oba_wearapp.data.BusStation
import com.example.oba_wearapp.data.BusStationFetcher
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.compareTo
import kotlin.text.format

/**
 * Screen to display bus arrival information for a specific station
 */
@Composable
fun BusArrivalScreen(
    station: BusStation,
    onBackClick: () -> Unit
) {
    // State to hold the bus arrivals
    var arrivals by remember { mutableStateOf<List<BusArrival>>(emptyList()) }
    
    // State to track loading
    var isLoading by remember { mutableStateOf(true) }
    
    // State to track errors
    var error by remember { mutableStateOf<String?>(null) }
    
    // State to trigger retries
    var retryTrigger by remember { mutableStateOf(0) }
    
    // Create the fetcher
    val fetcher = remember { BusStationFetcher() }
    
    LaunchedEffect(key1 = retryTrigger) {
        try {
            isLoading = true
            error = null
            
            if (retryTrigger > 0) {
                delay(3000)
            }
            
            val fetchedArrivals = fetcher.fetchBusArrivals(station.id)
            arrivals = fetchedArrivals
            isLoading = false
        } catch (e: Exception) {
            if (e.message?.contains("429") == true) {
                error = "Rate limit exceeded. Please try again after a few seconds."
            } else {
                error = "Failed to load arrivals: ${e.message}"
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.caption1,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0x33FF0000),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.caption2
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Button(
                            onClick = { retryTrigger++ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Retry", 
                                style = MaterialTheme.typography.button
                            )
                        }
                        
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        ) {
                            Text(
                                text = "Back", 
                                style = MaterialTheme.typography.button
                            )
                        }
                    }
                }
            }
            arrivals.isEmpty() -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0x33808080),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "No arrivals found",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.caption1
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Button(
                            onClick = { retryTrigger++ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Refresh", 
                                style = MaterialTheme.typography.button
                            )
                        }
                        
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        ) {
                            Text(
                                text = "Back", 
                                style = MaterialTheme.typography.button
                            )
                        }
                    }
                }
            }
            else -> {
                ArrivalsListView(
                    station = station,
                    arrivals = arrivals,
                    onRefresh = { retryTrigger++ },
                    onBackClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun ArrivalsListView(
    station: BusStation,
    arrivals: List<BusArrival>,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedArrival by remember { mutableStateOf<BusArrival?>(null) }
    
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center
                )
                
                if (station.code != null) {
                    Text(
                        text = "Stop #${station.code}",
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                if (station.direction != null) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "→ ${station.direction}",
                            style = MaterialTheme.typography.caption1,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        
        items(arrivals) { arrival ->
            ArrivalItem(
                arrival = arrival,
                onClick = { selectedArrival = arrival }
            )
        }
        
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh", style = MaterialTheme.typography.button)
                }
                
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                ) {
                    Text("Back", style = MaterialTheme.typography.button)
                }
            }
        }
    }
    
    if (selectedArrival != null) {
        ArrivalDetailDialog(
            arrival = selectedArrival!!,
            onDismiss = { selectedArrival = null }
        )
    }
}

@Composable
private fun ArrivalItem(
    arrival: BusArrival,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFF0D47A1),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = arrival.routeName,
                    style = MaterialTheme.typography.caption1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = "→ ${arrival.tripHeadsign}",
                    style = MaterialTheme.typography.caption2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
            
            val minutes = arrival.getMinutesUntilArrival()
            val arrivalColor = when {
                minutes <= 0 -> Color.Red
                minutes <= 5 -> Color(0xFFFF9800)
                else -> Color(0xFF4CAF50)
            }
            
            val timeText = when {
                minutes <= 0 -> "NOW"
                else -> "$minutes min"
            }
            
            Text(
                text = timeText,
                style = MaterialTheme.typography.caption1,
                color = arrivalColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ArrivalDetailDialog(
    arrival: BusArrival,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(
                    color = Color(0xFF1D2733),
                    shape = MaterialTheme.shapes.large
                )
                .clickable { /* Intercept clicks to prevent dismissal */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF3D7BF4),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = arrival.routeName,
                            style = MaterialTheme.typography.title3,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "ID: ${arrival.routeId}",
                        style = MaterialTheme.typography.caption1,
                        color = Color.LightGray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    text = "→ ${arrival.tripHeadsign}",
                    style = MaterialTheme.typography.title2,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                val minutes = arrival.getMinutesUntilArrival()
                val arrivalColor = when {
                    minutes <= 0 -> Color(0xFFFF5252)
                    minutes <= 5 -> Color(0xFFFFB74D)
                    else -> Color(0xFF81C784)
                }

                Text(
                    text = when {
                        minutes <= 0 -> "Arriving now"
                        else -> "Arriving in $minutes minutes"
                    },
                    style = MaterialTheme.typography.title3,
                    color = arrivalColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (arrival.predictedArrivalTime != null) {
                    val predictedTime = SimpleDateFormat("h:mm a", Locale.US)
                        .format(Date(arrival.predictedArrivalTime))

                    Text(
                        text = "Predicted: $predictedTime",
                        style = MaterialTheme.typography.body1,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    val scheduledTime = SimpleDateFormat("h:mm a", Locale.US)
                        .format(Date(arrival.scheduledArrivalTime))

                    Text(
                        text = "Scheduled: $scheduledTime",
                        style = MaterialTheme.typography.body1,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                arrival.distanceFromStop?.let { distance ->
                    val formattedDistance = String.format("%.1f", distance)
                    Text(
                        text = "Bus is $formattedDistance meters away",
                        style = MaterialTheme.typography.body2,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Spacer(modifier = Modifier.fillMaxHeight(0.05f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF263238),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Route Details",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Agency: Puget Sound Transit",
                            style = MaterialTheme.typography.body2,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Text(
                            text = "Route Type: Bus",
                            style = MaterialTheme.typography.body2,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (arrival.predictedArrivalTime != null) {
                            Text(
                                text = "Status: Real-time data available",
                                style = MaterialTheme.typography.body2,
                                color = Color(0xFF81C784),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.fillMaxHeight(0.05f))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    colors = androidx.wear.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3D7BF4)
                    )
                ) {
                    Text(
                        "Close",
                        style = MaterialTheme.typography.button,
                        color = Color.White
                    )
                }
            }
        }
    }
}