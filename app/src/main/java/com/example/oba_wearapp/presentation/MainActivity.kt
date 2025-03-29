/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.oba_wearapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.oba_wearapp.data.BusStation
import com.example.oba_wearapp.presentation.theme.Oba_wearappTheme

// Define screens to navigate between
enum class AppScreen {
    MAP,
    BUS_STATIONS,
    BUS_ARRIVALS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    // State to track the current screen
    var currentScreen by remember { mutableStateOf(AppScreen.MAP) }
    
    // State to store the previous screen
    var previousScreen by remember { mutableStateOf(AppScreen.MAP) }
    
    // State to store the selected bus station
    var selectedStation by remember { mutableStateOf<BusStation?>(null) }
    
    Oba_wearappTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {

            when (currentScreen) {
                AppScreen.MAP -> MapScreen(
                    context = context, 
                    onNavigateToBusStations = { 
                        previousScreen = AppScreen.MAP
                        currentScreen = AppScreen.BUS_STATIONS 
                    },
                    onBusStationSelected = { station ->
                        previousScreen = AppScreen.MAP
                        selectedStation = station
                        currentScreen = AppScreen.BUS_ARRIVALS
                    }
                )
                AppScreen.BUS_STATIONS -> BusStationScreen(
                    onBackClick = { currentScreen = AppScreen.MAP },
                    onStationClick = { station ->
                        previousScreen = AppScreen.BUS_STATIONS
                        selectedStation = station
                        currentScreen = AppScreen.BUS_ARRIVALS
                    }
                )
                AppScreen.BUS_ARRIVALS -> {
                    selectedStation?.let { station ->
                        BusArrivalScreen(
                            station = station,
                            onBackClick = { 
                                // Go back to the previous screen
                                currentScreen = 
                                    if (previousScreen == AppScreen.BUS_STATIONS) 
                                        AppScreen.BUS_STATIONS 
                                    else 
                                        AppScreen.MAP
                            }
                        )
                    } ?: run {
                        currentScreen = AppScreen.MAP
                    }
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}