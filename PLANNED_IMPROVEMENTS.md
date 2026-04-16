# HabitPower — Planned Improvements

> **App Philosophy:** Free. Helpful. Built for productive people.
> Every feature added here must reflect that principle — no engagement tricks,
> no gamification inflation, no dark patterns. Only genuine utility.
>
> **Status:** Planned. Not yet implemented. Features will be added one by one.
> Check off each section as it is completed.

---

## Why This List Exists

A candid audit of HabitPower as a *lifetime companion* scored it at **6.8 / 10**.
The goal is to reach **8.5–9.0** through focused, principled additions.

The gaps fell into three buckets:
1. **Survivability** — data can be lost permanently with no warning
2. **Longevity** — the app hits walls (gamification ceiling, habit lifecycle dead-ends)
3. **Intelligence** — the experience is static; it doesn't learn or surface insights

---

## P0 — Foundation (Survivability)
*These are non-negotiable. Everything else is irrelevant if data dies.*

### [x] 1. Enable Android Auto Backup
**What:** Change `android:allowBackup="false"` → `android:allowBackup="true"` in
`AndroidManifest.xml`. The `backup_rules.xml` already correctly includes both the
Room database and shared preferences — so this is a **one-line fix**.

**Why it matters:** Currently, the Room database has zero protection. A factory
reset, lost phone, or failed OS update destroys years of habit history permanently.
Android Auto Backup uses the user's existing Google One schedule — no app-side
infrastructure, no sync, fully user-controlled.

**Scope:** 1 line in `AndroidManifest.xml`.

**Completed:** 2026-04-14
- `AndroidManifest.xml`: `allowBackup` flipped to `true`. `backup_rules.xml` (Android 6–11)
  and `data_extraction_rules.xml` (Android 12+) were already correctly configured — both
  activate automatically with this change.
- `HelpGuideScreen.kt`: Added a one-line callout in Quick Start (visible at install time)
  and a full "Your Data & Backup" section (7 bullets covering how backup works, how to
  verify it, the new-phone restore flow specific to sideloaded APKs, and the 60-day
  retention window).

---

### [x] 2. User Data Export (CSV + JSON)
**What:** Add an Export screen (accessible from Settings or Profile) that lets the
user export their full `DailyHabitEntry` history and `UserStats` as either:
- **CSV** — one row per habit-day entry, importable into Excel/Sheets for personal analysis
- **JSON** — full structured dump, usable as a manual backup or data migration tool

Delivered via Android's `ShareCompat` / `FileProvider` — the user chooses where it
goes (Drive, email, local storage). No server involved.

**Why it matters:** Productive people care about owning their data. This also
serves as a user-initiated backup path independent of Auto Backup.

**Scope:** New `ExportViewModel` + simple Export screen + `FileProvider` config.

**Completed:** 2026-04-14
- Used Android Storage Access Framework (`ActivityResultContracts.CreateDocument`) instead of
  FileProvider + ShareCompat — user picks the destination folder and filename directly in the
  system file browser. No temp files, no permissions needed.
- `HabitTrackingDao.kt`: added `getAllEntriesForUser(userId)` suspend query.
- `HabitPowerRepository.kt`: exposed `getAllEntriesForUser()` and `getAllHealthStats()`.
- `Screen.kt`: added `AdminExport` route.
- `ExportViewModel.kt`: new file — serializes data in a coroutine, emits a `PendingExport`
  state that triggers the SAF launcher; `writeTo(uri)` writes directly via ContentResolver.
- `ExportScreen.kt`: new file — two cards (CSV analysis / JSON backup), each with a
  description of what's included and a save button. Loading overlay during preparation.
  Snackbar confirmation on success or error.
- `AdminHomeScreen.kt`: new "Data" section with Export Data entry.
- `MainActivity.kt`: route wired up.
- `HelpGuideScreen.kt`: "coming soon" bullet updated to point to Admin > Export Data.
- **CSV** exports: date, user name, habit name, type, life area, and all value columns.
- **JSON** exports: users, habits, all entries, health stats, gamification stats per user —
  pretty-printed, human-readable.

---

## P1 — Longevity (Habit Lifecycle + Meaningful Progress)
*For people who actually master habits over years.*

