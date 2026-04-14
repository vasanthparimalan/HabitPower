# Your Data & Backup

HabitPower stores everything locally on your device. No accounts, no servers. Your data is yours.

---

## Android Auto Backup

HabitPower is enrolled in **Android Auto Backup** (enabled in v1.2). This means:

- Android automatically backs up your habit database and preferences to your Google account on the same schedule as your other apps — typically once every 24 hours when the device is idle, on Wi-Fi, and charging.
- The backup uses your existing **Google One** storage quota. If you already back up your phone, HabitPower is covered automatically.
- Backups retain the last 60 days of history.

### How to verify your backup is running

1. Open Android **Settings**
2. Go to **System › Backup** (exact path varies by Android version and manufacturer)
3. Confirm "Backup by Google One" is turned on
4. You should see "Last backup" with a recent timestamp

### Restoring on a new phone

1. During new device setup, sign in to the **same Google account** you used on the old phone
2. Choose **Restore from backup** and select your old device
3. Install HabitPower (download the APK or install from your source)
4. Your data will restore automatically on first launch

> **Sideloaded APK note:** Android restores app data by package name (`com.example.healthtrack`). The backup runs correctly whether or not the app came from the Play Store — as long as you install the same APK on the new device and sign in to the same Google account.

---

## Manual Export

For complete control over your data, use **Admin › Export Data**.

### CSV Export — Analysis
- One row per habit-day entry
- Columns: date, user name, habit name, habit type, life area, boolean value, numeric value, text value
- Import into Excel or Google Sheets for personal analysis
- Best for: understanding patterns, building your own charts, sharing with a coach

### JSON Export — Full Backup
- Complete structured dump of your entire database:
  - All user profiles
  - All habit definitions
  - All daily entries
  - All health stats
  - Gamification stats per user
- Human-readable, pretty-printed
- Best for: manual backups before a phone swap, data portability, long-term archiving

### How to export

1. Open **Admin › Export Data**
2. Choose CSV or JSON
3. Tap the save button — the system file browser opens
4. Navigate to your destination (local storage, Google Drive, etc.)
5. Confirm the filename and save

No app permissions are required. Android's Storage Access Framework handles the file write directly.

---

## What is and isn't backed up

| Data | Auto Backup | Export CSV | Export JSON |
|------|-------------|------------|-------------|
| Habit definitions | Yes | Yes | Yes |
| Daily check-in entries | Yes | Yes | Yes |
| Streaks and XP | Yes | — | Yes |
| Life areas | Yes | — | Yes |
| User profiles | Yes | — | Yes |
| Notification preferences | Yes | — | — |
| App settings | Yes | — | — |

---

## Privacy

All data stays on your device and in your Google account. HabitPower has no backend, no analytics SDK, and no network requests. Exports go wherever you direct them — the app never sees the destination.
