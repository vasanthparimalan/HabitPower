# HabitPower — Test Checklist

All tests run **offline** (no network required).
- **T1** — Pure JVM unit tests: `./gradlew test`
- **T2/T3** — Instrumented, in-memory Room DB: `./gradlew connectedAndroidTest`
- **T4** — Compose UI tests on emulator/device: `./gradlew connectedAndroidTest`

Legend: `[ ]` = pending · `[x]` = written · `[✓]` = verified passing

---

## T1 — Basic Functionality (Unit Tests, JVM)
> `app/src/test/java/com/example/habitpower/`

### T1.1 `HabitRecurrenceCalculatorTest`
> File: `reminder/HabitRecurrenceCalculatorTest.kt`

- [✓] `weeklySelectedDays_matchesOnlyChosenWeekdays`
- [✓] `everyNDays_usesAnchorDate`
- [✓] `monthlyByDate_skipsInvalidShortMonthDate`
- [✓] `monthlyNthWeekday_supportsLastWeekdayOfMonth`
- [✓] `yearlyMultiDate_matchesConfiguredDatesOnly`
- [✓] `nextReminderTrigger_returnsNextValidOccurrenceForSkippedMonthlyDate`
- [✓] `daily_withinStartEndDate_scheduled`
- [✓] `daily_beforeStartDate_notScheduled`
- [✓] `daily_afterEndDate_notScheduled`
- [✓] `weeklySelectedDays_noMaskBits_neverScheduled`
- [✓] `weeklySelectedDays_allBitsSet_everyDay`
- [✓] `everyNDays_beforeAnchor_notScheduled`
- [✓] `monthlyByDate_feb29_leapYear`
- [✓] `monthlyByDate_feb29_nonLeapYear`
- [✓] `monthlyNthWeekday_firstWeekdayOfMonth`
- [✓] `monthlyNthWeekday_fifthWeekdayWhenOnly4Exist`
- [✓] `yearlyByDate_matches_correctDate`
- [✓] `nextTrigger_commitmentTimePassedToday_returnsNextOccurrence`
- [✓] `nextTrigger_habitWithEndDatePassed_returnsNull`
- [✓] `nextTrigger_weeklyHabit_skipsUnscheduledDays`
- [✓] `nextTrigger_reminderMinutesZero_exactCommitmentTime`

### T1.2 `DashboardMetricsTest`
> File: `ui/dashboard/DashboardMetricsTest.kt`

- [✓] `consistencyPercentage_countsOnlyScheduledOpportunities`
- [✓] `currentStreak_skipsUnscheduleDaysAndStopsOnMiss`
- [✓] `bestWeeklyPercentage_usesScheduledCountsPerWeek`
- [✓] `buildHeatmap_returnsZeroRatioWhenNothingScheduled`
- [✓] `consistencyPercentage_noHabits_returnsZero`
- [✓] `consistencyPercentage_allMissed_returnsZero`
- [✓] `consistencyPercentage_100percent`
- [✓] `currentStreak_noHabits_returnsZero`
- [✓] `currentStreak_todayIncomplete_doesNotCount`
- [✓] `currentStreak_multipleHabits_allMustComplete`
- [✓] `currentStreak_30dayStreak`
- [✓] `buildHeatmap_allComplete_ratio1f`
- [✓] `buildHeatmap_halfComplete_ratio0point5`
- [✓] `bestWeeklyPercentage_singlePerfectWeek`
- [✓] `scheduledHabitIdsForDate_returnsOnlyScheduledIds`

### T1.3 `HabitHealthDetectionTest`
> File: `ui/dashboard/HabitHealthDetectionTest.kt`

- [✓] `belowThreshold_lessThan4Scheduled_notFlagged`
- [✓] `belowThreshold_exactly50percent_notFlagged`
- [✓] `flagged_0percentOf4scheduled`
- [✓] `flagged_25percentOf8scheduled`
- [✓] `notFlagged_51percentOf8scheduled`
- [✓] `pausedHabit_excluded`
- [✓] `graduatedHabit_excluded`
- [✓] `completionPercent_calculatesCorrectly`
- [✓] `isEntryCompleted_boolean_trueOnlyWhenTrue`
- [✓] `isEntryCompleted_numeric_anyValueCounts`
- [✓] `isEntryCompleted_text_blankNotCompleted`

---

## T2 — Interconnections (Instrumented, in-memory Room DB)
> `app/src/androidTest/java/com/example/habitpower/`

