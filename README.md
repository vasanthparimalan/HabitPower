# HabitPower

A comprehensive habit tracking Android application built with Kotlin and Jetpack Compose. Track your daily habits, monitor progress with visual heatmaps, and build lasting positive routines inspired by Atomic Habits principles.

## Features

- **Habit Tracking**: Create and track various types of habits (boolean, count, duration, time-based)
- **Visual Dashboard**: 3-month GitHub-style heatmap showing habit completion patterns
- **Motivational KPIs**: Track current streak, weekly consistency, and personal best records
- **Daily Check-ins**: Easy daily habit logging with intuitive interface
- **Widget Support**: Home screen widget for quick habit updates
- **Data Persistence**: Local Room database for offline functionality
- **Material Design 3**: Modern, accessible UI following latest Android design guidelines

## Screenshots

*[Add screenshots here]*

## Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Minimum Android SDK 26 (Android 8.0)
- Kotlin 1.9.21

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/yourusername/LifeTrack.git
cd LifeTrack
```

2. Open in Android Studio and sync Gradle files

3. Build and run on device/emulator:
```bash
./gradlew assembleDebug
```

## Architecture

- **UI Layer**: Jetpack Compose for declarative UI
- **ViewModel**: State management with LiveData/Flow
- **Repository**: Data access abstraction
- **Database**: Room ORM with SQLite
- **Dependency Injection**: Manual DI (can be upgraded to Hilt/Dagger)

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by Atomic Habits by James Clear
- Built with Android Jetpack libraries
- Icons from Material Design Icons
