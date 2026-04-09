# Engine compartment

This package contains the game rules for creating and combining viruses.

- `VirusFactory` creates starter viruses, decodes invite codes, creates random friend strains, and builds genome strings.
- `InfectionEngine` collapses selected virus groups into templates and merges them into offspring viruses.

Naming and genome notes:

- A strain's display `name` is separate from its lineage `family`; the engine combines families and only displays names as flavor text.
- Genome strings are readable fingerprints used for deterministic mutation seeding and UI flavor. They are not treated as a stable parse format for reconstructing stats.
- Random friend stand-ins use dedicated fallback provenance instead of the lab-seeding origin string.

Combination summary:

1. Collapse owned viruses into one template strain.
2. Collapse friend viruses into one template strain.
3. Merge the two templates into one offspring.
4. Decide mutation using a deterministic seed derived from the parent genomes.
5. Build the final genome and origin metadata.

Boundary rule:

- This package can depend on the `model` package.
- It should not depend on Android framework classes.