# funfection

Infect your friends - with fun.

`funfection` is a small Android app where you combine virus strains, generate a new offspring strain, and view its stats.

## What the app does

- Lets you select one or more local virus strains.
- Accepts optional friend invite/share codes.
- Combines local + friend strains through `InfectionEngine.infect(...)`.
- Shows an offspring result and stores it in the in-memory collection.
- Opens a details screen for the selected virus.

## App screens

- `MainActivity` (`app/src/main/java/com/kingjoshdavid/funfection/ui/MainActivity.java`)
  - Hosts the main flow (combine, infect, create, collection/friends tabs) via fragments.
  - Backed by layout: `app/src/main/res/layout/activity_main.xml` and fragment layouts under `app/src/main/res/layout/`.
- `MyVirusActivity` (`app/src/main/java/com/kingjoshdavid/funfection/ui/MyVirusActivity.java`)
  - Details screen for one virus (name, stats, genome, family, origin).
  - Backed by layout: `app/src/main/res/layout/my_virus.xml`.

## Project structure

Top-level:

- `app/` - Android application module.
- `gradle/` - Gradle wrapper files.
- `build.gradle`, `settings.gradle`, `gradle.properties` - project-level Gradle config.
- `README.md`, `TODO.md` - project docs.

Inside `app/src/main/java/com/kingjoshdavid/funfection/`:

- `ui/` - Activities, fragments, and user-flow orchestration.
- `engine/` - Strain generation, invite parsing, and combination logic.
- `model/` - Core domain types (`Virus`, stat value objects, infection rate mapping).
- `data/` - In-memory repository (`VirusRepository`).

Inside `app/src/main/`:

- `AndroidManifest.xml` - app entry points and activity registration.
- `res/layout/` - XML layouts (`activity_main.xml`, fragment layouts, `my_virus.xml`).
- `res/values/` - strings, colors, themes.

Tests:

- `app/src/test/java/com/kingjoshdavid/funfection/`
  - `engine/` tests for infection/combination behavior.
  - `model/` tests for value objects and mapping behavior.
  - `data/` tests for repository behavior.

## How to navigate the code quickly

- UI behavior: start in `ui/` (`MainActivity`, `MyVirusActivity`).
- Infection rules: go to `engine/InfectionEngine.java` and related factory classes.
- Virus fields and score semantics: inspect `model/`.
- Seed data and collection state: inspect `data/VirusRepository.java`.
- Resource text and visual tweaks: `app/src/main/res/values/` + `app/src/main/res/layout/`.
- Feature verification: use matching package under `app/src/test/java/...`.

## Local development requirements

- JDK 17+
- ~~Android Studio (current stable)~~ IntelliJ used for dev consider install studio?
- Android SDK installed for API 36
- An Android device running API 24+ (or emulator)

Current project settings:

- Module: `app`
- Namespace/applicationId: `com.kingjoshdavid.funfection`
- compileSdk: `36`
- targetSdk: `36`
- minSdk: `24`
- Android Gradle Plugin: `8.12.3`
- Gradle wrapper: `8.14.4`

## Preflight checks (recommended)

From the repository root (`C:\code\github\infection`) on Windows PowerShell:

```powershell
.\gradlew.bat -v
adb version
adb devices
```

Also verify `local.properties` has a valid SDK path (`sdk.dir=...`).

## Build locally

From the repository root (`C:\code\github\infection`) on Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Build a signed bundle for Google Play

The Play Console upload artifact is an Android App Bundle (`.aab`) built from the `release` variant.

1. Ensure your upload keystore exists locally (this repo currently expects it at `app/keystore/funfection.jks`).
2. Add signing values to `local.properties` (already ignored by git):

```properties
KEYSTORE_FILE=keystore/funfection.jks
KEYSTORE_STORE_PASSWORD=your_store_password
KEYSTORE_KEY_ALIAS=your_key_alias
KEYSTORE_KEY_PASSWORD=your_key_password
```

3. Build the signed release bundle:

```powershell
.\gradlew.bat :app:bundleRelease
```

4. Upload this file in Google Play Console:

- `app/build/outputs/bundle/release/app-release.aab`

5. (Recommended) Confirm the file exists before upload:

```powershell
Get-Item .\app\build\outputs\bundle\release\app-release.aab | Select-Object FullName, Length, LastWriteTime
```

Notes:

- The keystore file (`*.jks`) should not be committed to GitHub.
- `local.properties` is ignored by git and is the correct place for local signing secrets.
- Increment `versionCode` in `app/build.gradle` before each new Play upload.

## Deploy to a physical Android phone

1. On your phone, enable **Developer options** and **USB debugging**.
2. Connect phone by USB and accept the debugging authorization prompt.
3. Verify Android SDK platform-tools (`adb`) are installed.
4. Build and install debug APK:

```powershell
.\gradlew.bat :app:installDebug
```

If multiple devices are connected, pick one device ID and install with `adb`:

```powershell 
adb devices
adb -s <device-id> install -r .\app\build\outputs\apk\debug\app-debug.apk
```

If install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall the existing package first and retry:

```powershell
adb -s <device-id> uninstall com.kingjoshdavid.funfection
.\gradlew.bat :app:installDebug
```

You can also run from Android Studio:

- Open the project.
- Select your connected device.
- Run the `app` configuration.

## Notes

- Repository state is in-memory only; restarting the app resets the collection.
- A blank invite input still produces an offspring strain by generating a seeded friend strain.
- Virus display names and families are intentionally separate: the name is flavor text for a specific strain, while the family is the lineage label used by combine logic and UI grouping.
- Genome strings are display-oriented fingerprints used for deterministic mutation behavior and flavor text; they are not treated as a parseable source of truth for stats.
- Package-level docs are available in:
  - `app/src/main/java/com/kingjoshdavid/funfection/ui/README.md`
  - `app/src/main/java/com/kingjoshdavid/funfection/engine/README.md`
  - `app/src/main/java/com/kingjoshdavid/funfection/model/README.md`
  - `app/src/main/java/com/kingjoshdavid/funfection/data/README.md`