### T2.1 `UserDaoTest`
> File: `data/UserDaoTest.kt`

- [x] `insertUser_retrieveById`
- [x] `updateUser_nameChange_persisted`
- [x] `deleteUser_entriesCascadeDeleted`
- [x] `deleteUser_assignmentsCascadeDeleted`
- [x] `multipleUsers_getAllReturnsAll`

### T2.2 `HabitTrackingDaoTest`
> File: `data/HabitTrackingDaoTest.kt`

- [x] `insertHabit_retrieveById`
- [x] `upsertAssignment_habitAppearsInUserList`
- [x] `removeAssignment_habitDisappearsFromUserList`
- [x] `upsertDailyEntry_then_replaceOnSecondUpsert`
- [x] `deleteDailyEntry_entryGone`
- [x] `insertAllEntries_bulkInsert500`
- [x] `getEntriesForUserInRange_boundaryDatesIncluded`
- [x] `getEntriesForUserInRange_excludesOtherUser`
- [x] `getDailyHabitItems_returnsNullEntryForUncompletedHabit`

### T2.3 `LifeAreaDaoTest`
> File: `data/LifeAreaDaoTest.kt`

- [x] `insertLifeArea_retrieveAll`
- [x] `assignLifeAreaToUser_appearsInUserList`

### T2.4 `RepositoryIntegrationTest`
> File: `data/RepositoryIntegrationTest.kt`

- [x] `createUser_saveActiveUserId_resolvedUserCorrect`
- [x] `createHabit_assignToUser_appearsInDailyItems`
- [x] `toggleBooleanHabit_on_entryCreated`
- [x] `toggleBooleanHabit_off_entryDeleted`
- [x] `saveDailyHabitEntry_numeric_persisted`
- [x] `saveDailyHabitEntry_text_persisted`
- [x] `updateEntryQuality_persisted`
- [x] `setHabitLifecycle_paused_excludedFromDailyItems`
- [x] `setHabitLifecycle_graduated_appearsInGraduatedList`
- [x] `resetAllData_allTablesEmpty`
- [x] `clearForRestore_tablesEmpty`
- [x] `importBulkEntries_allPersisted`
- [x] `getHabitStreak_7dayDailyHabit`
- [x] `getHabitStreak_breaksOnMiss`

---

## T3 — Feature by Feature (Instrumented, in-memory Room DB)
> `app/src/androidTest/java/com/example/habitpower/`

### T3.1 `HpexBackupManagerTest`
> File: `data/HpexBackupManagerTest.kt`

- [x] `export_allSections_validJson`
- [x] `export_sectionSubset_onlyRequestedSectionsPresent`
- [x] `roundTrip_users_exactNameAndIdPreserved`
- [x] `roundTrip_habitDefinition_allRecurrenceFieldsPreserved`
- [x] `roundTrip_habitEntries_allDatesAndValuesPreserved`
- [x] `roundTrip_userHabitAssignments_preserved`
- [x] `roundTrip_userLifeAreaAssignments_preserved`
- [x] `roundTrip_activeUserRestored`
- [x] `import_twice_noDuplicates`
- [x] `import_invalidJson_throwsException`
- [x] `import_versionZero_throwsWithMessage`
- [x] `import_missingUsersSection_graceful`
- [x] `import_builtInChantsReseeded`
- [x] `import_customChants_preserved`
- [x] `import_routinesWithExercises_crossRefsPreserved`
- [x] `import_tasks_allItemsPreserved`

### T3.2 `HabitLifecycleTest`
> File: `data/HabitLifecycleTest.kt`

- [x] `activeHabit_appearsInDailyItems`
- [x] `pausedHabit_hiddenFromDailyItems`
- [x] `pausedHabit_reactivated_reappearsInDailyItems`
- [x] `retiredHabit_hiddenFromAllTracking`
- [x] `graduatedHabit_inGraduatedList_notInDaily`
- [x] `isActiveFalse_hiddenRegardlessOfLifecycle`

### T3.3 `DailyEntryTest`
> File: `data/DailyEntryTest.kt`

- [x] `toggleBoolean_noEntry_creates`
- [x] `toggleBoolean_existingTrue_deletes`
- [x] `saveNumericEntry_stored`
- [x] `saveTextEntry_stored`
- [x] `saveRoutineEntry_stored`
- [x] `updateQuality_onlyQualityUpdated_otherFieldsUnchanged`
- [x] `saveEntry_wrongDate_doesNotAppearInTodayQuery`
- [x] `completeRoutineLinkedHabits_allLinkedHabitsCompleted`

