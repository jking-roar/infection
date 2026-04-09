# Data compartment

This package contains app state storage.

- `VirusRepository` is an in-memory collection used by the activities.
- The repository seeds starter viruses on demand through `VirusFactory`.
- Reads return copied lists so callers do not mutate repository storage directly.

Current limitation:

- State is process-local only.
- Restarting the app rebuilds the starter set and loses newly created viruses.