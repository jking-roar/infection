# UI Refactor Plan: Collection -> Lab

## Goal
Turn the current Collection-first flow into a Lab-first flow where users can manage a virus directly from one place.

## Scope
- Rename Collection to Lab in user-facing UI copy.
- Tapping a virus in Lab opens commands for that virus:
  - View details
  - Share (with sub-optiosn):
    - via text
    - via QR code
  - Clean action (rename to an in-universe term; suggested: `Purge Strain`)
  - Combine (selected virus is always left side; user chooses right-side virus(es) in a dialog)
- Add a bottom `Create Virus` action in Lab:
  - Prompt for inspiration/seed
  - Create the virus
  - Open details for the newly created virus
- Update details view to include the same commands as Lab (except View), plus `Back to Lab`.

## Assumptions to Confirm
- `Clean` means removing the virus from local repository state.
- Existing `Create` tab remains for now (Lab create is primary; no tab removal in this refactor).
- Share text and share QR reuse the existing share payload format without modification.

## Sequential Tasks

### Task 1 - Planning and naming decisions
- Finalize terminology: `Lab`, and clean action label (`Purge Strain` or `Contain Strain`).
  - Answer: 'Purge Strain'
    - Warn that this is forever.
- Confirm whether the existing Create tab stays unchanged during this pass.
  - Answer: No, eliminate tabs with duplicate functionality.  
- Lock UX behavior for combine and creation dialogs.
  - Question: what does this mean? clarify with author.

### Task 2 - Lab naming and screen copy
- Update bottom-nav/tab label from Collection to Lab.
- Update Collection screen title/labels to Lab terminology.

### Task 3 - Lab virus action menu
- Replace current tap behavior with a per-virus action menu/dialog.
- Implement handlers:
  - View details
  - Share (with sub-options):
    - text
    - QR
  - Combine (as left-side fixed input)
  - Clean/remove

### Task 4 - Combine-from-Lab flow
- On `Combine`, open right-side selection dialog (single or multi-select per existing combine rules).
  - I like a right-side selection dialog, but as a slide out that will list the selcted virus and the left side will transform into a multiselect with the first selected virus (left side) greyed out and unselectable.
    - Selected viruses on the left side will be added as indented items under the fixed virus, with a line connecting them to show they are combined.
- Call local combine logic with tapped virus fixed as left side.
- Persist offspring and refresh Lab list.

### Task 5 - Create-from-Lab flow
- Add bottom `Create Virus` CTA to Lab.
- Prompt for inspiration/seed.
- Create and save virus.
- Navigate directly to its details screen.

### Task 6 - Details screen parity
- Add actions in details view:
  - Share text
  - Share QR
  - Clean/remove
  - Combine
  - Back to Lab
- Ensure details actions follow the same behavior/rules as Lab actions.

### Task 7 - Repository and tests
- Add repository support for clean/remove if missing.
- Add/adjust unit tests for:
  - Remove behavior
  - Add ordering (newest first)
  - Combine and create side effects where testable
- Run smoke checks across tabs (Lab/Create/Combine/Infect/Friends) for regressions.

## Acceptance Criteria
- UI consistently says `Lab` instead of `Collection`.
- Tapping a Lab virus shows all expected actions.
- Combine from Lab fixes tapped virus as left side and lets user choose right side before commit.
- Bottom `Create Virus` in Lab prompts for seed and opens new virus details after creation.
- Details screen includes action parity with Lab and a working `Back to Lab` button.
- Share payload format remains compatible with current invite/share parsing.

## Out of Scope (This Refactor)
- Engine rule changes for mutation/infection math.
- Share-code format changes.
- Full redesign of Create/Combine/Infect tabs beyond regression-safe alignment.
