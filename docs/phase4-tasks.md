# Memly -- Phase 4: Cloud Sync (Firebase) Task Breakdown

## Task Legend

| Symbol | Meaning      |
|--------|--------------|
| ⬜     | Not Started  |
| 🔄     | In Progress  |
| ✅     | Completed    |
| ❌     | Blocked      |

---

## Dependency Map

```
Section 1 (Firebase Setup)
    |
    v
Section 2 (Auth Data Layer)
    |
    v
Section 3 (Auth UI)
    |
    v
Section 4 (Firestore Sync Logic)
    |
    +---> Section 5 (Background Sync)
    |         |
    |         v
    |     Section 6 (Sync Settings & Account)
    |
    v
Section 7 (Security Rules & Testing)
```

Sections are strictly sequential; each depends on the previous.
The app must remain fully functional offline without an account at all times.

---

## Summary Table

| Section | Name                              | Tasks | Complexity | Risk   |
|---------|-----------------------------------|-------|------------|--------|
| 1       | Firebase Project Setup            | 6     | Low        | Medium |
| 2       | Authentication -- Data Layer      | 8     | High       | High   |
| 3       | Authentication -- UI              | 7     | Medium     | Medium |
| 4       | Firestore Schema & Sync Logic     | 10    | High       | High   |
| 5       | Background Sync                   | 7     | Medium     | Medium |
| 6       | Sync Settings & Account Mgmt     | 7     | Medium     | High   |
| 7       | Firestore Security Rules & Testing| 5     | Medium     | Medium |
|         | **Total**                         | **50**|            |        |

---

## Section 1: Firebase Project Setup & Dependencies

**Status:** NOT STARTED

**Risk Notes:** Google Services plugin compatibility with AGP 9. Potential version conflicts between Firebase BOM and existing dependencies.

| Task | Description                                                              | Status | Notes |
|------|--------------------------------------------------------------------------|--------|-------|
| 1.1  | Create Firebase project in Firebase Console                              | ⬜     |       |
| 1.2  | Add google-services.json to app module                                   | ⬜     |       |
| 1.3  | Add Firebase BOM, Auth, Firestore, and Storage dependencies to version catalog | ⬜ |       |
| 1.4  | Add Google Services Gradle plugin                                        | ⬜     |       |
| 1.5  | Add Credential Manager and Google Identity dependencies for Google Sign-In | ⬜   |       |
| 1.6  | Verify project builds with Firebase dependencies, no runtime crashes     | ⬜     |       |

**Checkpoint:** Project compiles and runs with all Firebase dependencies. No runtime crashes on launch. google-services.json is properly loaded.

---

## Section 2: Authentication -- Data Layer

**Status:** NOT STARTED

**Risk Notes:** Credential Manager API complexity on different Android versions. Firebase Auth error mapping to user-friendly messages. CallbackFlow lifecycle management.

| Task | Description                                                              | Status | Notes |
|------|--------------------------------------------------------------------------|--------|-------|
| 2.1  | Create AuthUser data class (uid, email, displayName, photoUrl)           | ⬜     |       |
| 2.2  | Create AuthState sealed class (Authenticated, Unauthenticated, Loading)  | ⬜     |       |
| 2.3  | Create AuthRepository interface with sign-in, sign-out, and delete methods | ⬜   |       |
| 2.4  | Implement AuthRepository using FirebaseAuth (callbackFlow for auth state, map FirebaseUser to AuthUser) | ⬜ | |
| 2.5  | Create GoogleSignInHelper wrapping Credential Manager API                | ⬜     |       |
| 2.6  | Add FirebaseModule to DI (provides FirebaseAuth, FirebaseFirestore, FirebaseStorage) | ⬜ | |
| 2.7  | Add syncEnabled and userId fields to DataStore preferences               | ⬜     |       |
| 2.8  | Verify AuthRepository correctly emits auth state changes                 | ⬜     |       |

**Checkpoint:** AuthRepository emits correct AuthState transitions. Google Sign-In helper returns credentials. DI module provides all Firebase instances.

---

## Section 3: Authentication -- UI

**Status:** NOT STARTED

**Risk Notes:** Medium. Google Sign-In UX flow with Credential Manager can be tricky. Handling configuration changes during sign-in flow.

| Task | Description                                                              | Status | Notes |
|------|--------------------------------------------------------------------------|--------|-------|
| 3.1  | Create AuthScreen with Google Sign-In button and email/password form     | ⬜     |       |
| 3.2  | Create AuthViewModel with form validation and sign-in/sign-up logic      | ⬜     |       |
| 3.3  | Create account toggle (sign in vs create account mode)                   | ⬜     |       |
| 3.4  | Forgot password dialog (Firebase password reset email)                   | ⬜     |       |
| 3.5  | Add account section to SettingsScreen (user info when signed in, sign-in button when not) | ⬜ | |
| 3.6  | Add Screen.Auth route to navigation                                      | ⬜     |       |
| 3.7  | Verify can sign in with Google and email/password, sign out from settings | ⬜     |       |

**Checkpoint:** User can sign in via Google or email/password, see account info in Settings, and sign out. App remains fully functional without signing in.

