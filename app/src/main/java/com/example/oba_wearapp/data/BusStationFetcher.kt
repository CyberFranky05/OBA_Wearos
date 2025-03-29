package com.example.oba_wearapp.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Data class representing a bus station
 */
data class BusStation(
    val id: String,
    val name: String,
    val code: String?,
    val lat: Double,
    val lon: Double,
    val direction: String?
) {
    override fun toString(): String {
        return "$name (Code: ${code ?: "N/A"}, Direction: ${direction ?: "N/A"})"
    }
}

/**
 * Data class representing a bus arrival
 */
data class BusArrival(
    val routeId: String,
    val routeName: String,
    val tripHeadsign: String,
    val predictedArrivalTime: Long?,
    val scheduledArrivalTime: Long,
    val distanceFromStop: Double?
) {
    // Calculate minutes until arrival
    fun getMinutesUntilArrival(): Int {
        val arrivalTime = predictedArrivalTime ?: scheduledArrivalTime
        val currentTime = System.currentTimeMillis()
        return ((arrivalTime - currentTime) / 60000).toInt()
    }
    
    override fun toString(): String {
        val minutes = getMinutesUntilArrival()
        val timeDescription = if (minutes <= 0) "Due now" else "$minutes min"
        return "$routeName - $tripHeadsign: $timeDescription"
    }
}

/**
 * Utility class to fetch bus station information from the OneBusAway API
 */
class BusStationFetcher {

    companion object {
        // Washington DC coordinates (approximate center)
        private const val DC_LAT = 38.9072
        private const val DC_LON = -77.0369
        

        private const val BASE_URL = "https://api.pugetsound.onebusaway.org/api/where"
        private const val API_KEY = "TEST"
        
        // Try to fech 3 for testing
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_DELAY_MS = 1000L // 1 second
        
        // Hardcoded list of known Puget Sound stop IDs we can use for testing
        private val TEST_STOPS = listOf(
            "1_10914", // 15th Ave NE & NE Campus Pkwy
            "1_11160", // 15th Ave NE & NE 55th St
            "1_11370", // 15th Ave NE & NE 45th St
            "1_10346", // University Way NE & NE 50th St
            "1_10380"  // University Way NE & NE 45th St
        )
    }

    private val gson = Gson()
    
    private var cachedStations: List<BusStation>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_VALIDITY_MS = TimeUnit.MINUTES.toMillis(5)
    /**
     * Fetches a specific set of hardcoded bus stations
     * This approach avoids the stops-for-location API which has rate limits
     * @return List of BusStation objects
     */
    suspend fun fetchBusStations(radius: Int = 500): List<BusStation> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if we have valid cached data
                val currentTime = System.currentTimeMillis()
                if (cachedStations != null && (currentTime - cacheTimestamp) < CACHE_VALIDITY_MS) {
                    println("Using cached bus stations data")
                    return@withContext cachedStations!!
                }
                
                val stations = mutableListOf<BusStation>()
                
                for (stopId in TEST_STOPS) {
                    try {
                        val stationUrl = "$BASE_URL/stop/$stopId.json?key=$API_KEY"
                        val response = makeApiRequestWithRetry(stationUrl)
                        val station = parseStationFromStopResponse(response)
                        station?.let { stations.add(it) }
                        
                        // Add a small delay between requests
                        delay(1000)
                    } catch (e: Exception) {
                        println("Error fetching station $stopId: ${e.message}")
                        // Continue with the next station
                    }
                }
                
                cachedStations = stations
                cacheTimestamp = currentTime
                