### T3.4 `StreakCalculationTest`
> File: `data/StreakCalculationTest.kt`

- [x] `streak_zero_noEntries`
- [x] `streak_7consecutive_daily`
- [x] `streak_breaksOnSingleMiss`
- [x] `streak_pausedDaysDoNotBreak`
- [x] `streak_numericHabit_targetMet`
- [x] `streak_numericHabit_noEntry_breaks`

### T3.5 `UserManagementTest`
> File: `data/UserManagementTest.kt`

- [x] `createMultipleUsers_switchActive_itemsChangePerUser`
- [x] `deleteUser_assignmentsGone`
- [x] `deleteUser_entriesGone`

### T3.6 `TaskAndChecklistTest`
> File: `data/TaskAndChecklistTest.kt`

- [x] `createList_addTasks_allAppear`
- [x] `toggleTask_completionStateChanges`
- [x] `deleteTask_goneFromList`
- [x] `deleteList_tasksCascadeDeleted`
- [x] `createChecklist_addItems_resetChecklist`

### T3.7 `ChantTest`
> File: `data/ChantTest.kt`

- [x] `seedChantsIfNeeded_populatesBuiltIns`
- [x] `seedChantsIfNeeded_idempotent`
- [x] `insertCustomChant_appearsInAllChants`
- [x] `deleteCustomChant_removed`
- [x] `insertChantSession_retrievedByUser`

### T3.8 `WorkoutAndHealthTest`
> File: `data/WorkoutAndHealthTest.kt`

- [x] `insertWorkoutSession_retrieveByDate`
- [x] `deleteWorkoutSession_gone`
- [x] `saveDailyStat_retrieveByDate`
- [x] `getStatsForDateRange_boundariesIncluded`

### T3.9 `HabitOnHoldTest`
> File: `data/HabitOnHoldTest.kt`

- [✓] `putOnHold_timeBound_setsCorrectFields`
- [✓] `putOnHold_indefinite_setsCorrectFields`
- [✓] `timeBoundHold_hiddenFromDailyItems`
- [✓] `indefiniteHold_hiddenFromDailyItems`
- [✓] `resumeFromHold_restoresActiveState`
- [✓] `resumedHabit_reappearsInDailyItems`
- [✓] `getOnHoldHabits_timeBoundOnly_excludesIndefinitePause`
- [✓] `getOnHoldHabits_sortedByReturnDate`
- [✓] `autoResume_expiredHold_resumesHabit`
- [✓] `autoResume_holdEndingToday_notResumedYet`
- [✓] `autoResume_futureHold_notResumed`
- [✓] `autoResume_indefiniteHold_neverAutoResumes`

### T3.10 `DatabaseMigrationTest` *(expand existing)*
> File: `data/HabitPowerDatabaseMigrationTest.kt`

- [✓] `migrate11To12_backfillsRecurrenceDefaults` *(pre-existing, verified)*
- [✓] `migrate11To12_preservesExistingHabitCoreFields` *(pre-existing, verified)*
- [x] `currentSchema_matchesLatestVersion`
- [x] `inMemoryDatabase_createsAndDestroys_withoutError`

---

## T4 — UI Smoke Tests (Compose, emulator required)
> `app/src/androidTest/java/com/example/habitpower/ui/`

### T4.1 `AppLaunchUiTest`
> File: `ui/AppLaunchUiTest.kt`

- [x] `appLaunches_dashboardVisible`
- [x] `adminScreen_navigatesAndDisplaysTitle`
- [x] `adminResetDialog_appearsOnButtonTap`

---

## Totals

| Tier | Verified `[✓]` | Written `[x]` | Total |
|---|---|---|---|
| T1 — Unit (JVM) | 21 | 0 | 21 |
| T2 — DAO/Repo wiring | 0 | 16 | 16 |
| T3 — Feature flows | 14 | 44 | 58 |
| T4 — UI smoke | 0 | 3 | 3 |
| **All** | **35** | **63** | **98** |

> **Next step:** Connect an Android emulator and run `./gradlew connectedAndroidTest` to promote `[x]` → `[✓]`.
> Remember to set **Developer options → Window/Transition/Animator scale → 0x** before running T4 UI tests.

---

*Last updated: 2026-06-13*
