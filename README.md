# ElevationMap

ElevationMap is a cross-platform mobile application that leverages the power of Kotlin Multiplatform Mobile (KMM) to share business logic
across iOS and Android platforms, while allowing each to maintain its own native user interface for the best user experience.

## Features

- Display a map centered at a predefined location with zoom controls.
- Fetch and display the user's current location.
- Request location permissions from the user when needed.
- Utilize shared common logic between Android and iOS platforms.

## Technologies

- **Kotlin Multiplatform Mobile (KMM):** Shares common logic for both Android and iOS platforms.

- **Android:**
    - **Jetpack Compose:** Modern declarative UI toolkit for Android to define UI components.
    - **Google Maps Android API:** For displaying and interacting with maps.
    - **Coroutines:** For managing background tasks with simplified code and reducing needs for callbacks.
    - **Permissions API with Accompanist:** For handling runtime permissions.

- **iOS:**
    - **SwiftUI:** Declarative Swift API for building UIs.
    - **MapKit:** For rendering maps on iOS devices.
    - **CocoaPods:** Dependency manager for Swift and Objective-C Cocoa projects.

## Structure

The project is divided into multiple modules:

- `:shared`: Contains the shared business logic written in Kotlin.
- `androidApp`: Android application module powered by Jetpack Compose.
- `iosApp`: iOS application module powered by SwiftUI.

### Shared Module

The shared module contains common code which includes domain logic, data management, and permission handling. This module compiles into an
Android library and an iOS framework to be used by respective platform-specific modules.

#### Key Components:

- `PermissionStateShared`: An expected interface for handling permission state
- that has actual implementations in both Android and iOS modules.
- `ContextShared`: An expected interface providing a context for operations which require it, like getting the last known location.
- `LocationShared`: An expected class representing a geographical location.
- `GoogleMapShared`: An expected class abstracting the map component for both platforms.

### Android Module

Utilizes Jetpack Compose to build the UI and interacts with the shared module for executing common logic.

### iOS Module

Leverages SwiftUI for UI components and integrates the shared module for common functionality.

## Getting Started

To get started with the ElevationMap project, clone the repository and follow the setup instructions for each platform.

### Android

1. Open the `androidApp` module in Android Studio.
2. Sync the project with Gradle files.
3. Run the application on an emulator or a physical device.

### iOS

1. Navigate to the `iosApp` directory.
2. Run `pod install` to fetch the shared framework and other CocoaPods dependencies.
3. Open the generated `.xcworkspace` file in Xcode.
4. Run the application on an iOS simulator or a physical device.
