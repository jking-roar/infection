# Vectors of Infection Feature Tasks (Prioritized)

This tasklist is ordered to deliver a usable **MVP first**, then layer enhancements.

## MVP Scope (Release Target)

[ ] P0-0) Save friend info on infection exchange and provenance discovery
**Goal:** Populate friend/vector records automatically from infection events and provenance data, not manual entry.
**Implementation Notes:**
- hard coded scientists are everyone's friends and cannot be deleted. They cannot have custom notes, but they will have descriptions.
- When a new infection is recorded, if the carrier or patient zero is not already in the friends table, create a new friend record with the most recent handle as the display name and an empty override. If the carrier or patient zero is already in the friends table but has a different most recent handle, update the existing record's most recent handle and append the prior handle to the history.
  - May need a handle table to track handle history and support efficient updates without creating duplicates.
- Keep identity stable by UUID mapping strategy from provenance data.

[ ] P0-1) Reframe Friends Tab as Vectors of Infection
**Goal:** Replace "Friends" placeholder behavior with a vectors-focused view: people discovered through infection exchange and lineage provenance.

**Implementation Notes:**
- Keep orchestration in `ui/` and persistence/rules in `data` + model layers.
- Remove manual friend-creation assumptions from UI copy and flows.
- Preserve bottom-nav entry from `MainActivity` to the vectors screen.

**Definition of Done:**
- `Friends` tab presents vectors language and purpose, not manual contact management.
- No manual "Add Friend" task or button in MVP UI.
- Screen opens without crash on empty and populated datasets.

---

[ ] P0-2) Define Room-Backed Vector Identity Model (No Invite Code Persistence)
**Goal:** Update friend/vector data model to align with privacy and provenance needs.

**Implementation Notes:**
- Friend/vector record includes: `id` (UUID), source/current display handle, personal override display name, private notes.
- Persist historical handles for same vector identity.
- Do **not** store invite codes in friend/vector tables.
- Keep Room as the persistence target for this plan.

**Definition of Done:**
- Model and repository contracts reflect UUID + names + notes + handle history.
- Invite code fields are removed/unused for vector persistence.
- Room schema supports reading/writing the new fields.

---

[ ] P0-3) Build Provenance-Driven Discovery and Upsert Pipeline
**Goal:** Create/update vector records automatically from infection events and provenance (carrier/patient zero references), not manual entry.

**Implementation Notes:**
- Wire discovery from existing infection/provenance touchpoints.
- If known vector appears with a new handle, update most recent handle and append prior handle to history.
- Keep identity stable by UUID mapping strategy from provenance data.

**Definition of Done:**
- New vectors appear only after infection/provenance discovery.
- Existing vector updates on handle change without creating duplicates.
- Historical handles are retained for identity continuity.

---

[ ] P0-4) Persist Vector-to-Virus Associations
**Goal:** Track which viruses each vector is known as carrier or patient zero for.

**Implementation Notes:**
- Add association storage/query support (join table or equivalent Room mapping).
- Support both roles where available: carrier and patient zero.
- Keep association writes tied to infection/provenance ingestion.

**Definition of Done:**
- For any vector, UI can load linked viruses and role context.
- Associations persist across app restarts.
- No invite-code persistence is introduced as part of association tracking.

---

[x] P0-5) Vectors List UI (Discovered Records Only)
**Goal:** Render vectors in `FriendsFragment` as a historical exchange network view.

**Implementation Notes:**
- Show current handle (or override when present), compact history signal, and linked-virus count/summary.
- Include loading and empty states; empty copy explains vectors appear through exchanges.
- Keep newest/recently-updated-first ordering unless repository contract defines otherwise.

**Definition of Done:**
- List shows discovered vectors from repository.
- Empty state communicates discovery-based creation.
- Cards do not expose invite-code actions.

---

[ ] P0-6) Vector Detail + Personal Annotation Editing
**Goal:** Provide detail view for a vector with editable private notes and personal display-name override.

**Implementation Notes:**
- Detail shows: personal override (if set), most recent source handle, historical handles, linked viruses.
- Editing scope is limited to private notes + override only.
- If override exists, list/detail display override as primary but still show source-most-recent handle.

**Definition of Done:**
- User can save/update private notes and override.
- Detail displays both override and most recent source handle when override exists.
- Historical handles remain visible after edits.

---

[ ] P0-7) Replace Deletion With Anonymization ("Mystery Entity")
**Goal:** Implement deletion semantics as anonymization instead of hard delete.

**Implementation Notes:**
- "Delete" action rewrites username/override/history fields to `Mystery Entity` state.
- Keep vector record and association integrity so lineage remains usable.
- On future rediscovery from infection/provenance, recover only current source username details.

**Definition of Done:**
- Deletion flow no longer removes vector rows; it anonymizes to `Mystery Entity`.
- Historical handles and override are replaced as specified.
- Future infection can repopulate current source handle without restoring prior private override/history.

---

[ ] P0-8) UX, Accessibility, and Failure-State Baseline
**Goal:** Ensure vectors MVP is usable and resilient.

**Implementation Notes:**
- Touch targets >=48dp and content descriptions for actions.
- User-safe error messaging for repository/Room failures.
- Validate edit fields (override/notes constraints) without blocking app responsiveness.

**Definition of Done:**
- Accessibility labels present for interactive controls.
- Loading, empty, and error states are visible and testable.
- Repository failure paths do not freeze UI.

## Post-MVP Enhancements (Next Iteration)

[ ] P1-1) Handle History Timeline UX
**Goal:** Improve historical handle readability with timeline-style presentation.

**Definition of Done:**
- Detail screen shows ordered historical handles with clear current marker.
- Works with anonymized records and rehydrated records.

[ ] P1-2) Rich Virus Association Insights
**Goal:** Expand vector cards/details with role counts and recent-associated strains.

**Definition of Done:**
- UI displays carrier/patient-zero breakdown for each vector.
- Queries remain performant for typical local dataset sizes.

[ ] P2-1) Test Expansion (Unit + Robolectric)
**Goal:** Increase confidence around provenance upsert, anonymization, and annotation edits.

**Definition of Done:**
- Unit tests cover handle-change history retention and anonymization rules.
- Robolectric tests cover vectors list/detail rendering and edit flows.

## Suggested Execution Order
1. P0-1, P0-2 (scope + data contract)
2. P0-3, P0-4 (discovery and associations)
3. P0-5, P0-6 (list/detail UX)
4. P0-7 (anonymization semantics)
5. P0-8 (quality baseline)

