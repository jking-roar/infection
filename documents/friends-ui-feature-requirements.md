# Friends UI Feature Requirements (Product Draft)

## 1) Context and Problem Statement

`Friends` is currently a placeholder tab that renders a generic “Coming Soon” message and has no friend-management workflow. The app already has a persistence-backed `FriendsRepository` and `Friend` model, but no production UI path that creates, edits, lists, or uses friend records directly from the Friends tab.

**Problem:** users can import strains and share invite codes, but cannot curate a trusted friend network, quickly reuse saved invite codes, or understand their “infected network” over time.

## 2) Product Vision

Deliver a dedicated Friends experience that lets players:
- build and maintain a personal network of friends,
- quickly reuse friend invite codes in import flows,
- view simple network health/recency signals,
- perform core lifecycle actions (add, edit, remove, share, import) from one place.

## 3) Goals / Non-Goals

### Goals
1. Replace placeholder content with a fully functional Friends screen.
2. Allow CRUD on friend records (`displayName`, `inviteCode`) backed by `FriendsRepository`.
3. Enable one-tap action handoff into existing Import flow.
4. Preserve app architecture boundaries (UI orchestration in `ui`, logic in `data`/`engine`).
5. Ensure first usable release works offline with local Room persistence.

### Non-Goals (for v1)
- Cloud sync or account-based social graph.
- Real-time multiplayer presence/chat.
- Cross-device friend reconciliation.
- Deep epidemiology simulation in Friends (advanced analytics can be future work).

## 4) Primary Users and Jobs-to-be-Done

1. **Collector player**: “I want to save friend invite codes so I can import quickly without asking repeatedly.”
2. **Experimenter player**: “I want to identify my most useful friend strains and re-import from them fast.”
3. **Casual player**: “I need simple, clear friend actions without learning technical code formats.”

## 5) User Stories (v1)

1. As a player, I can view my saved friends in newest-first order.
2. As a player, I can add a friend with name + invite code manually.
3. As a player, I can scan/paste an invite code while adding a friend.
4. As a player, I can edit an existing friend’s name/invite code.
5. As a player, I can delete a friend with confirmation.
6. As a player, I can tap “Import from friend” to launch/import using that friend’s saved invite code.
7. As a player, I can quickly share my own current invite from the Friends tab.
8. As a player, I see empty-state guidance when no friends are saved.

## 6) Information Architecture and Navigation

### Entry
- Bottom-nav `Friends` tab remains the entry point.

### Screen Structure (single-fragment v1)
1. **Header block**
   - Title: “Friends Network”
   - Subtitle/summary: total saved friends and optional recent activity text.

2. **Quick Actions row**
   - Add Friend
   - Import via QR Scan (pre-fill friend form or route to Import)
   - Share My Invite

3. **Friends list**
   - Recycler list of friend cards.
   - Newest first (consistent with repository ordering).

4. **Empty state**
   - Friendly explainer + CTA button “Add First Friend”.

### Friend Card (minimum fields/actions)
- Display name
- Truncated invite code preview
- Created/updated relative time (optional for v1 if timestamp handling is straightforward)
- Actions:
  - Import
  - Edit
  - Delete
  - Copy code

## 7) Functional Requirements

### FR-1: Friend List Rendering
- App shall display all saved friends from `FriendsRepository.getFriendsAsync(...)`.
- Ordering shall be newest-first.
- Loading state shall be visible for async fetch (skeleton/spinner acceptable).
- Empty state shown when list is empty.

### FR-2: Add Friend
- App shall provide an Add Friend form with:
  - Required: display name
  - Required: invite code
- App shall generate/store a stable friend ID at creation time.
- On save success, list refreshes and new friend appears at top.

### FR-3: Edit Friend
- App shall open existing friend values in an edit dialog/sheet.
- Save updates shall preserve identity (same ID) and overwrite values.
- Updated friend remains singular (no duplicates by ID).

### FR-4: Delete Friend
- App shall provide a destructive confirmation step.
- On delete success, card is removed immediately and undo snackbar is optional.

### FR-5: Import From Friend
- App shall expose “Import” action per friend card.
- Selecting Import shall route into existing import execution path using friend invite code.
- User shall receive same preview/confirm UX semantics used in Import flow.

### FR-6: Add via Scan/Paste UX
- Add Friend form shall support QR scan result injection into invite code field.
- Paste detection/help text should explain valid invite code expectations.

### FR-7: Validation & Error Handling
- Required-field validation with inline messaging.
- Invalid invite payload should not block save if product chooses “save as raw code”; however Import must still support fallback behavior already in place.
- Repository/database failures surface user-safe error toast/dialog and keep UI responsive.

### FR-8: Accessibility and UX Baseline
- Touch targets >= 48dp.
- TalkBack-friendly content descriptions for card actions.
- Color contrast aligns with Material defaults.

## 8) Data & Domain Requirements

1. Continue using `Friend(id, displayName, inviteCode)` as v1 domain model.
2. Persist via existing Room-backed `FriendsRepository` APIs.
3. Keep fallback behavior compatible with in-memory mode for JVM tests.
4. New metadata fields (e.g., lastImportedAt, importCount) are deferred unless schema migration is approved for v1.1+.

## 9) Integration Requirements

1. **Import flow integration**
   - Preferred: Friends card action launches Import tab/flow with invite prefilled.
   - Alternate: execute preview/import directly from Friends fragment using shared helper.
2. **Share integration**
   - Friends quick action should reuse existing share-code generation logic from collection data.
3. **Repository init dependency**
   - Friends screen assumes repository initialization already handled in `MainActivity`.

## 10) Analytics / Telemetry (if instrumentation added)

Track at minimum:
- `friends_view_opened`
- `friend_add_submitted`
- `friend_add_failed`
- `friend_import_tapped`
- `friend_deleted`

(Events are optional for v1 if telemetry pipeline does not yet exist.)

## 11) Acceptance Criteria (v1 Release Gate)

1. Friends tab no longer shows placeholder text.
2. User can add, edit, and delete friends entirely on-device.
3. Friend list persists across app restarts (Room path).
4. Tapping Import on a friend uses that friend invite code and reaches import preview/result.
5. Empty state and error states are visually clear and non-blocking.
6. Basic accessibility labels exist for all interactive controls.
7. Existing import/local combine features remain unaffected.

## 12) Delivery Plan (Suggested)

### Phase 1: MVP Friends CRUD
- Replace placeholder fragment/layout.
- Implement list + empty state + add/edit/delete.
- Wire repository async callbacks and UI refresh.

### Phase 2: Action Integrations
- Add import handoff from friend cards.
- Add copy/share conveniences.
- Harden validation and UX polish.

### Phase 3: Quality & Scale
- Add targeted unit tests for presenter/controller logic.
- Add Robolectric tests for list rendering and core interactions.
- Evaluate schema extension for friend activity metadata.

## 13) Open Questions for Product/Design

1. Should invalid invite codes be saveable (for later correction) or hard-rejected?
2. Should “Import from friend” always open preview, or allow one-tap import for power users?
3. Should friend cards show last-used/import count in v1 or v1.1?
4. Do we need search/filter at launch, or defer until friend list grows?
5. Should deleting a friend also remove related history (if introduced later)?

---

## Appendix: Current-State Notes Used for This Draft

- Friends tab currently maps to a placeholder fragment and static message.
- `FriendsRepository` and Room DAO already support get/save/delete and newest-first list behavior.
- Main activity initializes Friends repository at startup.
- Existing Import flow already handles invite parsing, preview, and fallback behavior.
