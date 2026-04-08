package com.example.funfection.model;

import java.io.Serializable;

/**
 * Immutable domain record describing a single virus strain in the app.
 *
 * <p>Stat wrappers on this model use a gameplay range of {@code 1..10} per axis:
 * infectivity (spread), resilience (survivability), and chaos (instability).
 * Derived totals therefore span {@code 3..30} when all three axes are present.</p>
 */
public class Virus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Stable identifier used for repository lookups and intent extras.
     */
    private final String id;

    /**
        * Display name shown to the player for this strain.
        *
        * <p>This is presentation-facing flavor text, not the canonical lineage key. Multiple
        * strains can share a family while having different display names.</p>
     */
    private final String name;

    /**
        * Family label grouping related strains, such as starter and offspring lineages.
        *
        * <p>The engine combines and compares families as lineage metadata separate from the
        * display name. UI copy should treat name and family as two distinct fields.</p>
     */
    private final String family;

    /**
     * Fictional host or patient-zero label associated with the strain.
     */
    private final String carrier;

    /**
     * Transmission strength score representing how easily the strain spreads.
     *
     * <p>Expected engine range: {@code 1..10}. Higher values increase contagiousness and
     * contribute linearly to the derived infection-rate total.</p>
     */
    private final Infectivity infectivity;

    /**
     * Durability score representing how well the strain survives resistance.
     *
     * <p>Expected engine range: {@code 1..10}. This is the defensive axis in infection math
     * and contributes linearly to the derived infection-rate total.</p>
     */
    private final Resilience resilience;

    /**
     * Volatility score representing how unstable or unpredictable the strain is.
     *
     * <p>Expected engine range: {@code 1..10}. Higher chaos raises mutation pressure in merge
     * logic and contributes linearly to the derived infection-rate total.</p>
     */
    private final Chaos chaos;

    /**
     * Flag indicating whether the strain is a mutated offspring rather than a stable merge.
     */
    private final boolean mutation;

    /**
        * Compact genome signature used to describe and deterministically combine strains.
        *
        * <p>This stays a display-oriented fingerprint used for deterministic seeding and flavor
        * text. It is not the source of truth for reconstructing the stat model.</p>
     */
    private final String genome;

    /**
     * Human-readable provenance text describing how or where this strain was created.
     */
    private final String origin;

    /**
     * Number of committed infection actions this strain lineage has participated in.
     *
     * <p>This count only changes after a share or combine action is committed. Preview flows do
     * not modify it. Offspring strains inherit a combined count from their parent templates.</p>
     */
    private final int infectionCount;

    /**
     * Creates a virus strain with its identity, stat profile, and provenance metadata.
     *
     * @param id stable identifier used for lookups and sharing inside the app
     * @param name display name shown in lists and detail views
     * @param family lineage label used when grouping or combining strains
     * @param carrier fictional host label for flavor text and detail screens
     * @param infectivity spread strength score for the strain (engine-normalized to {@code 1..10})
     * @param resilience resistance and survivability score for the strain (engine-normalized to {@code 1..10})
     * @param chaos instability score used in outbreak and mutation behavior (engine-normalized to {@code 1..10})
     * @param mutation true when this strain resulted from a mutation event
     * @param genome compact genome signature used in deterministic combination logic
     * @param origin description of how the strain entered the player's collection
     */
    public Virus(String id,
                 String name,
                 String family,
                 String carrier,
                 Infectivity infectivity,
                 Resilience resilience,
                 Chaos chaos,
                 boolean mutation,
                 String genome,
                 String origin) {
        this(id, name, family, carrier, infectivity, resilience, chaos, mutation, genome, origin, 0);
    }

    public Virus(String id,
                 String name,
                 String family,
                 String carrier,
                 Infectivity infectivity,
                 Resilience resilience,
                 Chaos chaos,
                 boolean mutation,
                 String genome,
                 String origin,
                 int infectionCount) {
        this.id = id;
        this.name = name;
        this.family = family;
        this.carrier = carrier;
        this.infectivity = infectivity;
        this.resilience = resilience;
        this.chaos = chaos;
        this.mutation = mutation;
        this.genome = genome;
        this.origin = origin;
        this.infectionCount = Math.max(0, infectionCount);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public String getCarrier() {
        return carrier;
    }

    public Infectivity getInfectivity() {
        return infectivity;
    }

    public Resilience getResilience() {
        return resilience;
    }

    public Chaos getChaos() {
        return chaos;
    }

    public boolean hasMutation() {
        return mutation;
    }

    public String getGenome() {
        return genome;
    }

    public String getOrigin() {
        return origin;
    }

    public int getInfectionCount() {
        return infectionCount;
    }

    /**
     * Calculates the UI-facing infection strength score for this strain.
     *
     * <p>This renames the old "friends infected" concept without changing its gameplay math.
     * Strength remains a derived display metric rather than a stored field.</p>
     *
     * <p>Formula:</p>
     * <p>{@code infectivity.score() * 10 + resilience.score() * 7}</p>
     *
     * @return derived infection-strength score shown on the details screen
     */
    public int getInfectionStrength() {
        return infectivity.score() * 10 + resilience.score() * 7;
    }

    /**
     * Calculates the UI-facing infection severity band for this strain.
     *
     * <p>The app does not store a separate severity field. Instead, it derives the displayed
     * infection rate from the combined infectivity, resilience, and chaos scores each time the
     * value is requested. That keeps the display tier consistent with the underlying stat model
     * used by the engine.</p>
     *
     * <p>Formula:</p>
     * <p>{@code total = infectivity.score() + resilience.score() + chaos.score()}</p>
     *
     * <p>With engine-normalized stats, {@code total} is typically {@code 3..30} and maps to:
     * {@code LOW} when {@code total <= 10}, {@code MEDIUM} when {@code total <= 18},
     * {@code HIGH} when {@code total <= 24}, otherwise {@code OUTBREAK}.</p>
     *
     * @return severity band derived from the combined stat total
     */
    public InfectionRates getInfectionRate() {
        return InfectionRates.fromScore(infectivity.score() + resilience.score() + chaos.score());
    }

    /**
     * Serializes this virus into the share-code format consumed by the engine.
     *
     * <p>Field order:</p>
     * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier:infectionCount}</p>
     *
     * <p>Serialization math and encoding:</p>
     * <p>{@code infectivity = infectivity.score()}</p>
     * <p>{@code resilience = resilience.score()}</p>
     * <p>{@code chaos = chaos.score()}</p>
     * <p>{@code mutation = hasMutation() ? 1 : 0}</p>
     *
     * <p>The mutation slot uses {@code 1} for mutated strains and {@code 0} for stable strains.
     * The name and carrier fields are sanitized to remove reserved separators before they are
     * appended, and the trailing infection-count field carries the committed lineage infection
     * total. Older invite codes without the trailing count still parse correctly.</p>
     *
     * @return one-line share code representing this virus
     */
    public String toShareCode() {
        return id + ":" + family + ":" + infectivity.score() + ":" + resilience.score() + ":" + chaos.score() + ":"
                + (mutation ? "1" : "0") + ":" + genome + ":" + sanitize(name) + ":" + sanitize(carrier)
                + ":" + infectionCount;
    }

    /**
     * Builds a compact list-row label for the lab screen.
     *
     * <p>The summary intentionally shows only the name, family, mutation state, and derived
     * infection-rate tier so the selection list stays readable while still surfacing the most
     * important gameplay signals.</p>
     *
     * @return short human-readable summary for list display
     */
    public String getSummaryLine() {
        String mutationLabel = mutation ? "Mutated" : "Stable";
        return name + "  |  " + family + "  |  " + mutationLabel + "  |  Rate " + getInfectionRate()
                + "  |  Infections " + infectionCount;
    }

    /**
     * Returns a copy of this strain with a replaced infection count.
     *
     * @param updatedInfectionCount new committed infection count for the strain
     * @return copied virus preserving all non-count fields
     */
    public Virus withInfectionCount(int updatedInfectionCount) {
        return new Virus(id, name, family, carrier, infectivity, resilience, chaos, mutation, genome, origin,
                updatedInfectionCount);
    }

    /**
     * Returns a copy of this strain with its committed infection count incremented by one.
     *
     * @return copied virus with infection count increased by one
     */
    public Virus incrementInfectionCount() {
        return withInfectionCount(infectionCount + 1);
    }

    /**
     * Removes reserved delimiter characters from user-facing text before serialization.
     *
     * <p>Share codes use {@code :} as a field separator and the UI summary uses {@code |} as a
     * visual divider. Replacing those characters preserves readability while ensuring that names
     * and carriers cannot corrupt the encoded invite format.</p>
     *
     * @param value display text to normalize for serialization
     * @return sanitized text safe to embed inside a share-code row
     */
    private String sanitize(String value) {
        return value.replace(':', '-').replace('|', '/');
    }
}