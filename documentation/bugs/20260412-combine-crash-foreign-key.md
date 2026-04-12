App crash when combining two viruses
---
## Title: "App Crashes on Local Combine — Foreign Key Constraint Violation"

## Description
The app crashes immediately after combining two locally-owned viruses via the combine drawer. The crash occurs on the background I/O thread (`virus-repository-io`) and is a fatal, unrecoverable exception.

## Steps to Reproduce
1. Open the app and navigate to the Collection tab.
2. Open the combine drawer (swipe or tap the combine button on any virus).
3. Select a second virus to combine with.
4. Tap the button to execute the combine.
5. App crashes.

## Expected Behavior
The combine operation should complete successfully, the new offspring virus should be saved, and the virus detail screen (`MyVirusActivity`) should open showing the result.

## Actual Behavior
The app crashes with a fatal exception on the `virus-repository-io` thread. The new virus is not saved and the detail screen never opens.

## Additional Information
- Device: Pixel 5
- Android Version: 14
- App Version: 1.0

### Logcat Output:
```
04-12 15:29:12.620  5614  5636 E AndroidRuntime: FATAL EXCEPTION: virus-repository-io
04-12 15:29:12.620  5614  5636 E AndroidRuntime: Process: com.kingjoshdavid.funfection, PID: 5614
04-12 15:29:12.620  5614  5636 E AndroidRuntime: java.lang.IllegalStateException: VirusRepository operation failed
04-12 15:29:12.620  5614  5636 E AndroidRuntime:        at com.kingjoshdavid.funfection.data.VirusRepository.runOnIo(VirusRepository.java:397)
04-12 15:29:12.620  5614  5636 E AndroidRuntime:        at com.kingjoshdavid.funfection.data.VirusRepository.addVirus(VirusRepository.java:159)
04-12 15:29:12.620  5614  5636 E AndroidRuntime: Caused by: android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)
04-12 15:29:12.620  5614  5636 E AndroidRuntime:        at android.database.sqlite.SQLiteConnection.nativeExecuteForLastInsertedRowId(Native Method)
```

## Thoughts where problem might be:
The crash originates in `VirusRepository.syncRoomLinksForVirus` (line 493), which tries to insert rows into the `friend_virus_cross_ref` table via `FriendVirusDao.upsert`. That table enforces a `FOREIGN KEY` constraint: `friendId` must exist in the `friends` table.

The helper `deriveAssociatedFriendIds` (line 497) extracts source IDs from the virus's `VirusOrigin` — both the `directSource` and any `patientZeros` — but does **not** filter by `source.isRealFriend()` or verify that the ID is already present in the `friends` table. When one of the parent viruses in the combine carries a `VirusOrigin` whose `directSource` or `patientZero` IDs are not tracked in the `FriendEntity` table (e.g. a `Source.real(...)` created from `VirusOrigin.seededByUser(userId, ...)` using the player's own profile ID, or a stably-derived carrier ID from `importedFromInvite`), the upsert fails the FK check and the whole I/O operation throws.

The fix likely needs to go in `deriveAssociatedFriendIds` and/or `syncRoomLinksForVirus`: only attempt to link friend IDs that are confirmed to exist in the `FriendEntity` table (or that have `isRealFriend() == true` and are pre-ensured in `FriendsRepository`). Alternatively, the `FriendVirusCrossRef` FK could be made deferrable/nullable, though constraining it at the repository layer is cleaner.

# Fix Direction:
After the app starts up, add the user to the friends table if they are not already present.

That way they are there before attempting to link them in `syncRoomLinksForVirus`. This would ensure that any real friend sources are properly linked without causing FK violations, while still allowing non-friend sources to be ignored.


Filter the friends list to not show yourself by default.  Add a configuration item to show yourself in the friends list if desired. call the option "You are your friend" and display as a checkbox.

This configuration should be saved immediately when changed.

Extra fix: add static UUIDs to all the MadScientists, so they can be added to the friends table as well. This would allow them to be linked as sources without causing FK violations, and would also enable features like showing them in the friends list or allowing the player to "friend" them for fun.
