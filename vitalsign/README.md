# VitalSign Patient Manager

An Android application for managing patient data with age timeline and Buddhist year conversion.

## Features

- **Patient Management**: Store and manage patient information
- **Current Date Display**: Shows current date and day of the week
- **Date of Birth Input**: Input patient's birth date with date picker
- **Age Timeline**: Displays patient's age timeline from birth to current year
- **Buddhist Year Conversion**: Shows Buddhist calendar years alongside Gregorian years
- **Life Events**: Add and track significant events in patient's life
- **Chronological Display**: View life events in age order (0, 1, 2... current age)

## Sample Timeline Display

```
Age | Year | Buddhist | Events
----|------|----------|--------
0   | 2004 | 2547     | natal: jaundice
1   | 2005 | 2548     | accident
2   | 2006 | 2549     | 
...
21  | 2025 | 2568     | 
```

## Key Components

### Activities
- `MainActivity`: Patient list view with add functionality
- `PatientDetailActivity`: Detailed patient view with timeline
- `AddPatientActivity`: Form to add new patients

### Data Models
- `Patient`: Main patient data model with age calculation
- `LifeEvent`: Individual life events with Buddhist year conversion

### Features Implementation
- ✅ Current date and day display
- ✅ Date of birth input with date picker
- ✅ Age calculation from birth date
- ✅ Buddhist year conversion (Gregorian + 543)
- ✅ Vertical timeline display
- ✅ Event addition with plus icon
- ✅ Month/day specification for events
- ✅ Monthly view button (placeholder)

## Getting Started

1. Open project in Android Studio
2. Sync Gradle files
3. Run on Android device or emulator (API 24+)

## Requirements

- Android API 24+ (Android 7.0)
- Kotlin support
- Material Design 3 components

## Future Enhancements

- Database persistence with Room
- Monthly calendar view
- Event editing and deletion
- Data export functionality
- Patient search and filtering
