# Virus Naming And Combination Assessment Tasks

## Quick Wins

- [ ] Make all deterministic ID derivation charset-explicit with `StandardCharsets.UTF_8` in seed and combine paths.
- [ ] Add a dedicated fallback friend generator or origin override so random friend strains do not use `Seeded in lab` provenance.
- [ ] Decide whether genomes should remain display-only fingerprints or move to a parseable format; if parseable, zero-pad or delimit stat segments.
- [ ] Document the current distinction between display name and family so the code and UX are explicit about the existing model.

## Naming Model Decisions

- [ ] Decide whether seeded strains should continue using `PREFIX + SUFFIX` with family stored separately, or move to a structured `PREFIX + FAMILY + SUFFIX` style display model.
- [ ] If the naming model changes, add explicit `prefix` and `suffix` fields plus a derived display-name builder instead of storing only a flat name string.
- [ ] Define whether prefix and suffix tokens should encode mechanics such as severity, transmission, symptoms, mutation, or origin.
- [ ] Remove token-role overlap where suffixes duplicate family-like terms unless that overlap is intentionally part of the lore.

## Family And Lineage Rules

- [ ] Decide whether family is a stable curated taxonomy or a synthetic hybrid label.
- [ ] Replace substring-based mixed-family generation with a curated deterministic hybrid mapping or a dominant-family rule.
- [ ] Ensure hybrid family names remain pronounceable, lore-consistent, and testable.
- [ ] Review achievements, UI text, and repository expectations that assume family comes from a fixed set.

## Combination Semantics

- [ ] Decide whether combining strains should be commutative or intentionally left-dominant.
- [ ] If left-dominance is retained, surface it as an explicit mechanic in UI copy and developer documentation.
- [ ] Revisit collapse behavior for mixed selections so family is not chosen purely by last item in list order unless that rule is intentional.
- [ ] Revisit collapse carrier selection so mixed templates are not always attributed to the first selected carrier unless that is a deliberate design choice.
- [ ] Decide whether offspring names should preserve parent identity instead of collapsing to `family + Remix/Chimera`.

## Naming Refactor

- [ ] Introduce metadata-driven naming tokens with roles, tags, weights, and compatibility rules.
- [ ] Derive naming tags from infectivity, resilience, chaos, mutation state, and origin.
- [ ] Add validation rules or blocklists to prevent contradictory or low-quality token combinations.
- [ ] Preserve determinism in token selection so the same seed or parent pair always produces the same output.
- [ ] Define an inheritance rule for offspring naming, such as one token from each parent plus a mutation or severity epithet.

## Tests

- [ ] Add golden snapshot tests for starter seeds and a small fixed set of custom seeds covering name, family, stats, genome, and ID.
- [ ] Add tests that lock in UTF-8-based deterministic UUID generation.
- [ ] Add tests for friend fallback provenance and origin strings.
- [ ] Add tests for curated hybrid-family behavior once the replacement for substring splicing is chosen.
- [ ] Add tests for whichever combine rule is chosen: fully commutative or deliberately asymmetric.
- [ ] Add post-refactor naming invariant tests so mutation, severity, and family constraints remain coherent.
- [ ] Add validation tests to block contradictory token combinations and accidental token-role duplication.

## Documentation And Evaluation

- [ ] Update engine and UI documentation to explain the current or revised relationship between display name, family, genome, and mutation.
- [ ] Write down the intended lore rules for seeded strains, friend strains, local mixes, remixes, and chimeras.
- [ ] Add a small debug or test harness to measure naming diversity, collision rates, and semantic alignment after refactoring.
- [ ] Define success metrics for lore coherence, interpretability, and determinism before the naming-system redesign begins.
