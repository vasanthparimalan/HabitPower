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

## Guide and Help

### Quick Start

1. Open **Admin > Manage Users** and add each person who will be tracked.
2. Open **Admin > Manage Habits** and create reusable habit definitions.
3. Open **Admin > Manage Life Areas** and keep or customize the starter areas.
4. Open **Admin > Assignments** to assign habits and life areas per user.
5. Go to **Dashboard** and switch the active user to review KPIs and log check-ins.

### Family Multiuser Workflow

- One device can track multiple family members, including children without phones.
- Adults can configure habits once and assign age-appropriate subsets to each child.
- Each user has separate streaks, progress, and life-area analytics.
- Use Dashboard user switching before daily check-in to log the correct person.
- For younger children, do parent-assisted check-ins at routine times (for example, after school and before bed).

### Gamification and Achievements

- Completing habits grants XP.
- Consecutive successful days increase streaks.
- XP progression increases user level over time.
- Milestones and achievements are designed to reinforce consistency.
- Open the in-app **Guide & Help** screen for a full explanation and usage tips.

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by Atomic Habits by James Clear
- Built with Android Jetpack libraries
- Icons from Material Design Icons

## Design System and Accessibility

This app uses a task-first visual language inspired by modern habit trackers while keeping its own identity.

### Theme Foundation

- Primary: calm blue for navigation, primary actions, and progress.
- Secondary: deep blue for supporting controls and emphasis.
- Tertiary: green for positive progress and consistency states.
- Error: muted red reserved for destructive states only.
- Surfaces: neutral, low-glare layers to stay readable with night mode and blue-light filters.

### Typography

- Sans-serif hierarchy for quick scanning and low cognitive load.
- Headlines communicate focus and immediate action.
- Body text keeps comfortable line height for repeated daily reading.
- Labels are compact but readable for chips, counters, and metadata.

### Iconography

- Icons represent stable intent, not decoration.
- Core tab icons are fixed by meaning:
	- Dashboard: DateRange
	- Routines: List
	- Focus: PlayArrow
	- Analytics: Insights
- Pair icons with text labels for clarity and accessibility.

### Accessibility Rules

- Do not rely on color alone; pair color states with labels, shape, or icon cues.
- Maintain strong text-to-background contrast in both light and dark themes.
- Keep celebratory and warning colors semantically distinct (avoid red-green ambiguity).
- Preserve touch target size and spacing for one-handed operation.

### Design Principles Applied

- Atomic Habits:
	- Make it obvious: clear sections and predictable icon meanings.
	- Make it easy: low-friction defaults and visible next actions.
	- Make it satisfying: subtle celebrations tied to real progress.
- The Design of Everyday Things:
	- Strong signifiers and mappings for key actions.
	- Immediate feedback after check-ins and streak events.
	- Consistent patterns across admin, dashboard, and daily flows.
