# HabitPower — Improvements Plan

> **The vision:** HabitPower is not a habit tracker. It is a **personal operating system
> for lifelong human evolution** — the place where a person sits with themselves the way
> a team sits in a sprint review. Weekly. Monthly. Seasonally. Yearly. Across decades.
>
> Great teams don't just do tasks — they review, reflect, recalibrate, and grow together.
> Most people have never once done this for themselves. HabitPower is the tool that
> makes it possible. Not just once. For a lifetime.
>
> **App Philosophy:** Free. Helpful. Built for productive people. No engagement tricks,
> no gamification inflation, no dark patterns. Only genuine utility.

---

## App Identity & Voice
*This governs every word in the app — screens, notifications, onboarding, error messages, templates, everything.*

HabitPower has Indian roots. Its philosophy draws from across the world — James Clear,
Cal Newport, Stoicism, Ayurveda, Yoga, Zen — but the voice that holds it together
is distinctly Indian: warm, wise, direct, and quietly confident. It speaks like a
knowledgeable elder who genuinely wants you to succeed, not a corporate product trying
to keep you engaged.

**The seven characteristics to uphold in every feature and every word:**

| Characteristic | What it means in practice |
|----------------|--------------------------|
| **Polite** | Respectful tone always. Never commanding — "Try this" not "You must". |
| **User-friendly** | The simplest path is always the right path. No hidden features. |
| **Helpful** | Every screen answers: "what does the user actually need right now?" |
| **Open** | No dark patterns. No manipulation. No guilt. Data is the user's, always. |
| **Integrity** | The app does what it says. Honest feedback, not flattery. |
| **Clever & Intuitive** | Surfaces the right thing at the right time. Anticipates, doesn't surprise. |
| **Fast & Performant** | Instant response. No jank. Respect for the user's time is non-negotiable. |

**Tone in practice:**
- Use "practice" not "grind". Use "intention" not "goal" where it fits. Use "well done" not "amazing 🎉🎉🎉".
- Completion messages should feel like encouragement from someone who means it, not a chatbot.
- Error messages should be calm and constructive: "Something didn't save. Try again." — not "Oops! Something went wrong!"
- The app should feel like it was built by someone who uses it every day themselves.

**Honest science — never overstate what the app knows:**
- "21 days to form a habit" is a myth. Research shows 18 to 254 days. Never cite a fixed timeline.
- XP and levels measure practice *occurrence*, not transformation. They are motivational scaffolding — useful early, less important over time. The Sadhana Score and Identity Sentences are closer to truth.
- The app should never claim to know more about the user than it does.

**The Duolingo warning — never cross this line:**
Duolingo began as genuinely educational. It became a streak-anxiety engine with guilt notifications. That drift happened feature by feature, each one justifiable in isolation. Before adding any new engagement mechanism, ask: does this serve the user's growth, or does it serve the app's retention? If the answer is the latter, do not ship it.

---

## What Has Been Built

The app as it stands is a complete, functional personal operating system. Every major system is live.

### Survivability
| Feature | What it does |
|---------|-------------|
| Android Auto Backup | Room DB + DataStore backed up to user's Google account. One-line fix that protects years of data. |
| Data Export (CSV + JSON) | Full export via Storage Access Framework. User picks destination. No server. No permissions beyond storage. |
| Google Drive Sync | WorkManager-based sync. CSV snapshots pushed to Drive on habit completion and app open. Auth via Google Sign-In. |
| `.hpex` Unified Backup | Single-file backup/restore (`.hpex`) replacing the old 3-file approach. Versioned JSON with 9 selectable sections. ID-preserving import with `REPLACE` strategy. Full restore from one tap. |
| Factory Reset | Two-step confirmation wipes all tables, clears DataStore, re-seeds chants, and cancels all reminders. Safe pre-restore state. |
| Crashlytics | Firebase Crashlytics integrated via `CrashLogger` wrapper. Logs user ID, backup events, factory reset, and records non-fatal import exceptions. Silently no-ops without credentials. |

