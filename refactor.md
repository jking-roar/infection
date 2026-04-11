# UI Refactor Plan: Collection -> Lab

## Goal
Move to a Lab-first experience where users can manage strains from one place, while reducing duplicate UI flows.

## Confirmed Decisions
- Rename `Collection` to `Lab` in all user-facing copy.
- `Clean` action name is `Purge Strain`.
- `Purge Strain` is destructive and should include a confirmation warning (irreversible).
- Eliminate tabs with duplicate functionality (Create/Combine should no longer be primary standalone flows).
- Share text and share QR continue using current share payload format.

## Open Questions
- What exact interaction should ship first for combine selection?
  - Proposed MVP: standard dialog with fixed left strain and selectable right-side strains.
  - Proposed enhanced UX: slide-out panel with left-side multiselect view, fixed first strain (disabled), and visual linking of selected right-side strains.
- Should enhanced combine UX ship in the same release or a follow-up release after MVP?

## Scope
- Rename Collection to Lab in UI copy and navigation labels.
- Tapping a virus in Lab opens a per-virus command menu:
  - View details
  - Share via text
  - Share via QR code
  - Purge Strain
  - Combine (tapped virus is always left side)
- Add a bottom `Create Virus` action in Lab:
  - Prompt for inspiration/seed
  - Create and save virus
  - Open details for the new virus
- Update details view to include Lab-equivalent commands (except View), plus `Back to Lab`.

## Sequential Tasks

### Completed Task 1 - Navigation and terminology alignment
- Definition of Done: all user-facing `Collection` labels are updated to `Lab`, and duplicate Create/Combine nav entries are removed or explicitly redirected.
- Update bottom nav and labels from `Collection` to `Lab`.
- Remove or redirect duplicate navigation entries for standalone Create/Combine flows.
- Update screen titles and supporting copy to match Lab-first wording.

### Completed Task 2a - Lab action menu
- Definition of Done: tapping any Lab virus opens a command menu with working actions for details, share text, share QR, purge, and combine.
- Replace current tap behavior with a per-virus action menu/dialog.
- Implement handlers for:
  - View details
  - Share text
  - Share QR
  - Purge Strain (with irreversible confirmation)
  - Combine (left-side fixed to tapped strain)

### Completed Task 2b - refinement of refactor
- Definition of Done: Lab virus rows have no selection checkbox, and tapping any row opens a command menu with working actions for details, share text, share QR, purge, and combine.
- Remove selection checkboxes from Lab virus list items.
- Use tap-to-open action menu as the only row interaction pattern.

### Completed Task 3 - Combine-from-Lab (MVP)
- Definition of Done: combine flow fixes left-side strain, places the pinned left strain at the top of the selection list, supports right-side selection, and provides back/cancel to return to Lab with no side effects when not committed.
- Implement right-side selection dialog for combine.
- Keep tapped virus fixed as left-side input.
- Bring the pinned left-side strain to the top of the combine selection list.
- Add a back/cancel path that returns to Lab without creating offspring or mutating repository state.
- Commit combine via existing local combine logic.
- Persist offspring and refresh Lab list.

### Completed Task 4 - Create-from-Lab
- Definition of Done: Lab `Create Virus` prompt creates and persists a strain, then opens that strain's details screen.
- Add bottom `Create Virus` CTA in Lab.
- Prompt for inspiration/seed.
- Create virus, persist it, and open details immediately.

### Completed Task 5a - Details screen parity (MVP)
- Definition of Done: details screen provides share text, share QR, purge, combine, and back-to-lab actions with behavior matching Lab.
- Add detail-screen actions:
  - Share text
  - Share QR
  - Purge Strain
  - Combine
  - Back to Lab
- Keep behavior and side effects consistent with Lab actions (same share payloads, same purge rules, same combine entry behavior).

### Completed Task 5b - Details/Lab action UI unification (follow-up)
- Definition of Done: details actions are presented in a dialog-like treatment, and the action UI is shared between details and Lab.
- Update details page presentation to feel dialog-like so action buttons are not confused with main navigation.
- Refactor a reusable action view/component that can be used by both details and Lab.
- Rework Lab actions to extend the list item interaction model instead of using a dropdown action menu.
- Preserve Task 5a behavior and side effects as-is while changing presentation.

#### Task 5 sequencing note
- Plan default: ship Task 5a first, then Task 6 regression/testing, then Task 5b as a follow-up.
- If Task 5b is pulled into the same release, time-box it and keep the Task 5a interaction path as fallback.

### Completed Task 6 - Repository and regression tests
- Definition of Done: remove/purge behavior is repository-backed, covered by unit tests, and Lab-related smoke checks pass without regressions.
- Add repository support for purge/remove if missing.
- Add/adjust unit tests for remove behavior and ordering (newest first).
- Run regression smoke checks across remaining tabs and updated Lab flows.

### Task 7 - Enhanced combine UX (optional follow-up)
- Definition of Done: enhanced slide-out combine UI is implemented behind an explicit ship/no-ship decision with MVP fallback preserved.
- Implement slide-out combine selector with richer visual selection model.
- Preserve MVP combine behavior as fallback.
- Ship only if complexity/risk is acceptable for target release.

### Task 8 - force portrait orientation
- Definition of Done: app is locked to portrait orientation on all screens, and landscape orientation is disabled.
- Update app configuration to lock orientation to portrait.
- Test on various devices to ensure landscape orientation is disabled and portrait orientation is enforced.
- Verify QR scanning is in portrait mode and functions correctly.

## Acceptance Criteria
- UI consistently uses `Lab` instead of `Collection`.
- Lab virus items do not show selection checkboxes.
- Virus tap in Lab shows all required actions.
- `Purge Strain` requires confirmation and removes the virus when confirmed.
- Combine from Lab fixes left side, places pinned strain at the top of selection UI, and allows right-side selection before commit.
- Combine flow includes back/cancel that returns to Lab with no changes when user exits before commit.
- `Create Virus` in Lab prompts for seed and opens new virus details.
- Details view includes action parity plus `Back to Lab`.
- Share payload format remains compatible with current invite/share parsing.
- Duplicate standalone Create/Combine paths are removed or intentionally redirected.

## Out of Scope
- Engine rule changes for mutation/infection math.
- Share-code format changes.
- Non-essential visual redesign outside the Lab/details refactor scope.
