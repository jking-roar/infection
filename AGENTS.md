# AGENTS Guide for funfection app

## Big picture (read this first)
- Single-module Android app (`:app`) with Java-only source under `app/src/main/java/com/kingjoshdavid/funfection`.
- Runtime flow: `ui` fragments collect selections/input -> `engine` builds/combines `Virus` objects -> `data` repositories keep state -> UI renders summaries/details.
- Main entry is `MainActivity` (`app/src/main/java/com/kingjoshdavid/funfection/ui/MainActivity.java`) with bottom-nav tabs: collection, create, combine, infect, friends.
- Core domain object is immutable `Virus` (`app/src/main/java/com/kingjoshdavid/funfection/model/Virus.java`); updates happen by replacement (`withInfectionCount`, `incrementInfectionCount`) instead of mutation.

## Component boundaries that matter
- `ui/` is orchestration only (dialogs, list selection, intents, QR scan/share). Keep rules out of fragments.
- `engine/` owns gameplay logic: seed generation (`VirusFactory`) and merge/mutation (`InfectionEngine`).
- `model/` holds value types and provenance (`VirusOrigin`), including share-code/payload serialization.
- `data/` is local state only: `VirusRepository` (in-memory collection) + `UserProfileRepository` (prefs-backed profile via `SharedPreferencesUtil`).

## High-value code paths
- Infection flow: `InfectFragment.prepareAndShowPreview()` -> `VirusFactory.parseInviteCode(...)` fallback to `createRandomFriendVirus()` -> `InfectionEngine.infect(...)` -> `VirusRepository.incrementInfectionCounts(...)` + `addVirus(...)`.
- Local combine flow: `CombineFragment.confirmCombine()` -> `InfectionEngine.infectLocal(...)` -> `VirusRepository.addVirus(...)`.
- Virus details: `CollectionFragment` passes `MyVirusActivity.EXTRA_VIRUS_ID`; detail screen resolves via `VirusRepository.getVirusById(...)`.
- Provenance/degree tracking is centralized in `VirusOrigin` and travels through share payloads (`Virus.toShareCode()` + `VirusOrigin.toSharePayload()`).

## Project-specific conventions
- Invite/share format is colon-delimited with optional trailing fields: `id:family:i:r:c:mutation:genome:name:carrier:infectionCount[:originPayload]` (see `Virus.toShareCode`, `VirusFactory.parseSingle`).
- `name` and `family` are intentionally different: name is flavor text, family is lineage key used by merge logic.
- `genome` is display + deterministic-seed metadata, not authoritative state to parse back from.
- New offspring counts reset to `1`; parent infection counts are incremented only on committed actions (share/infect), not previews.
- `VirusRepository.addVirus` prepends (`index 0`), so newest strains appear first in list UIs.

## Build/test/debug workflows
- Build debug APK: `./gradlew.bat :app:assembleDebug`
- Unit tests: `./gradlew.bat :app:testDebugUnitTest`
- Install on device/emulator: `./gradlew.bat :app:installDebug`
- Build signed bundle: `./gradlew.bat :app:bundleRelease` (requires signing keys in `local.properties`, see `README.md` and `PLAY_STORE_UPLOAD.md`).
- SDK/stack from repo config: AGP `8.12.3`, Gradle `8.14.4`, Java 17, `compileSdk/targetSdk 36`, `minSdk 24` (`app/build.gradle`).

## External integrations
- QR generation/scanning in infect flow uses `com.journeyapps:zxing-android-embedded` + `com.google.zxing:core` and camera permission (`AndroidManifest.xml`).
- Material components (`com.google.android.material`) drive dialogs and bottom navigation.

## Agent caveats
- Package README files in `ui/` mention `StartActivity`/`start.xml`; current runtime uses `MainActivity` + fragment layouts. Prefer concrete code over stale docs.
- Tests are JVM unit tests only (`app/src/test/...`); no instrumentation test suite is present.
- `VirusRepository` state is process-local static memory; app restart resets collection to seeded starters.