### Habit Lifecycle
| Feature | What it does |
|---------|-------------|
| Lifecycle States: Pause / Retire / Graduate | Habits can be paused (no miss counted), retired (preserved history, removed from tracking), or graduated (moved to the Identity Wall — a quiet celebration of internalized behavior). |
| Habit Inventory | Health audit screen: Thriving / Steady / Struggling / Stale per habit. Over-commitment detection. Retire and Pause actions with honest reframing: "Retiring is wisdom — not failure. You ran an experiment." |

### Gamification & Progress
| Feature | What it does |
|---------|-------------|
| Progression Beyond Level 20 | Levels extend infinitely via formula. Mastery milestones at 25, 30, 40, 50. The curve outlasts the user. |
| Practice Depth Model | Replaces streak anxiety. Depth grows with each session and never resets on a miss. Missing a day adds no depth — it does not erase what was built. Language rewritten throughout: "your practice is waiting" not "protect your streak." |
| Post-Habit Micro-Reflection | After completing a habit, an optional 3-second prompt: how was it? (low / steady / great). Auto-dismisses. Never blocks. Feeds quality data into Sadhana Score and Anchor Habit analytics. |

### Intelligence
| Feature | What it does |
|---------|-------------|
| Sadhana Score | One daily number (0–100) computed from habit completion, sleep, mood, focus sessions, hydration. A holistic signal that makes opening the app a meaningful morning ritual. |
| Identity Sentences | "You are someone who moves every morning." Generated from 14+ day habits at 70%+ completion. These sentences are the app's most emotionally powerful output — what James Clear promised and no app actually delivered until now. |
| Anchor Habit Surfacing | After 21 days of data, the app identifies which single habit, when completed, predicts the most habit completions that day. Shown as a quiet dashboard card: "When you complete Morning Walk, you complete 2.4× more habits that day." |
| Dashboard Intelligence | Bright Spot card (one win even on a hard week), Season Awareness (gentle nudge when consistency drops), Life Balance Map (per-area progress bars), Sunday Insight, Milestone Wins (anchor habits, identity statements, graduated habits). |
| Habit Inventory Health States | Recurrence-aware completion ratios: Thriving (≥80%), Steady (50–79%), Struggling (20–49%), Stale (<20%). Surfaces over-commitment when 7+ habits AND overall rate < 50%. |
| Habit Health Card | Dashboard card that auto-detects habits scheduled ≥4 times in the last 14 days with <50% completion. Surfaces three inline recovery actions per habit: Make it easier (→ edit), Change time (→ edit), Pause it (instant, no navigation). |

### Reflection & Review
| Feature | What it does |
|---------|-------------|
| Year-in-Review | On-demand from Report: totals, streaks, best life area, XP journey, personal headline sentence. Shareable as text. The feature that makes users never want to delete the app. |
| 90-Day Season Review | Every 90 days: anchor habit, most consistent, graduate candidate, retire candidate, one question about the next season. Keeps the system alive across years. |
| Report Depth (sparklines) | Per-habit sparkline dots in the Report screen — 7 most recent days of completion shown as a compact, honest visualization per habit row. |
| Self Standup | The platform feature. A personal review across all time horizons: Weekly → Monthly → Quarterly → Yearly → 5-Year Growth Path → 10-Year Vision → Decisions. Auto-insights per cadence. One deep reflection question per section. Growth projections for each habit across 5 and 10 years ("In 10 years, you'll have completed approximately 3,640 sessions of Morning Meditation"). Collapsible sections. Accessible from a permanent banner on the Dashboard. This is the soul of the platform vision. |