### [x] 3. Habit Lifecycle States: Pause / Retire / Graduate
**What:** Add a `status` field to `HabitDefinition`:
- **ACTIVE** — current default, tracked daily
- **PAUSED** — temporarily skipped (travel, illness, life events). Streak calculation
  is suspended. Reminders silenced. Does not count as a miss.
- **RETIRED** — habit discontinued. History preserved. Excluded from daily tracking.
- **GRADUATED** — the habit is now automatic behavior. Moves to an "Identity Wall" —
  a read-only display of habits the user has internalized. A point of quiet pride,
  not a graveyard.

**Why it matters:** Without this, long-term users either delete habits (losing history)
or feel judged by habits they've outgrown. Productive people evolve their systems —
the app should support that gracefully.

**Scope:** DB migration (new column), updated DAO queries, UI changes in habit
management screens, new "Identity Wall" section on Dashboard or Profile.

**Completed:** 2026-04-15
- `HabitLifecycleStatus.kt`: new enum — ACTIVE / PAUSED / RETIRED / GRADUATED, each with
  `label`, `description`, and `isTracked` properties.
- `HabitDefinition.kt`: added `lifecycleStatus: HabitLifecycleStatus = ACTIVE`.
- `Converters.kt`: added TypeConverter pair for `HabitLifecycleStatus` ↔ String.
- `HabitPowerDatabase.kt`: bumped to version 20; `MIGRATION_19_20` adds
  `lifecycleStatus TEXT NOT NULL DEFAULT 'ACTIVE'` to `habit_definitions`.
- `HabitTrackingDao.kt`: added `getGraduatedHabitsForUser(userId)` query (joins assignments).
- `HabitPowerRepository.kt`:
  - Added `getGraduatedHabitsForUser()`.
  - Added `setHabitLifecycle()` — updates lifecycle status and keeps `isActive` in sync
    (`isActive = status.isTracked`) so all existing DAO filters continue to work.
  - Fixed `syncHabitReminders()` — now skips scheduling for non-ACTIVE habits even if
    they remain in the user's assignment list.
- `AdminHabitsViewModel.kt`: added `setHabitLifecycle()`.
- `AdminHabitsScreen.kt`: each habit card now shows a lifecycle dropdown (Active /
  Paused / Retired / Graduated) with label + description per option. Status also appears
  in the attributes row for at-a-glance visibility.
- `DashboardViewModel.kt`: added `graduatedHabits` StateFlow (flatMapLatest on activeUser).
- `DashboardScreen.kt`: added `IdentityWallCard` — collapsible card at the bottom of
  Dashboard, shown only when graduated habits exist. Displays habit name + identity
  statement per entry. Hidden by default, expand on tap.

---

### [x] 4. Progression Beyond Level 20
**What:** Extend `GamificationEngine.LEVEL_XP_THRESHOLDS` with a formula-generated
tail so levels never hard-cap. After Level 20, each subsequent level requires
`30,000 + (level - 20) * 8,000` XP. No prestige resets, no inflation — just
honest continued progression for users who stay for years.

Also add **mastery milestones** at Level 25, 30, 40, 50 with new title names
and a badge, so long-term users have visible waypoints without manufactured drama.

**Why it matters:** A dedicated user hits Level 20 ("Enlightened") in roughly
8 months. The XP loop then goes silent. For a lifetime app, the progress curve
must outlast the user.

**Scope:** `GamificationEngine.kt` formula extension + new level names + 3–4
new badge entries in `Badge` object.

**Completed:** 2026-04-14
- `GamificationEngine.kt`: removed hard cap (`MAX_LEVEL = Int.MAX_VALUE`). Added
  `LEGACY_MAX_LEVEL = 20` and `XP_PER_POST_LEVEL = 8_000`. `levelForXp`, `xpForLevel`,
  `xpForNextLevel`, `levelProgress` all updated to use formula for levels 21+.
  `xpForNextLevel` return type changed from `Int?` to `Int` (no cap, no null).
- Added `POST_LEVEL_NAMES` array for levels 21–50 (Philosopher → Undying), then
  `levelName()` returns "Level N" for 51+.
- Added four mastery milestone badges: `LEVEL_25` (Mastermind), `LEVEL_30` (Eternal),
  `LEVEL_40` (Mythic), `LEVEL_50` (Undying) — bits 11–14. Registered in `Badge.ALL`
  and checked in `computeNewBadgesMask`.
