# AGENTS Guide for funfection app

## Big picture (read this first)
- Single-module Android app (`:app`) with Java-only source under `app/src/main/java/com/kingjoshdavid/funfection`.
- Runtime flow: `ui` fragments collect selections/input -> `engine` builds/combines `Virus` objects -> `data` repositories keep state -> UI renders summaries/details.
- Main entry is `MainActivity` (`app/src/main/java/com/kingjoshdavid/funfection/ui/MainActivity.java`) with bottom-nav tabs: collection, infect, friends. Local combine is opened as a right-side drawer inside `CollectionFragment` (`openCombine(...)` -> `executeCombine(...)`), not a permanent tab; virus creation is a dialog inside `CollectionFragment`.
- Core domain object is immutable `Virus` (`app/src/main/java/com/kingjoshdavid/funfection/model/Virus.java`); updates happen by replacement (`withGeneration`, `incrementGeneration`) instead of mutation.

## Component boundaries that matter
- `ui/` is orchestration only (dialogs, list selection, intents, QR scan/share). Keep rules out of fragments.
- `engine/` owns gameplay logic: seed generation (`VirusFactory`) and merge/mutation (`InfectionEngine`). `SeedUtil.seedFromString(String)` provides the shared FNV-1a+Murmur3 hash used for deterministic seeding. `VirusCombinationDemo` is a standalone console demo — not wired into the app runtime.
- `model/` holds value types and provenance (`VirusOrigin`), including share-code/payload serialization. Virus stats use typed `ViralStat` subclasses: `Infectivity.rate(n)` (spread), `Resilience.of(n)` (survivability), `Chaos.level(n)` (instability); access raw value via `.score()`. `Friend` and `UsernameHistoryEntry` are immutable value types for the friends list. `MadScientist` is a named scientist persona used as a simulated-friend stand-in during random seeding. `UserProfile` carries the local player's `id` and `userName`.
- `data/` is local state only: `VirusRepository` + `FriendsRepository` persist through Room (`data/local/` with `FunfectionDatabase`, `VirusDao`, `FriendDao`, `FriendUsernameHistoryDao`, `FriendVirusDao`, and the `FriendVirusCrossRef` join table) and fall back to static in-memory lists when Room is not initialized (notably JVM tests). `UserProfileRepository` remains prefs-backed via `SharedPreferencesUtil`, with prefs access abstracted behind `PreferencesStore` (`SharedPreferencesStore` for Android, `InMemoryPreferencesStore` as a test fallback). `AppSettingsRepository` is also prefs-backed (via `SharedPreferencesUtil`) and owns night-mode (`SYSTEM`/`LIGHT`/`NIGHT`) and the show-self-in-friends-list toggle.

## High-value code paths
- Infection flow: `InfectFragment.prepareAndShowPreview()` -> `VirusFactory.parseInviteCode(...)` fallback to `createRandomFriendVirus()` -> `InfectionEngine.infect(...)` -> `VirusRepository.addVirusAsync(...)`. The local seed is always only the **first** virus in the collection (`myViruses.get(0)`), not a user selection.
- Local combine flow: `CollectionFragment.executeCombine()` -> `InfectionEngine.infectLocal(...)` -> `VirusRepository.addVirusAsync(...)` -> `openVirusDetails(...)` (`MyVirusActivity`).
- Lab create flow: `CollectionFragment.promptCreateVirus()` (dialog) -> `VirusFactory.createLabVirus(seed)` -> `VirusRepository.addVirusAsync(...)` -> `openVirusDetails(...)` (`MyVirusActivity`).
- Purge flow: `CollectionFragment.confirmPurge(virus)` / `MyVirusActivity.confirmPurge()` first checks `VirusRepository.getPurgeStatus(...)`, then calls `VirusRepository.purgeVirusById(...)`; current repository behavior returns `REMOVED`/`MISSING` in active paths.
- Pinned combine flow: `CollectionFragment.openCombine(virus)` opens the combine drawer with that virus pinned. From `MyVirusActivity`, `openCombine()` starts `MainActivity` with `EXTRA_OPEN_COMBINE_VIRUS_ID` + `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`; `MainActivity.openPinnedCombineIfRequested(...)` then launches `CollectionFragment.newCombineInstance(virusId)` so the drawer opens on resume.
- Virus details: `CollectionFragment` passes `MyVirusActivity.EXTRA_VIRUS_ID`; detail screen resolves via `VirusRepository.getVirusById(...)`.
- Provenance/degree tracking is centralized in `VirusOrigin` and travels through share payloads (`Virus.toShareCode()` + `VirusOrigin.toSharePayload()`).
- Friends list flow: `FriendsFragment` calls `FriendsRepository.getFriendsAsync()` -> populates `VectorListAdapter`; tapping a row opens `FriendDetailsActivity` (via `EXTRA_FRIEND_ID`), which loads the friend via `FriendsRepository.getFriendByIdAsync()` and associated viruses via `VirusRepository.getVirusesByFriendIdAsync()`.
- Friend auto-discovery: `VirusRepository.addVirus(virus)` automatically calls `FriendsRepository.recordDiscovery(virus)`, which extracts `VirusOrigin` source/patientZero entries and upserts matching `Friend` records (skipping the current user and filtering non-real sources unless they match a known scientist).
- Settings flow: Settings `ImageButton` in `MainActivity` top bar -> `showFragment(new SettingsFragment())`. `SettingsFragment` reads/writes username via `UserProfileRepository.updateUserName()` (then calls `FriendsRepository.ensureCurrentUserFriendExists()` to sync the self-friend entry), night mode via `AppSettingsRepository.setNightMode()` + `AppCompatDelegate`, and the show-self toggle via `AppSettingsRepository.setCurrentUserVisibleInFriendsList()`.