### Practice Modes
| Feature | What it does |
|---------|-------------|
| Focus Section Restructure | Four-card session launcher: Work (Pomodoro), Still (Meditation), Chant (Japa/Mantra), Move (→ Routines). Clean architectural container before content was added. |
| Chant / Japa System | Dedicated mantra practice: tap-to-count, progress ring, midpoint bell, completion sequence. Built-in library (Om, Gayatri, So Hum, Om Mani Padme Hum, Hare Krishna, secular affirmation). Custom chant slots. Sound per-count. Haptic feedback. Session logging. The most culturally distinctive feature in any productivity app. |
| Sound as First-Class Design | Unified session bell across Meditation and Chant. Habit completion tones. Ambient options. All off by default, configurable. The ToneGenerator-based sound system costs zero bytes and requires no audio file permissions. |
| Meditation & Pomodoro | Full session flows with preset configurations. Ambient sound options. Session history. |

### User Experience
| Feature | What it does |
|---------|-------------|
| Missed-Day Welcome | After 2+ days away: "Welcome back. Your practice waited. Let's continue." — no streak loss banner, no missed-days counter. The most important retention feature in the app. Trains users to return, not avoid. |
| Step-Back Mode | Full pause of the entire app — reminders, notifications, all tracking — with a return date. On the return date, one gentle notification. No urgency. No recap of missed days. The app says: "Your life is more important than your streak. We'll be here." |
| Notification Discipline | Hard cap of two active notification channels. Settings shows all channels clearly. The app explicitly limits its own reach — a radical act of trust. |
| Tasks & Checklists | One-off task lists and reusable checklists alongside habits. Widget mode. Vocabulary kept explicitly separate from habits — tasks are not habits. |
| Preloaded Content Library | Ready-to-use routines, habits with importance explanations, meditation sessions, Pomodoro presets. New users start practicing within 5 minutes, not setup sessions. |
| Windows CSV Standardization | ISO 8601 dates, clean column headers, routines export. HabitPower data opens correctly in Excel and Google Sheets without reformatting. |

---

## The Self Standup Meeting Framework

The Self Standup is not a dashboard. It is a structured series of meetings the user has with themselves — each one with a defined type, duration, objective, and commitment. This is what makes it a platform feature rather than an analytics screen.

Every section in the Self Standup follows this structure:
1. **Meeting type** — what kind of review this is (Review, Retrospective, Projection, Vision)
2. **Duration** — how long to spend in this section (not enforced, but stated so the user can sit properly)
3. **Objective** — the one thing this meeting should accomplish
4. **Data + auto-insights** — what the app has computed for this period
5. **Reflection question** — one deep question to sit with before deciding
6. **Commitment** — what the user commits to coming out of this meeting (saved persistently, shown at the next session)

The commitment closes the loop. It is the difference between reviewing your data and actually deciding something. When a user opens the Monthly Retrospective next month, they see what they committed to last time — building personal accountability across cadences.

### Meeting Definitions

| Section | Type | Duration | Objective |
|---------|------|----------|-----------|
| Identity Mirror | Opening Check-in | 2–3 min | Ground yourself in who you are becoming before reviewing what you did |
| Weekly Review | Weekly Review | 10–15 min | See the week clearly. One pattern to carry forward, one to change. |
| Monthly Retrospective | Monthly Retrospective | 20–30 min | Assess habit health. Spot what is thriving and what needs a decision. |
| Season Retrospective | Season Retrospective | 30–45 min | Recalibrate the system. Graduate, retire, set one new intention for the next season. |
| Annual Vision Review | Annual Vision Review | 60–90 min | Anchor the year's effort. See who you became. Set the identity for the year ahead. |
| 5-Year Projection | 5-Year Projection | 15–20 min | Follow the math of your current practices forward. Decide if you like where you are going. |
| 10-Year Vision | 10-Year Vision | 10–15 min | Connect today's habits to the person you are building across a decade. |
| Decisions | Action Items | 5 min | Translate insights into specific next actions. Not someday — this week. |

### The Agile Analogy

| Agile meeting | Self Standup equivalent | Cadence |
|---------------|------------------------|---------|
| Daily standup | Dashboard — Sadhana Score + Anchor Habit | Daily |
| Sprint review | Weekly Review | Weekly |
| Sprint retrospective | Monthly Retrospective | Monthly |
| Quarterly OKR review | Season Retrospective | Every 90 days |
| Annual planning | Annual Vision Review | Yearly |
| Long-range roadmap | 5-Year + 10-Year sections | Ongoing |
| Action items | Decisions section | Each standup |