- `MotivationContent.kt`: added `levelUpMessage` cases for 25, 30, 40, 50 and
  `badgeEarnedTitle` for all four new badges. Level 20 message updated to note
  "the first pinnacle — not the end."

---

### [x] 5. Year-in-Review Screen
**What:** On January 1st (or on-demand from the Report screen), generate a
scrollable summary screen for the past year:
- Total habits completed
- Longest streak achieved
- Best life area
- XP gained and level journey
- Total perfect days
- A single personal headline generated from the data (e.g., "You showed up
  287 days out of 365. That's who you are now.")

Shareable as text via `Intent.ACTION_SEND`.

**Why it matters:** This is the feature that makes users never want to delete
the app. It anchors the year's effort into a single emotional moment.
Requires no new data — everything is already in `UserStats` and `DailyHabitEntry`.

**Scope:** New `YearInReviewScreen` + `YearInReviewViewModel` + text share
via `Intent.ACTION_SEND`.

**Completed:** 2026-04-14
- `Screen.kt`: added `YearInReview` route (`report/year-in-review`).
- `YearInReviewViewModel.kt`: new file. Takes `HabitPowerRepository` +
  `GamificationRepository`. Offers `availableYears` (current + previous year,
  ordered by recency). Computes from raw entries: total habits completed, active
  days, perfect days, best streak, best life area (by completion %), estimated
  XP gained (via `GamificationEngine.computeXpGain`). Generates a personal
  headline sentence from consistency data. `buildShareText()` produces a formatted
  plain-text summary.
- `YearInReviewScreen.kt`: new file. Year selector chips → headline card →
  6-stat grid (emoji + value + label cards) → level card → share button →
  closing message. Top-bar share icon for quick access.
- `ReportScreen.kt`: added `onNavigateToYearInReview` parameter. A tappable
  `YearInReviewBanner` card (tertiaryContainer) shown between the headline and
  period selector — visible whenever the caller provides the nav lambda.
- `MainActivity.kt`: `Report` composable now passes `onNavigateToYearInReview`;
  new `composable(Screen.YearInReview.route)` entry wired up.
- `AppViewModelProvider.kt`: `YearInReviewViewModel` initializer registered.

---

## P2 — Intelligence (Adaptive, Proactive)
*The app should learn and surface what matters, not wait to be asked.*

### [ ] 6. Adaptive Reminder Timing
**What:** After 14 days of data, analyze what time of day each habit is actually
completed (from `DailyHabitEntry` timestamps). If the actual completion time is
consistently 2+ hours away from the configured reminder time, surface a suggestion:
*"You usually complete 'Morning Run' around 7:30 AM. Want to move the reminder
from 6:00 AM to 7:15 AM?"*

Also add a **streak-at-risk nudge**: if a user hasn't completed any habits by
90 minutes before their self-defined bedtime (configurable), send a single gentle
notification.

**Why it matters:** Static reminders become invisible noise within weeks. Productive
people tune them out. Reminders that match actual behavior patterns remain useful
for years.

**Scope:** Timestamp recording on `DailyHabitEntry` (new field or derived from
creation time) + analysis logic in `HabitReminderScheduler` + suggestion UI in
habit detail/edit screen.

---

### [ ] 7. Proactive Weekly Insight Notification
**What:** Every Sunday evening, send one notification summarizing the week's most
actionable insight — derived from the existing `ReportViewModel` analytics:

Examples:
- "Your weakest area this week was Sleep (38%). One win tomorrow starts the turn."
- "You completed 6 of 7 days. Your longest current streak is 14 days — protect it."
- "When you completed Morning Exercise, you completed 2.3× more habits that day."

One notification. Honest. Actionable. No streak-shaming, no empty praise.

**Why it matters:** The Report screen already computes this data. It just waits
passively for the user to open it. Surfacing one sharp insight weekly is more
valuable than a full dashboard nobody visits.

**Scope:** `WeeklyInsightWorker` (WorkManager) + insight-selection logic drawn
from `ReportViewModel` computation + single notification channel.

---

### [ ] 8. Health Connect Integration
**What:** Integrate Android's Health Connect API (available SDK 26+, matching
the app's minSdk) to auto-fill compatible habits from system health data:
- **Steps** → auto-fill Count habits with a "steps" tag
- **Sleep duration** → auto-fill Duration habits tagged "sleep"
- **Active minutes / exercise time** → auto-fill Duration habits

The auto-fill is a *suggestion*, not an override — the user confirms or edits it
during daily check-in. No data leaves the device.

**Why it matters:** Reduces the daily logging friction that causes abandonment.
Productive people who wear a watch or use Google Fit shouldn't have to manually
re-enter data the OS already knows.

**Scope:** New `HealthConnectManager` class + READ permissions in manifest +
integration into `DailyCheckInViewModel` to pre-populate matching entries.

---

## P3 — Polish (Friction Reduction)
*Small changes with outsized daily impact.*

### [ ] 9. Quick-Complete from Notification
**What:** Habit reminder notifications gain an action button: **"Done"**. Tapping
it marks the habit complete for today without opening the app. Uses a
`BroadcastReceiver` to write the `DailyHabitEntry` directly.

**Why it matters:** The fewer taps between intent and completion, the better the
long-term completion rate. Productive people respect low-friction tools.

**Scope:** Updated `HabitReminderReceiver` + `NotificationCompat.Action` +
direct repository write from receiver.

---

### [ ] 10. Habit Template Library
**What:** On the "Add Habit" screen, offer a curated library of pre-configured
habit templates organized by archetype:
- **Builder** (developer/creator): Deep Work (Duration), Read (Duration),
  Ship Something (Boolean), No Phone First Hour (Boolean)
- **Athlete:** Morning Training (Duration), Steps (Count), Hydration (Count),
  Sleep 7h+ (Duration)
- **Student:** Study Block (Pomodoro), Flashcard Review (Boolean),
  No Social Media After 10pm (Boolean)
- **Mindful:** Meditation (Duration), Gratitude Journal (Text),
  Walk Outside (Boolean)

Templates pre-fill name, type, target, reminder time, and identity statement.
User edits before saving — they're starting points, not prescriptions.

**Why it matters:** Cold-start friction is the #1 reason new users abandon
habit apps in the first week. Templates get productive people to their first
streak in under 2 minutes.

**Scope:** Static template data class + template picker composable injected
into `AddEditHabitScreen`.

---

### [ ] 11. Mood × Habit Correlation in Report
**What:** `DailyHealthStat.mood` is already stored. Surface a simple correlation
in the Report screen: on days when all habits are completed, what is the average
mood score vs. partial or zero-completion days?

A single sentence + a small bar comparison. No ML required — just a mean
comparison across the existing data.

**Why it matters:** This closes the loop between effort and wellbeing in a way
that is honest and personally meaningful. It answers "does this actually make
me feel better?" with the user's own data.

**Scope:** New computation in `ReportViewModel.buildReport()` + small UI
component in `ReportScreen`.

---

## Implementation Order (Recommended)

When adding features one by one, this order minimizes risk and maximizes early
impact:

1. `[x] 1` — Android Auto Backup ✓
2. `[x] 2` — Data Export ✓
3. `[x] 3` — Habit Lifecycle States ✓
4. `[x] 4` — Progression Beyond Level 20 ✓
5. `[x] 5` — Year-in-Review Screen ✓
6. `[ ] 9` — Quick-Complete from Notification (daily friction win)
7. `[ ] 10` — Habit Template Library (onboarding improvement)
8. `[ ] 7` — Proactive Weekly Insight Notification (WorkManager, medium complexity)
9. `[ ] 6` — Adaptive Reminder Timing (requires timestamp data collection first)
10. `[ ] 8` — Health Connect Integration (most complex, highest daily value)
11. `[ ] 11` — Mood × Habit Correlation (light analytics enhancement)

---

## Projected Satisfaction After Completion

| After | Score |
|-------|-------|
| Current state | 6.8 / 10 |
| P0 complete (items 1–2) | 7.5 / 10 |
| P1 complete (items 3–5) | 8.2 / 10 |
| P2 complete (items 6–8) | 8.7 / 10 |
| P3 complete (items 9–11) | 9.0 / 10 |

*Score assumes an offline-only, user-managed backup model. Users who are
deliberate about their phone's Google One backup reach the top of this range.
The app's job is to make everything else excellent — backup discipline is the
user's side of the contract.*

---

*Last updated: 2026-04-15*
*Author: vasanthparimalan*