                stations
            } catch (e: Exception) {
                println("Error fetching bus stations: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Makes an API request with retry logic and exponential backoff
     */
    private suspend fun makeApiRequestWithRetry(urlString: String, retryCount: Int = 0): String {
        try {
            return makeApiRequest(urlString)
        } catch (e: Exception) {
            // If we get a 429 (Too Many Requests) or a 5xx server error, retry with backoff
            if (e.message?.contains("429") == true || e.message?.contains("5") == true) {
                if (retryCount < MAX_RETRIES) {
                    val backoffDelay = INITIAL_BACKOFF_DELAY_MS * (1 shl retryCount)
                    println("Rate limited (${e.message}). Retrying in ${backoffDelay}ms...")
                    delay(backoffDelay)
                    return makeApiRequestWithRetry(urlString, retryCount + 1)
                }
            }
            throw e
        }
    }
    
    /**
     * Makes a simple HTTP request to the API
     */
    private fun makeApiRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parses a single station from a stop response
     */
    private fun parseStationFromStopResponse(jsonResponse: String): BusStation? {
        try {
            // Parse the JSON response
            val jsonObject = gson.fromJson(jsonResponse, JsonObject::class.java)
            
            val code = jsonObject.get("code").asInt
            if (code != 200) {
                println("API returned error code: $code")
                return null
            }
            
            val data = jsonObject.getAsJsonObject("data")
            val entry = data.getAsJsonObject("entry")
            
            val id = entry.get("id").asString
            val name = entry.get("name").asString
            val stopCode = if (entry.has("code")) entry.get("code").asString else null
            val lat = entry.get("lat").asDouble
            val lon = entry.get("lon").asDouble
            val direction = if (entry.has("direction")) entry.get("direction").asString else null
            
            return BusStation(id, name, stopCode, lat, lon, direction)
            
        } catch (e: Exception) {
            println("Error parsing single station JSON: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parses the JSON response to extract station information using Gson
     * This is used for the stops-for-location API
     */
    private fun parseStationResponse(jsonResponse: String): List<BusStation> {
        val stations = mutableListOf<BusStation>()
        
        try {
            val jsonObject = gson.fromJson(jsonResponse, JsonObject::class.java)
            
            val code = jsonObject.get("code").asInt
            if (code != 200) {
                println("API returned error code: $code")
                return emptyList()
            }
            
            val data = jsonObject.getAsJsonObject("data")
            
            if (data.has("list")) {
                val stopsArray = data.getAsJsonArray("list")
                
                for (i in 0 until stopsArray.size()) {
                    val stop = stopsArray.get(i).asJsonObject
                    
                    val id = stop.get("id").asString
                    val name = stop.get("name").asString
                    val code = if (stop.has("code")) stop.get("code").asString else null
                    val lat = stop.get("lat").asDouble
                    val lon = stop.get("lon").asDouble
                    val direction = if (stop.has("direction")) stop.get("direction").asString else null
                    
                    stations.add(BusStation(id, name, code, lat, lon, direction))
                }
            }
        } catch (e: Exception) {
            println("Error parsing JSON response: ${e.message}")
            e.printStackTrace()
        }
        
        return stations
    }

    /**
     * Fetches bus arrivals for a specific station
     * @param stationId The ID of the station to fetch arrivals for
     * @return List of BusArrival objects
     */
    suspend fun fetchBusArrivals(stationId: String): List<BusArrival> {
        return withContext(Dispatchers.IO) {
            try {
                val arrivalsUrl = "$BASE_URL/arrivals-and-departures-for-stop/$stationId.json?key=$API_KEY&minutesBefore=0&minutesAfter=60"
                
                val response = makeApiRequestWithRetry(arrivalsUrl)
                parseArrivalsResponse(response)
            } catch (e: Exception) {
                println("Error fetching arrivals for station $stationId: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Parses the arrivals JSON response
     */
    private fun parseArrivalsResponse(jsonResponse: String): List<BusArrival> {
        val arrivals = mutableListOf<BusArrival>()
        
        try {
            val jsonObject = gson.fromJson(jsonResponse, JsonObject::class.java)
            
            val code = jsonObject.get("code").asInt
            if (code != 200) {
                println("API returned error code: $code")
                return emptyList()
            }
            
            val data = jsonObject.getAsJsonObject("data")
            val entry = data.getAsJsonObject("entry")
            
            if (entry.has("arrivalsAndDepartures")) {
                val arrivalsArray = entry.getAsJsonArray("arrivalsAndDepartures")
                
                for (i in 0 until arrivalsArray.size()) {
                    val arrival = arrivalsArray.get(i).asJsonObject
                    
                    val routeId = arrival.get("routeId").asString
                    val routeShortName = arrival.get("routeShortName").asString
                    
                    val tripHeadsign = if (arrival.has("tripHeadsign")) arrival.get("tripHeadsign").asString else "Unknown"
                    
                    val scheduledArrivalTime = arrival.get("scheduledArrivalTime").asLong
                    
                    val predictedArrivalTime = if (arrival.has("predictedArrivalTime") &&
                        !arrival.get("predictedArrivalTime").isJsonNull) {
                        arrival.get("predictedArrivalTime").asLong
                    } else {
                        null
                    }
                    
                    val distanceFromStop = if (arrival.has("distanceFromStop") &&
                        !arrival.get("distanceFromStop").isJsonNull) {
                        arrival.get("distanceFromStop").asDouble
                    } else {
                        null
                    }
                    
                    arrivals.add(
                        BusArrival(
                            routeId = routeId,
                            routeName = routeShortName,
                            tripHeadsign = tripHeadsign,
                            predictedArrivalTime = predictedArrivalTime,
                            scheduledArrivalTime = scheduledArrivalTime,
                            distanceFromStop = distanceFromStop
                        )
                    )
                }
                
                return arrivals.sortedBy { it.getMinutesUntilArrival() }
            }
        } catch (e: Exception) {
            println("Error parsing arrivals JSON: ${e.message}")
            e.printStackTrace()
        }
        
        return arrivals
    }
} 