The Dashboard serves as the daily standup — check your score, see your anchor habit, note any blocker. The Self Standup screen is for the deeper reviews where data is actually analyzed and decisions are made.

---

## Pre-Launch Gate — All Five Complete

All five pre-launch items are shipped. The app is ready for Play Store submission.

---

### [x] 0b. Bug Fix: Navigation Back to Dashboard After Routine or Meditation

**The problem:** After completing or exiting a Routine or Meditation session, the user cannot correctly navigate back to the Dashboard. The back stack is corrupted — the user may be stuck, sent to the wrong screen, or looped. This is a trust-level bug: every user hits it, and it makes the app feel broken at the exact moment they finish a practice.

**Why it must come first:** A completed practice deserves a clean landing. When the app fails to return the user to where they started, the session ends on frustration instead of completion. Navigation correctness is a promise the app makes every time a session ends.

**Why this fits the platform vision:** A personal OS for evolution should never strand the user. Every session should end cleanly, so they can immediately reflect, check their score, or close the app with dignity.

**The fix:** Audit `NavHost` route definitions and all `navController.navigate` calls in `ExecuteRoutineScreen`, `WorkoutRunnerScreen`, and the Focus screens. Correct the `popUpTo` / `inclusive` config so the back stack lands on the existing Dashboard instance — never pushes a new one.

**Scope:** Navigation audit + `popUpTo(Screen.Dashboard.route) { inclusive = false }` in all session-completion paths. No new screens. No new data.

---

### [x] 9. Quick-Complete from Notification

**The problem:** Every day, each user gets reminder notifications for their habits. To mark a habit complete, they must: see the notification → tap to open the app → find the habit → mark it done → close the app. That is 4–5 steps for something that takes 0 steps in real life. The friction is high enough that users miss completions they actually did — not because they didn't practice, but because the logging is too inconvenient.

**Why this makes the biggest daily difference:** This is the single highest-frequency interaction in the app — every habit, every user, every day. Reducing it from 5 steps to 1 tap ("Done" button in the notification itself) is the kind of friction reduction that compounds over a year into hundreds of extra completions logged. More completions → more accurate analytics → more accurate intelligence → more useful Self Standup.

**Why this fits the platform vision:** A personal OS that requires 5 steps to record a data point is failing its user. The system should work for the person, not against them. Quick-Complete makes the platform's data collection as frictionless as the practice itself.

**How it works:** A `BroadcastReceiver` handles the notification action button tap. It writes the `DailyHabitEntry` directly to Room, triggers a local broadcast to refresh the Dashboard widget, and shows a brief system notification confirming completion. The app never opens. The user's lock screen is their interface.

**Scope:** Updated `HabitReminderReceiver` → new `QuickCompleteReceiver` + `NotificationCompat.Action` "Done" button wired to receiver + direct repository write from receiver + widget refresh trigger.

---

### [x] 12. Repeatable Exercises in Routines

**The problem:** The current data model uses a composite primary key of `(routineId, exerciseId)` on the `RoutineExerciseCrossRef` table. This means the same exercise can appear only once per routine. Real training patterns — circuits, supersets, interval sets — require the same movement to appear multiple times in sequence:

> Jump Rope — 500 reps → Push-ups — 3×15 → Jump Rope — 200 reps → Rest

With the current model, this is impossible. Any user who tries to program a real workout hits this wall immediately and cannot use the Routines feature for serious training.

**Why this matters beyond just athletes:** Circuits, AMRAP sets, warm-up / work-set / cooldown patterns with the same movement — these are how real exercise programming works. Locking the model to one exercise per routine makes Routines a demonstration feature, not a tool. It limits the depth of any preloaded workout content and makes the Focus section's "Move" card less useful than it should be.

