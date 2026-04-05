# funfection

Infect your friends - with fun.

## Android Studio Setup

This project is now organized as a standard Gradle-based Android Studio application.

Open the repository root in Android Studio and let it import the Gradle project.

Project details:

- Module: app
- Namespace and applicationId: com.example.funfection
- compileSdk: 34
- targetSdk: 34
- minSdk: 21
- Gradle wrapper: 8.7
- Android Gradle Plugin: 8.5.2

Project structure:

- App module: app/
- Manifest: app/src/main/AndroidManifest.xml
- Java sources: app/src/main/java/
- Resources: app/src/main/res/

Local machine requirements:

- JDK 17 or newer. JDK 21 is known to work.
- An Android SDK installation with Android API 34 available.

Useful commands:

- ./gradlew --version
- ./gradlew tasks
- ./gradlew assembleDebug
