# Database Implementation Review (commit `12afc91`)

## Scope
- Reviewed the previous commit on this branch: `12afc91` ("Add Room-backed virus and friend repositories").
- Focused on correctness/regression risk in repository behavior, persistence safety, and test coverage.
- Ran unit tests: `:app:testDebugUnitTest` (passes), but these currently exercise only the in-memory fallback path.

## Findings (ordered by severity)

### ~~High - Repository APIs still block the main thread~~
- **Where:** `app/src/main/java/com/kingjoshdavid/funfection/data/VirusRepository.java:247-256`, `app/src/main/java/com/kingjoshdavid/funfection/data/FriendsRepository.java:130-139`
- **Evidence:** `runOnIo(...)` submits work to a background executor, then immediately calls `future.get()`. Callers therefore block until DB work completes.
- **User-facing impact:** Most call sites are UI paths (for example `app/src/main/java/com/kingjoshdavid/funfection/ui/InfectFragment.java:77`, `app/src/main/java/com/kingjoshdavid/funfection/ui/CollectionFragment.java:223`, `app/src/main/java/com/kingjoshdavid/funfection/ui/MyVirusActivity.java:146`). This can introduce visible jank and ANR risk as data volume grows.
- **Fix before merge:** Convert repository APIs used by UI to async callbacks/Futures (or lifecycle-aware observers) so UI does not wait on blocking DB reads/writes.
- developer notes:
  - runOnIo pattern should be replaced with asynchronous with(dataAccessor, doCallback) or similar, where callback is invoked with result on main thread. This will require refactoring call sites to handle async results, but is critical for a responsive UI.
  - While data is loading, UI should have a loading placeholder.
  - refer to [live data guidelines.md](live%20data%20guidelines.md) for developer guidelines on using LiveData and async patterns in the future.


### ~~Medium - "Cannot delete last virus" rule is not atomic in DB path~~
- **Where:** `app/src/main/java/com/kingjoshdavid/funfection/data/VirusRepository.java:175-186` and `app/src/main/java/com/kingjoshdavid/funfection/data/VirusRepository.java:205-213`
- **Evidence:** `purgeVirusById(...)` first checks status (`count` + `exists`) and then deletes in a separate operation. Between those calls, concurrent actions can change row count.
- **Impact:** Under concurrent purge operations, the invariant that the last virus cannot be deleted can be violated.
- **Fix before merge:** Make purge guard + delete a single transaction (DAO-level transactional method that conditionally deletes only when count > 1 and row exists).
- developer notes:
  - no need to fix, as the user can just create a new virus.  May want to consider removing the "cannot delete last virus" rule in the future, as it adds complexity and edge cases without a strong UX justification.

### Medium - Destructive migration can silently wipe local data on schema changes
- **Where:** `app/src/main/java/com/kingjoshdavid/funfection/data/local/DatabaseProvider.java:24`
- **Evidence:** Database builder uses `fallbackToDestructiveMigration()`.
- **Impact:** Any future version bump without explicit migration will clear saved viruses/friends, which is risky for player progression.
- **Fix before merge:** Either add a migration policy now (preferred), or explicitly gate destructive migration to debug-only builds and document expected data-loss behavior.
- developer notes:
  - Document migration patterns for rooms integration for future enhancement of the database.

### Medium - Missing tests for Room-backed behavior and new friend persistence path
- **Where:** `app/src/test/java/com/kingjoshdavid/funfection/data/VirusRepositoryTest.java` and absence of `FriendsRepository` tests.
- **Evidence:** Current tests manipulate static `COLLECTION` directly and do not initialize Room. No tests validate DAO mapping, ordering by `created_at`/`createdAt`, purge invariants in DB mode, or `FriendsRepository` CRUD.
- **Impact:** Regressions in the new persistence layer can ship undetected even when the unit suite is green.
- **Fix before merge:** Add repository tests that run against a Room database (or extraction layer with fakes) for both viruses and friends.
- devloper notes: it looks like rooms has an in memory database option, so that should be used when running from a test context.

## Items worth fixing before merging to `main`
1. Remove main-thread blocking from repository API usage in UI flows.
2. Make purge-status check + delete atomic for DB mode.
3. Decide and implement a non-destructive migration strategy (or explicitly limit destructive behavior).
4. Add tests that cover Room-backed repository behavior (including `FriendsRepository`).

## Notes
- Current commit structure is directionally good (clear DAO/entity separation and initialization in activities), but the points above are the highest-value reliability gaps to address pre-merge.