**Why this fits the platform vision:** A platform for lifelong evolution must grow with the user's fitness level. Beginner routines may be simple. Advanced users need the same structure they'd find in a real training log. Repeatable exercises are the architectural unlock for that growth.

**What changes:**
- Drop the composite PK on `RoutineExerciseCrossRef`. Add a surrogate auto-increment `id` column.
- Add `order: Int`, `sets: Int`, `reps: Int`, `durationSeconds: Int?` per cross-ref row — overrides of the exercise defaults, not stored on the exercise itself.
- `AddEditRoutineScreen`: exercise picker adds a new row (not deduplicated), with inline reps/sets editors per row and drag-to-reorder.
- `WorkoutRunnerScreen` / `ExecuteRoutineScreen`: read the ordered list, display per-row reps/sets.
- DB migration: new version, migrate existing rows with default reps/sets and ascending order values.

**Scope:** DB migration (new schema) + updated DAOs + `AddEditRoutineScreen` rework (per-row reps/sets editors) + runner reads updated model. Estimated 2–3 day effort.

---

### [x] 10. Habit Template Library

**The problem:** A new user opens HabitPower to an empty state. Nothing is pre-loaded beyond demo content. They must create every habit from scratch — name, type, target, frequency, reminder time, life area, identity statement. This is a 3–5 minute process per habit for someone who has never done it before. Most users abandon habit apps in the first session — not because they don't want the habit, but because the setup cost is too high before they see any value.

**Why templates work:** Templates don't replace the user's thinking — they give them a starting point. A user who sees a pre-configured "Morning Walk — 20 min — Body — Daily — 7:00 AM reminder — 'You are someone who moves every morning'" can start tracking in 15 seconds. They can edit anything before saving. The identity statement is pre-written but editable. This is the difference between "I have to set up the whole system before I can use it" and "I can begin right now."

**The Indian voice advantage:** Habit explanations in the template library are written with warmth and genuine care — not clinical bullets. "Morning Water — Starting with 500ml rehydrates your brain before it meets the day." "Reading 20 Pages — 20 pages a day is 18 books a year. Identity-level change." These explanations are why someone chooses a template. They deserve to be written by someone who believes in the practice, not a product manager optimizing for signups.

**Why this fits the platform vision:** Every person who starts the Self Standup with zero habits has nothing to reflect on. Templates solve the cold-start problem so the platform can actually do its job. The first 30 days of practice determine whether there is a year of data to review. Templates are the on-ramp to everything the platform offers.

**Archetypes offered:**
- **Builder** (developer/creator): Deep Work, Ship Something, Read Daily, No Phone First Hour
- **Athlete:** Morning Training, Steps, Hydration, Sleep 7h+
- **Student:** Study Block, Flashcard Review, No Social Media After 10pm
- **Mindful:** Meditation, Gratitude Journal, Walk Outside, Cold Shower
- **Family:** Quality Time, Digital Sunset, Weekly Check-in with someone you love

**Scope:** Static template data object + template picker composable injected into `AddEditHabitScreen`. Templates are pure data — no new DB entities, no new screens beyond the picker. Each template pre-fills all `HabitDefinition` fields; the user edits before saving.

---

### [x] 7. Proactive Weekly Insight Notification

**The problem:** The Report screen already computes this week's most actionable insight — the weakest life area, the current streak at risk, the anchor habit's effect, the consistency rate. It computes all of this perfectly. And then it waits, silently, for the user to open the Report screen. Most users open the Report screen once a week at most. The insight that could have motivated Monday's session sits unused until it's irrelevant.

**Why one Sunday notification changes the math:** The Self Standup, the Season Review, the Year-in-Review — these are all tools the user visits deliberately. But deliberate visits require the user to remember to visit. A single, sharp Sunday notification closes that gap: it brings the most important insight to the user, rather than waiting for them to come to it.

