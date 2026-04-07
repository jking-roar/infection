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

- `StartActivity` (`app/src/main/java/com/example/funfection/ui/StartActivity.java`)
  - Main lab screen for selecting strains, pasting invite codes, infecting, and sharing.
  - Backed by layout: `app/src/main/res/layout/start.xml`.
- `MyVirusActivity` (`app/src/main/java/com/example/funfection/ui/MyVirusActivity.java`)
  - Details screen for one virus (name, stats, genome, family, origin).
  - Backed by layout: `app/src/main/res/layout/my_virus.xml`.

## Project structure

Top-level:

- `app/` - Android application module.
- `gradle/` - Gradle wrapper files.
- `build.gradle`, `settings.gradle`, `gradle.properties` - project-level Gradle config.
- `README.md`, `TODO.md` - project docs.

Inside `app/src/main/java/com/example/funfection/`:

- `ui/` - Activities and user-flow orchestration.
- `engine/` - Strain generation, invite parsing, and combination logic.
- `model/` - Core domain types (`Virus`, stat value objects, infection rate mapping).
- `data/` - In-memory repository (`VirusRepository`).

Inside `app/src/main/`:

- `AndroidManifest.xml` - app entry points and activity registration.
- `res/layout/` - XML layouts (`start.xml`, `my_virus.xml`).
- `res/values/` - strings, colors, themes.

Tests:

- `app/src/test/java/com/example/funfection/`
  - `engine/` tests for infection/combination behavior.
  - `model/` tests for value objects and mapping behavior.
  - `data/` tests for repository behavior.

## How to navigate the code quickly

- UI behavior: start in `ui/` (`StartActivity`, `MyVirusActivity`).
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
- Namespace/applicationId: `com.example.funfection`
- compileSdk: `36`
- targetSdk: `36`
- minSdk: `24`
- Android Gradle Plugin: `8.5.2`
- Gradle wrapper: `8.7`

## Build locally

From the repository root (`C:\code\github\infection`) on Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

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

You can also run from Android Studio:

- Open the project.
- Select your connected device.
- Run the `app` configuration.

## Notes

- Repository state is in-memory only; restarting the app resets the collection.
- A blank invite input still produces an offspring strain by generating a seeded friend strain.
- Package-level docs are available in:
  - `app/src/main/java/com/example/funfection/ui/README.md`
  - `app/src/main/java/com/example/funfection/engine/README.md`
  - `app/src/main/java/com/example/funfection/model/README.md`
  - `app/src/main/java/com/example/funfection/data/README.md`
