# AGENTS Guide for funfection app

## Big picture (read this first)
- Single-module Android app (`:app`) with Java-only source under `app/src/main/java/com/kingjoshdavid/funfection`.
- Runtime flow: `ui` fragments collect selections/input -> `engine` builds/combines `Virus` objects -> `data` repositories keep state -> UI renders summaries/details.
- Main entry is `MainActivity` (`app/src/main/java/com/kingjoshdavid/funfection/ui/MainActivity.java`) with bottom-nav tabs: collection, infect, friends. `CombineFragment` is pushed onto the fragment back stack from `CollectionFragment`/`MyVirusActivity` (not a permanent tab); virus creation is a dialog inside `CollectionFragment`.
- Core domain object is immutable `Virus` (`app/src/main/java/com/kingjoshdavid/funfection/model/Virus.java`); updates happen by replacement (`withGeneration`, `incrementGeneration`) instead of mutation.

## Component boundaries that matter
- `ui/` is orchestration only (dialogs, list selection, intents, QR scan/share). Keep rules out of fragments.
- `engine/` owns gameplay logic: seed generation (`VirusFactory`) and merge/mutation (`InfectionEngine`).
- `model/` holds value types and provenance (`VirusOrigin`), including share-code/payload serialization.
- `data/` is local state only: `VirusRepository` (in-memory collection) + `UserProfileRepository` (prefs-backed profile via `SharedPreferencesUtil`). Prefs access is abstracted behind the `PreferencesStore` interface (`SharedPreferencesStore` for Android, `InMemoryPreferencesStore` as a test fallback).

## High-value code paths
- Infection flow: `InfectFragment.prepareAndShowPreview()` -> `VirusFactory.parseInviteCode(...)` fallback to `createRandomFriendVirus()` -> `InfectionEngine.infect(...)` -> `VirusRepository.addVirus(...)`. The local seed is always only the **first** virus in the collection (`myViruses.get(0)`), not a user selection.
- Local combine flow: `CombineFragment.confirmCombine()` -> `InfectionEngine.infectLocal(...)` -> `VirusRepository.addVirus(...)`.
- Lab create flow: `CollectionFragment.promptCreateVirus()` (dialog) -> `VirusFactory.createLabVirus(seed)` -> `VirusRepository.addVirus(...)` -> `openVirusDetails(...)` (`MyVirusActivity`).
- Purge flow: `CollectionFragment.confirmPurge(virus)` / `MyVirusActivity.confirmPurge()` -> `VirusRepository.purgeVirusById(id)`; returns `PurgeResult.BLOCKED_LAST` and refuses deletion when only one virus remains.
- Pinned combine flow: `CollectionFragment.openCombine(virus)` or `MyVirusActivity.openCombine()` -> pushes `CombineFragment.newPinnedInstance(virusId)` onto the back stack. From `MyVirusActivity`, this fires `MainActivity` with `EXTRA_OPEN_COMBINE_VIRUS_ID` + `FLAG_ACTIVITY_CLEAR_TOP`.
- Virus details: `CollectionFragment` passes `MyVirusActivity.EXTRA_VIRUS_ID`; detail screen resolves via `VirusRepository.getVirusById(...)`.
- Provenance/degree tracking is centralized in `VirusOrigin` and travels through share payloads (`Virus.toShareCode()` + `VirusOrigin.toSharePayload()`).

## Project-specific conventions
- Invite/share format is colon-delimited with optional trailing fields: `id:family:i:r:c:mutation:genome:name:carrier:generation[:originPayload]` (see `Virus.toShareCode`, `VirusFactory.parseSingle`). The 10th field is `generation` (lineage depth), not an infection count.
- `name` and `family` are intentionally different: name is flavor text, family is lineage key used by merge logic.
- `Virus.name` is stored internally as two tokens: `prefix` and `suffix`. Combine logic picks `prefix` from the lower-generation parent and `suffix` from the higher-generation parent; `getName()` returns `(prefix + " " + suffix).trim()`.
- `productionContext` is a nullable flavor tag set by the engine: `"Cluster"` (same-family collapse), `"Hybrid"` (cross-family collapse), `"Local Mix"` (`infectLocal`). Displayed as the first line of the origin report in `MyVirusActivity`.
- `genome` is display + deterministic-seed metadata, not authoritative state to parse back from.
- Offspring `generation = max(parentGenerations) + 1`; seeded starters begin at generation `1`. Use `withGeneration(n)` or `incrementGeneration()` to produce updated copies.
- `VirusOrigin.toSharePayload()` is Base64url-encoded (no padding) newline-delimited text, not colon-delimited. Format is versioned (`SHARE_VERSION = "1"`); adding new origin fields requires a version bump and updated `fromSharePayload` parsing.
- `VirusRepository.addVirus` prepends (`index 0`), so newest strains appear first in list UIs.

## Build/test/debug workflows
- Build debug APK: `./gradlew.bat :app:assembleDebug`
- Unit tests: `./gradlew.bat :app:testDebugUnitTest`
- Install on device/emulator: `./gradlew.bat :app:installDebug`
- Build signed bundle: `./gradlew.bat :app:bundleRelease` (requires signing keys in `local.properties`, see `README.md` and `PLAY_STORE_UPLOAD.md`).
- SDK/stack from repo config: AGP `8.12.3`, Gradle `8.14.4`, Java 17, `compileSdk/targetSdk 36`, `minSdk 24` (`app/build.gradle`).
- Deploy (the debug apk by default) to attached devices if there are any unless requested not to.

## External integrations
- QR generation/scanning in infect flow uses `com.journeyapps:zxing-android-embedded` + `com.google.zxing:core` and camera permission (`AndroidManifest.xml`).
- Material components (`com.google.android.material`) drive dialogs and bottom navigation.
- `coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.5'` is required for `java.util.Base64` (used by `VirusOrigin.toSharePayload`) on `minSdk 24`.

## Agent caveats
- Package README files in `ui/` mention `StartActivity`/`start.xml`; current runtime uses `MainActivity` + fragment layouts. Prefer concrete code over stale docs.
- Tests are JVM unit tests only (`app/src/test/...`); no instrumentation test suite is present.
- `VirusRepository` state is process-local static memory; app restart resets collection to seeded starters.
- `FriendsFragment` is a "Coming Soon" placeholder (`fragment_coming_soon.xml`); no social logic is implemented.
- `VirusRepository.purgeVirusById(id)` returns `PurgeResult.BLOCKED_LAST` when only one virus remains; the UI surfaces this with a toast and never deletes the last entry.

