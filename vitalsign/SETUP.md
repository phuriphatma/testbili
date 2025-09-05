# VitalSign Patient Manager - Setup Guide

## Project Overview

This Android application manages patient data with the following key features:

### ✅ Implemented Features

1. **Patient List Management**
   - View all patients in a list
   - Add new patients with floating action button
   - Click on patient to view details

2. **Patient Detail View**
   - Display current date and day (automatically updated)
   - Date of birth input with date picker
   - Real-time age calculation
   - Automatic timeline generation from birth year to current year

3. **Life Timeline Display**
   - Vertical list showing: Age | Year | Buddhist Year | Events
   - Buddhist year conversion (Gregorian + 543 years)
   - Sample display:
     ```
     0  | 2004 | 2547 | natal: jaundice
     1  | 2005 | 2548 | accident  
     2  | 2006 | 2549 |
     ...
     21 | 2025 | 2568 |
     ```

4. **Event Management**
   - Add events to specific ages using plus icon
   - Store event descriptions
   - Support for month/day specification (framework ready)

5. **Monthly View Button**
   - Button placeholder for future monthly calendar view

## To Build and Run

### Prerequisites
- Android Studio (latest version)
- Android SDK API 24 or higher
- Kotlin support

### Setup Steps

1. **Open in Android Studio**
   ```bash
   cd /Users/xeno/webdev/vitalsign
   # Open this folder in Android Studio
   ```

2. **Sync Project**
   - Android Studio will prompt to sync Gradle files
   - Click "Sync Now" when prompted

3. **Add App Icons (Optional)**
   - Replace placeholder icon files in `app/src/main/res/mipmap-*` folders
   - Use Android Studio's Image Asset Studio for proper icon generation

4. **Build Project**
   ```bash
   ./gradlew build
   ```

5. **Run on Device/Emulator**
   - Connect Android device or start emulator
   - Click "Run" in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/vitalsign/patientmanager/
│   │   ├── MainActivity.kt              # Patient list
│   │   ├── PatientDetailActivity.kt     # Patient timeline view
│   │   ├── AddPatientActivity.kt        # Add new patient
│   │   ├── model/
│   │   │   └── Patient.kt               # Data models
│   │   └── adapter/
│   │       ├── PatientListAdapter.kt    # RecyclerView adapter
│   │       └── LifeEventAdapter.kt      # Timeline adapter
│   ├── res/
│   │   ├── layout/                      # UI layouts
│   │   ├── values/                      # Strings, colors, themes
│   │   └── mipmap-*/                    # App icons
│   └── AndroidManifest.xml
└── build.gradle                         # Dependencies
```

## Key Features Demonstrated

### 1. Date Handling
- Current date display with day of week
- Date picker for birth date input
- Age calculation based on birth date

### 2. Buddhist Calendar
- Automatic conversion from Gregorian to Buddhist years
- Display both calendars side by side

### 3. Timeline Generation
- Dynamic timeline from birth year to current year
- Age-based event organization
- Vertical scrollable list

### 4. Material Design
- Modern Android UI with Material 3 components
- Floating action buttons
- Card-based layouts
- Medical-themed color scheme

## Future Enhancements

To extend this application, consider adding:

1. **Database Persistence**
   - Room database for data storage
   - Patient data persistence across app restarts

2. **Monthly Calendar View**
   - Implement the monthly view button functionality
   - Calendar widget showing events by month

3. **Advanced Event Management**
   - Edit/delete events
   - Event categories (medical, personal, etc.)
   - Photo attachments

4. **Export/Import**
   - PDF report generation
   - Data backup/restore

5. **Search and Filter**
   - Patient search functionality
   - Filter by age, events, etc.

## Technical Notes

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Language**: Kotlin
- **UI Framework**: View Binding + Material Design 3
- **Architecture**: Simple MVP pattern

The app is ready to run and demonstrates all requested features including patient data storage, current date display, age timeline with Buddhist years, and event management with a plus icon interface.
