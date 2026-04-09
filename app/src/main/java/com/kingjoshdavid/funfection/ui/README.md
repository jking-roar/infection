# UI compartment

This package contains the Android-facing code.

- `StartActivity` owns the lab screen, wires button handlers, gathers selected viruses, and coordinates infection, sharing, and navigation.
- `MyVirusActivity` renders a single virus from a repository lookup using the virus ID passed in the intent.

Layout mapping:

- `app/src/main/res/layout/start.xml` belongs to `StartActivity`.
- `app/src/main/res/layout/my_virus.xml` belongs to `MyVirusActivity`.

Boundary rule:

- UI classes should coordinate user actions and presentation only.
- Virus creation, invite parsing, and combination rules belong in the `engine` package.
- Collection state belongs in the `data` package.