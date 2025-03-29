package com.example.oba_wearapp.data

import kotlinx.coroutines.runBlocking

/**
 * A simple utility to test fetching bus stations without running the full app
 */
object FetchBusStationsTest {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Fetching bus stations near Washington DC...")
        
        // Use a larger radius to increase chances of finding stations
        val radius = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 1000 else 1000
        
        // Create the fetcher
        val fetcher = BusStationFetcher()
        
        // Run the coroutine in a blocking manner for this simple test
        runBlocking {
            val stations = fetcher.fetchBusStations(radius)
            
            if (stations.isEmpty()) {
                println("\nNo bus stations found. Try increasing the radius or check the API connection.")
                return@runBlocking
            }
            
            println("\n--- Bus Stations near Washington DC ---")
            stations.take(10).forEachIndexed { index, station ->
                println("${index + 1}. ${station.name}")
                println("   ID: ${station.id}")
                println("   Code: ${station.code ?: "N/A"}")
                println("   Location: ${station.lat}, ${station.lon}")
                println("   Direction: ${station.direction ?: "N/A"}")
                println()
            }
            println("Total stations found: ${stations.size}")
            println("---------------------------------------")
        }
    }
} 