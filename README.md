# OBA Wear OS

A sample Wear OS application demonstrating integration with the OneBusAway API for Seattle bus transit information.

## About

This repository contains a proof-of-concept implementation of a public transit app for Wear OS smartwatches. It showcases how to work with location services, maps, and external APIs within the constraints of a wearable device.

## Key Features

- Map interface showing bus stations in Seattle
- Location-based functionality with geofencing for Seattle area
- Real-time bus arrival information display

## Technical Details

### API Constraints & Workarounds

This sample intentionally uses a limited approach to demonstrate API handling strategies:

- **Hardcoded Station IDs**: Uses 3 predefined Seattle bus station IDs instead of dynamic searches to avoid rate limiting
- **TEST API Key**: Operates with a testing API key with restricted access
- **Error Handling**: Demonstrates graceful handling of common API issues (timeout, rate limits, etc.)
- **Request Delays**: Implements pauses between API calls to prevent Error 429 (rate exceeded)

### Test Conditions

When testing this application:
- The app will show a maximum of 3 stations regardless of actual station density
- Station selection is fixed and does not change based on actual proximity
- Location simulation may be needed if testing outside Seattle
- The dialog prompt to "relocate to Seattle" is primarily for demonstration purposes

### UI Implementation

- Demonstrates proper Wear OS UI patterns using Jetpack Compose
- Shows how to implement scrollable content in small displays
- Includes examples of location permission handling
- Provides map interaction samples optimized for watch interfaces



## Note

This is not intended as a full production app but rather as a sample implementation to demonstrate specific technical concepts in Wear OS development.
