# Data Export

Export your full habit history at any time from **Admin › Export Data**. Two formats available: CSV for analysis, JSON for backup.

---

## CSV Export

Best for: personal analysis in Excel or Google Sheets, sharing data with a coach or accountability partner.

**Columns:**

| Column | Description |
|--------|-------------|
| date | Entry date (YYYY-MM-DD) |
| user_name | User profile name |
| habit_name | Habit name |
| habit_type | BOOLEAN, COUNT, DURATION, NUMBER, TEXT, ROUTINE, TIMER, POMODORO, TIME |
| life_area_id | ID of the associated life area |
| boolean_value | true/false for boolean/routine habits |
| numeric_value | Numeric entry for count/duration/number habits |
| text_value | Text entry for text habits |

One row per habit per day where an entry was recorded. Days with no entry are not included.

---

## JSON Export

Best for: complete manual backup, data migration, archiving.

**Structure:**
```
{
  "exportedAt": "...",
  "users": [...],
  "habits": [...],
  "habitEntries": [...],
  "healthStats": [...],
  "userStats": [...]
}
```

Includes every record in the database — all users, all habits, all check-in entries, daily health stats, and full gamification state per user. Pretty-printed and human-readable.

---

## How to Export

1. Go to **Admin › Export Data**
2. Choose **CSV** or **JSON**
3. Tap the Save button for your chosen format
4. The system file browser opens — navigate to your destination
5. Edit the filename if desired, then confirm
6. A confirmation message appears when the file is saved

No app permissions are required. The file is written directly to the location you choose.

---

## Recommended backup routine

For important data, do both:
1. **Rely on Auto Backup** for automatic protection (see [Your Data & Backup](Data-and-Backup))
2. **Export JSON monthly** to a cloud storage folder (Google Drive, OneDrive, etc.) for an independent copy you control

The JSON export is a complete snapshot that can serve as a restore point independent of Android's backup system.
