# Model compartment

This package defines the app's core data shapes.

- `Virus` is the central domain object shared by the UI, engine, and repository.
- `ViralStat` is the shared score wrapper base type.
- `Infectivity`, `Resilience`, and `Chaos` provide named stat wrappers.
- `InfectionRates` translates combined stat totals into display-friendly severity bands.

Field model:

- `name` is the player-facing display label for one strain instance.
- `family` is the lineage label used by engine rules and UI grouping.
- `genome` is a readable fingerprint for deterministic behavior and flavor text, not a parseable source of truth.

Boundary rule:

- Model types should stay framework-light and reusable.
- Business rules that derive new viruses belong in `engine`, not here.