---

## Section 4: Firestore Schema & Sync Logic

**Status:** NOT STARTED

**Risk Notes:** HIGH. Conflict resolution is complex. Large media uploads may fail or timeout. Firestore security rules must be tested early. Incremental sync requires reliable timestamp tracking.

| Task  | Description                                                              | Status | Notes |
|-------|--------------------------------------------------------------------------|--------|-------|
| 4.1   | Design Firestore document structure (users/{uid}/memories/{memoryId}, tags, collections) | ⬜ | |
| 4.2   | Create SyncRepository interface with upload and download logic           | ⬜     |       |
| 4.3   | Implement memory upload: MemoryEntity + tags + collection refs to Firestore document | ⬜ | |
| 4.4   | Implement memory download: Firestore document to Room entities (handle conflicts by timestamp) | ⬜ | |
| 4.5   | Implement media file upload to Firebase Storage (users/{uid}/media/{hash}) | ⬜    |       |
| 4.6   | Implement media file download from Firebase Storage to local sandbox     | ⬜     |       |
| 4.7   | Create sync status tracking (last sync timestamp in DataStore)           | ⬜     |       |
| 4.8   | Handle sync conflicts: server wins for same memoryId, merge tags         | ⬜     |       |
| 4.9   | Implement incremental sync (only sync memories modified since last sync) | ⬜     |       |
| 4.10  | Verify: create memory offline, sign in, sync, verify data in Firestore console | ⬜ |       |

**Checkpoint:** Full round-trip sync works. Memories created offline appear in Firestore after sync. Downloaded memories from Firestore appear in local Room database. Conflicts resolved by timestamp.

---

## Section 5: Background Sync

**Status:** NOT STARTED

**Risk Notes:** WorkManager constraints may behave differently across OEMs. Battery optimization on some devices can prevent scheduled work from running reliably.

| Task | Description                                                              | Status | Notes |
|------|--------------------------------------------------------------------------|--------|-------|
| 5.1  | Add WorkManager dependency (if not already present)                      | ⬜     |       |
| 5.2  | Create SyncWorker (CoroutineWorker) that runs SyncRepository logic       | ⬜     |       |
| 5.3  | Schedule periodic sync (configurable: every 1h, 6h, 12h, 24h)           | ⬜     |       |
| 5.4  | Add Wi-Fi only constraint option                                         | ⬜     |       |
| 5.5  | Re-schedule sync after device boot (BootReceiver or WorkManager)         | ⬜     |       |
| 5.6  | Show sync status indicator in Settings (last synced time, sync in progress) | ⬜  |       |
| 5.7  | Verify background sync runs on schedule, respects Wi-Fi constraint       | ⬜     |       |

**Checkpoint:** Background sync executes on the configured schedule. Wi-Fi only constraint is respected. Sync status is visible in Settings with last synced timestamp.

---

## Section 6: Sync Settings & Account Management

**Status:** NOT STARTED

**Risk Notes:** Account deletion must be thorough across Firestore, Storage, and Auth. GDPR compliance requires all user data to be removed. Sign-out must not delete local data.

| Task | Description                                                              | Status | Notes |
|------|--------------------------------------------------------------------------|--------|-------|
| 6.1  | Add sync settings section in SettingsScreen (auto-sync toggle, frequency picker, Wi-Fi only) | ⬜ | |
| 6.2  | Manual sync button (trigger immediate sync)                              | ⬜     |       |
| 6.3  | Sync progress indicator (uploading X of Y memories)                      | ⬜     |       |
| 6.4  | Delete account flow: delete all Firestore data + Firebase Storage + Firebase Auth account | ⬜ | |
| 6.5  | Double confirmation for account deletion                                 | ⬜     |       |
| 6.6  | Handle sign-out: stop sync, clear sync state, keep local data            | ⬜     |       |
| 6.7  | Verify all sync settings work, account deletion wipes cloud data         | ⬜     |       |

**Checkpoint:** All sync settings are functional. Manual sync triggers immediately. Account deletion removes all cloud data (Firestore documents, Storage files, Auth account). Sign-out preserves local data.

---

## Section 7: Firestore Security Rules & Testing

**Status:** NOT STARTED

**Risk Notes:** Security rule testing requires Firebase Emulator Suite or manual testing. Edge cases in multi-device sync may surface data integrity issues.

| Task | Description                                                              | Status | Notes |
|------|--------------------------------------------------------------------------|--------|-------|
| 7.1  | Write Firestore security rules (users can only read/write their own data) | ⬜    |       |
| 7.2  | Write Firebase Storage security rules (users/{uid}/ path restriction)    | ⬜     |       |
| 7.3  | Test unauthorized access is denied                                       | ⬜     |       |
| 7.4  | Test sync with two devices (if possible) or verify data integrity round-trip | ⬜  |       |
| 7.5  | Final build and smoke test of all cloud features                         | ⬜     |       |

**Checkpoint:** Security rules deployed and verified. Unauthorized access is blocked. Data integrity confirmed across sync round-trips. All cloud features pass smoke testing.