## Project-specific conventions
- Invite/share format is colon-delimited with optional trailing fields: `id:family:i:r:c:mutation:genome:name:carrier:generation[:originPayload]` (see `Virus.toShareCode`, `VirusFactory.parseSingle`). The 10th field is `generation` (lineage depth), not an infection count.
- `name` and `family` are intentionally different: name is flavor text, family is lineage key used by merge logic.
- `Virus.name` is stored internally as two tokens: `prefix` and `suffix`. Combine logic picks `prefix` from the lower-generation parent and `suffix` from the higher-generation parent; `getName()` returns `(prefix + " " + suffix).trim()`.
- `productionContext` is a nullable flavor tag set by the engine: `"Cluster"` (same-family collapse), `"Hybrid"` (cross-family collapse), `"Local Mix"` (`infectLocal`). Displayed as the first line of the origin report in `MyVirusActivity`.
- `genome` is display + deterministic-seed metadata, not authoritative state to parse back from.
- Offspring `generation = max(parentGenerations) + 1`; seeded starters begin at generation `1`. Use `withGeneration(n)` or `incrementGeneration()` to produce updated copies.
- `VirusOrigin.toSharePayload()` is Base64url-encoded (no padding) newline-delimited text, not colon-delimited. Format is versioned (`SHARE_VERSION = "1"`); adding new origin fields requires a version bump and updated `fromSharePayload` parsing.
- `VirusRepository.addVirus` prepends (`index 0`), so newest strains appear first in list UIs.
- Newest-first ordering is preserved in both repository modes: in-memory fallback prepends (`index 0`), and Room reads from `VirusDao.getAll()` with `ORDER BY created_at DESC`.
- `Friend` carries `displayName`, `displayNameOverride`, `notes`, `description`, `protectedProfile`, `usernameHistory` (`List<UsernameHistoryEntry>`), and `lastInfectionAt`. Use `getEffectiveDisplayName()` (returns override if non-empty, else `displayName`). `notes` is always blank for protected profiles (`isProtectedProfile() == true`).
- Five pre-seeded protected friends ("known scientists") are inserted by `FriendsRepository.initialize()`: Professor Tesla, Doctor Curie, Doc Brown, Professor Xavier, The Gutter Man. Protected friends cannot be deleted.
- The local player is maintained as a protected friend entry via `FriendsRepository.ensureCurrentUserFriendExists()`; call this after any username change.
- Friends list sort order: non-protected friends first, then by `lastInfectionAt` descending; ties preserve insertion/persistence order.
- `FriendVirusCrossRef` (join table `friend_virus_cross_ref`) links viruses to their originating friends; populated automatically when a virus is added. Query via `VirusRepository.getVirusesByFriendId(friendId)`.

## Build/test/debug workflows
- Build debug APK: `./gradlew.bat :app:assembleDebug`
- Unit tests: `./gradlew.bat :app:testDebugUnitTest`
- Install on device/emulator: `./gradlew.bat :app:installDebug`
- Build signed bundle: `./gradlew.bat :app:bundleRelease` (requires signing keys in `local.properties`, see `README.md` and `PLAY_STORE_UPLOAD.md`).
- SDK/stack from repo config: AGP `8.12.3`, Gradle `8.14.4`, Java 17, `compileSdk/targetSdk 36`, `minSdk 24` (`app/build.gradle`).
- Deploy (the debug apk by default) to attached devices if there are any unless requested not to.
- Room-backed repository tests (`VirusRepositoryRoomTest`, `FriendsRepositoryRoomTest`) live in `app/src/test/...` and use Robolectric (`org.robolectric:robolectric:4.13`) + `androidx.test:core` with `ApplicationProvider.getApplicationContext()`. Test setup/teardown is handled by `RepositoryRoomTestSupport`.

## External integrations
- QR generation/scanning in infect flow uses `com.journeyapps:zxing-android-embedded` + `com.google.zxing:core` and camera permission (`AndroidManifest.xml`).
- Material components (`com.google.android.material`) drive dialogs and bottom navigation.
- `coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.5'` is required for `java.util.Base64` (used by `VirusOrigin.toSharePayload`) on `minSdk 24`.
- Local persistence uses Room (`androidx.room:room-runtime:2.6.1` + `room-compiler`) for `VirusRepository` and `FriendsRepository`.

## Agent caveats
- Package README files in `ui/` mention `StartActivity`/`start.xml`; current runtime uses `MainActivity` + fragment layouts. Prefer concrete code over stale docs.
- Tests are JVM unit tests only (`app/src/test/...`); no instrumentation test suite is present. Room-backed tests run via Robolectric — they are still local JVM tests, not device tests.
- `VirusRepository`/`FriendsRepository` persist through Room in app runtime; static in-memory collections are fallback behavior when Room is not initialized (common in local JVM tests).
- `FriendsFragment` is fully implemented — it shows the live friends list backed by `FriendsRepository`. It is no longer a placeholder.
- `CombineFragment` currently has no runtime wiring from `MainActivity`; active combine UX is the drawer flow inside `CollectionFragment`.
- `PurgeResult.BLOCKED_LAST` is still present in enum/UI branches, but repository methods currently do not emit it in active code paths.
