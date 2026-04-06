# funfection

Infect your friends - with fun.

## Code Compartments

The app is now split into small package-level compartments so UI code, domain logic, and in-memory state stop bleeding into one another.

- `app/src/main/java/com/example/funfection/ui/` contains Android activities and screen orchestration.
- `app/src/main/java/com/example/funfection/engine/` contains virus generation, invite parsing, and infection combination rules.
- `app/src/main/java/com/example/funfection/model/` contains the virus entity and stat value types.
- `app/src/main/java/com/example/funfection/data/` contains the in-memory repository used by the screens.

Each compartment also has its own README with more detail:

- `app/src/main/java/com/example/funfection/ui/README.md`
- `app/src/main/java/com/example/funfection/engine/README.md`
- `app/src/main/java/com/example/funfection/model/README.md`
- `app/src/main/java/com/example/funfection/data/README.md`

## Virus Combination

Virus combination is driven by `InfectionEngine.infect(...)`.

1. The main screen gathers the selected local viruses and any viruses decoded from the friend invite box.
2. Each side is collapsed into a template strain by averaging infectivity, resilience, and chaos scores.
3. If the selected strains do not all belong to the same family, the last family in that selection becomes the collapsed family label.
4. The two collapsed strains are merged into a single offspring virus.
5. Matching families increase similarity, while closer stat values reduce mutation pressure.
6. Mutation is deterministic for a pair of genomes because the pseudo-random choice is seeded from both genome strings.
7. A mutation boosts chaos and infectivity, changes the suffix from `Remix` to `Chimera`, and marks the genome with `-M` instead of `-S`.

Practical effect:

- Similar strains usually produce a stable offspring.
- Dissimilar strains are more likely to mutate.
- Leaving the invite field blank causes the app to generate a seeded random friend strain so the flow always produces an offspring.

## App Layout

The app has two layouts and each one maps directly to one activity.

`start.xml` powers the lab screen:

- Header and collection summary at the top.
- Multi-select virus list for choosing one or more owned strains.
- Invite code input area for pasted share codes.
- Action row for infecting and sharing.
- Secondary action for opening the details screen.
- Result area at the bottom for the latest outbreak summary.

`my_virus.xml` powers the details screen:

- A large virus name header.
- Carrier, infected population, and infection rate stats.
- Family, chaos, genome, and origin metadata.
- A single button that returns to the lab.

Data flow through the layout is intentionally simple: the lab screen owns user actions, reads and writes repository state, then launches the details screen with a virus ID extra.

## Other Relevant Sections

The remaining compartments are intentionally small and focused.

`model`

- `Virus` is the main serializable domain object.
- `Infectivity`, `Resilience`, and `Chaos` wrap raw scores so the engine works with named value types.
- `InfectionRates` converts a combined score into the `LOW` to `OUTBREAK` display band.

`data`

- `VirusRepository` keeps the current collection in memory only.
- The repository seeds starter viruses lazily and returns defensive copies for list reads.
- There is no persistence layer yet, so app restarts reset the collection.

`tests`

- Engine tests cover deterministic mutation and combination behavior.
- Model tests cover share-code sanitization and infection-rate mapping.
- Repository tests cover seeding, ordering, and lookup behavior.

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

Machine-specific Java setup (do not commit):

- Keep repository `gradle.properties` environment-neutral.
- Set `JAVA_HOME` to a local JDK 17+ install, or add `org.gradle.java.home=...` in `%USERPROFILE%\\.gradle\\gradle.properties`.
- Do not put machine-specific Java paths in the project `gradle.properties` file.

Useful commands:

- ./gradlew --version
- ./gradlew tasks
- ./gradlew assembleDebug
- ./gradlew testDebugUnitTest