**What makes this notification different from notification spam:**
- It fires once a week, on Sunday evening. That is 52 notifications per year — less than a quarter of what most apps send.
- It carries exactly one insight. Not a summary of the week. One sharp, actionable sentence drawn from the most meaningful signal in the data.
- It is honest, not flattering. "Your weakest area this week was Sleep (38%). One win tomorrow starts the turn." Not "You're doing amazing! Keep it up!"
- It is one of the two notification channels the user chooses. If they don't select the Weekly Insight channel, they never receive it.

**Why this fits the platform vision:** The Self Standup works best when users come to it with already-active awareness of their patterns. A weekly insight notification primes that awareness. It is the system doing its job between sessions — being a genuine operating system, not just a passive data store.

**How it works:** `WeeklyInsightWorker` runs via WorkManager, triggered Sunday evenings. It calls the same computation logic as `ReportViewModel.buildReport()`, selects the single highest-signal insight, and fires a notification on the Weekly Insight channel (only if that channel is enabled in the user's two-channel selection).

**Scope:** `WeeklyInsightWorker.kt` (WorkManager, periodic, Sunday-evening trigger) + insight-selection logic (drawn from existing `ReportViewModel` computation — no new analysis) + notification on existing channel infrastructure.

---

## Set Aside — With Reasons

These items are real improvements. They are set aside not because they are unimportant, but because building them now would be the wrong use of time. Each has a specific reason for its deferral.

---

### Complete App Refactoring

**What:** A ground-up architectural review of the entire codebase — data layer, navigation, ViewModel dependencies, composable structure, and feature alignment with the platform vision. The goal is not to add features but to make the app faster, cleaner, and more internally consistent.

**What this includes:**
- Audit every ViewModel for unnecessary state duplication and redundant Repository calls
- Consolidate the navigation graph — some routes were added incrementally and have inconsistent `popUpTo` behavior (which is also causing Bug 0b)
- Review all DataStore keys for naming consistency and unused entries
- Audit the Room schema for columns that were added during rapid development but may be underused or misaligned
- Review composable composition — identify deep nesting, unnecessary recompositions, and missing `remember` / `derivedStateOf` optimizations
- Ensure the Self Standup data path (which touches all entities) is efficient and not re-computing work already done by DashboardViewModel or ReportViewModel
- Extract shared computation (habit stats, health distribution) into a domain service rather than duplicating it across DashboardViewModel, ReportViewModel, SeasonReviewViewModel, and SelfStandupViewModel

**Why set aside:** The app is functionally complete and this work is invisible to users. Refactoring before shipping creates risk (regressions in working features) without user benefit. The right time for this is after Play Store launch, with a stable codebase and real user feedback clarifying which paths are performance-critical.

**When to pick up:** After Play Store launch. Do this as the first major internal work in v2.0. Treat it as a sprint with no new features — pure quality improvement.

---

### Health Connect Integration (Item 8)
*Auto-fill steps, sleep, and active minutes from wearables and Health Connect.*

**Why set aside:** Health Connect requires the `ACTIVITY_RECOGNITION` and `READ_STEPS` / `READ_SLEEP` permission declarations in the manifest, review of the Health Connect permission rationale UI requirements, and hands-on testing against actual Health Connect data sources (Pixel Watch, Fitbit, Wear OS). This is a 3–4 day integration that can break the app's data integrity if the merge logic between auto-filled and user-logged entries is wrong. The core app should be stable and shipped before adding a wearable data layer.

**When to pick up:** After Play Store launch, when real user feedback clarifies which health data users actually want auto-filled and which they prefer to log manually.

---

### Family Practice Board (Item 28)
*Shared presence without surveillance — per-family-member 7-day practice dots on the Dashboard.*

**Why set aside:** This feature works on multi-user devices — specifically devices shared within a family that all use HabitPower. The privacy model (only dot colors shown, no habit names, no percentages), the UX for profile switching within the board, and the data isolation guarantees all need design review before building. Building this feature incorrectly — in a way that feels like surveillance rather than quiet shared accountability — would be worse than not building it. It deserves its own design session.

**When to pick up:** After Play Store launch, with actual multi-user device testers who can give feedback on the privacy model and shared-presence UX.

---

### Windows Companion App (Item 15)
*Compose Desktop companion app built with Kotlin Multiplatform.*

**Why set aside:** This is a separate 3–4 month project. It requires extracting domain logic (`GamificationEngine`, data models, CSV parser) into a KMP `shared` module, setting up a `desktopMain` source set, and building a Compose Desktop UI. The Drive Sync infrastructure (already built) is the interoperability layer. This should start only after the Android app is stable in production and the Drive-exported CSV format has been validated by real users.

**When to pick up:** After Play Store launch + 3 months of real-world usage data to verify the CSV format is stable before building a Windows reader on top of it.

---

## Play Store — The Final Gate

### [ ] 17. Publish to Google Play Store

Do not publish until items 0b, 9, 12, 10, and 7 are implemented and manually tested end-to-end. A bad first impression on the Play Store is nearly impossible to recover from.

**Pre-submission checklist:**

**1. Release signing**
- Generate a release keystore: `keytool -genkey -v -keystore habitpower-release.jks`
- Add `signingConfigs { release { ... } }` to `app/build.gradle`
- **Critical:** Back up the keystore and passwords — losing them means you can never update the app on the Play Store.
- Enroll in Play App Signing (Google holds a secondary key) for safety.

**2. Build format**
- Play Store requires Android App Bundle (`.aab`), not APK.
- Command: `./gradlew bundleRelease`
- Enable R8 minification: `minifyEnabled = true`, `shrinkResources = true`

**3. Privacy policy (required)**
- All data stored on-device. No network transmission. Simple statement hosted on GitHub Pages or as a GitHub Gist.

**4. Data Safety form**
- Declare: no data collected or shared. Android Auto Backup (Google account) declared under user control.

**5. Store listing assets**
- App icon: 512×512 PNG
- Feature graphic: 1024×500 PNG
- Minimum 2 phone screenshots
- Short description: 80 chars max

**6. Release track strategy**
- Internal Testing → Open Testing (public beta) → Production (full release after 1–2 weeks of stable beta)

**7. Target API compliance**
- App targets SDK 34 — meets current Play Store requirement.
- Verify `SCHEDULE_EXACT_ALARM` permission declaration is correct for SDK 31+ (needed for habit reminders).

**One-time cost:** $25 USD Google Play Developer registration fee.

---

## Current State Summary

| Category | Status |
|----------|--------|
| Survivability (.hpex backup, CSV export, Drive sync, Factory Reset, Crashlytics) | Complete |
| Habit lifecycle (pause, retire, graduate) | Complete |
| Gamification (XP, levels, depth model) | Complete |
| Intelligence (Sadhana Score, Identity Sentences, Anchor Habit, Habit Health) | Complete |
| Dashboard Intelligence (Bright Spot, Life Balance, Milestone Wins) | Complete |
| Practice Modes (Meditation, Pomodoro, Chant, Routines) | Complete |
| User Experience (Missed-Day Welcome, Step-Back, Notification Discipline) | Complete |
| Reflection (Year-in-Review, Season Review, Report Depth) | Complete |
| **Self Standup — the platform feature** | **Complete** |
| Habit Inventory | Complete |
| Tasks & Checklists | Complete |
| Navigation bug (0b) | Complete |
| Quick-Complete from Notification (9) | Complete |
| Repeatable Exercises in Routines (12) | Complete |
| Habit Template Library (10) | Complete |
| Weekly Insight Notification (7) | Complete |
| Health Connect, Family Board, Windows App | Set aside |
| **Play Store Publishing** | **Ready** |

---

*Last updated: 2026-06-13 — v1.7 (versionCode 8). Added .hpex unified backup/restore, factory reset, Habit Health dashboard card, and Firebase Crashlytics. All five pre-launch gate items confirmed complete. App is ready for Play Store submission.*

*Author: vasanthparimalan*
