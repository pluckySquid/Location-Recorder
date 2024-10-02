# Location Recorder

**Location Recorder** is an Android application designed to record and display location data using the device's GPS functionality. 
It allows users to track their location and view recorded locations on the app interface. This project is built using Java and 
follows a standard Android architecture, utilizing activities, fragments, and services.

## Features

- **Real-Time Location Tracking:** Continuously records the user's location.
- **Fragments for UI:** Two fragments (`FirstFragment` and `SecondFragment`) provide the user interface for interacting with the recorded locations.
- **Android GPS Services:** Utilizes Android's GPS system to retrieve the device's current location.
- **Firebase Integration:** Firebase is used for real-time data storage, enabling users to store location data remotely and retrieve it when needed.

## Project Structure

The project follows a typical Android application structure:

- **`MainActivity.java`**: The main activity that manages the fragments and acts as the entry point for the application.
- **`LocationRecorder.java`**: Manages the logic for recording location data and interacting with the Firebase backend.
- **`RecordedLocation.java`**: A model class representing the details of a recorded location, including fields such as latitude, longitude, and timestamp.
- **`FirstFragment.java`**: Displays the list of recorded locations.
- **`SecondFragment.java`**: Another fragment used for displaying additional UI elements or location data.


## Installation

### Prerequisites

- **Android Studio**: The official IDE for Android development.
- **Java**: Ensure your development environment is set up to support Java.
- **Firebase Project**: A Firebase project should be set up, and the necessary configuration files (like `google-services.json`) should be included in the app.

### Steps to Run

1. **Clone the Repository**

   ```bash
   git clone git@github.com:pluckySquid/Location-Recorder.git
2. # Open the Project in Android Studio

1. Open Android Studio.
2. Select **Open an Existing Project**.
3. Navigate to the `Location-Recorder` directory and open it.

3. # Sync and Build

1. Ensure that Gradle dependencies are properly synced by clicking **Sync Now** when prompted.
2. Build the project by selecting **Build > Make Project** in the menu.

4. # Run on Emulator or Device

1. Connect an Android device via USB or start an Android emulator.
2. Click **Run** to launch the app.

5. # Usage

Once the app is installed on your device, it will:

- Track your location using the device's GPS services.
- Store recorded locations in Firebase.
- Display recorded locations through its user interface.
- Allow interaction with recorded location data via fragments.

6. # Files Overview

- **MainActivity.java**: Manages the main user interface and controls fragment transactions.
- **LocationRecorder.java**: Handles the logic related to location recording and interaction with Firebase for real-time storage.
- **RecordedLocation.java**: Represents the data structure for storing location details.
- **FirstFragment.java**: Displays part of the user interface, possibly related to the recorded locations.
- **SecondFragment.java**: Another fragment for additional interaction or display of data.

