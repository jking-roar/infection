# funfection

Infect your friends - with fun.

## Android Studio Setup

This project now includes a Gradle-based Android Studio configuration while keeping the original legacy source layout.

Open the repository root in Android Studio and let it import the Gradle project.

Project details:

- Module: app
- Namespace and applicationId: com.example.funfection
- compileSdk: 34
- targetSdk: 34
- minSdk: 11
- Gradle wrapper: 8.7
- Android Gradle Plugin: 8.5.2

The Gradle module uses sourceSets to point at the existing project files:

- Java sources: src/
- Resources: res/
- Manifest: AndroidManifest.xml

Local machine requirements:

- JDK 17 or newer. JDK 21 is known to work.
- An Android SDK installation with Android API 34 available.

Useful commands:

- ./gradlew --version
- ./gradlew tasks
- ./gradlew assembleDebug
