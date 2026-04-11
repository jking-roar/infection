package com.kingjoshdavid.funfection.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable domain record describing a single virus strain in the app.
 *
 * <p>Stat wrappers on this model use a gameplay range of {@code 1..10} per axis:
 * infectivity (spread), resilience (survivability), and chaos (instability).
 * Derived totals therefore span {@code 3..30} when all three axes are present.</p>
 */
public class Virus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Stable identifier used for repository lookups and intent extras.
     *
     * <p>This is not user-facing and does not need to be globally unique. It only needs to be unique within the player's collection and consistent across app sessions. The engine does not
     * consume or recognize this field, so it can be generated in any way that meets those requirements. For example, a simple UUID or a hash of the strain's stat profile and origin metadata could work. The key requirement is that the same strain should always have the same ID when reconstructed from the player's collection, so that sharing and combining logic can reliably identify and match strains.</p>
     */
    private final String id;

    /**
     * First token of the display name shown to the player for this strain.
     */
    private final String prefix;

    /**
     * Second token of the display name shown to the player for this strain.
     */
    private final String suffix;

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
     * Structured provenance metadata describing how or where this strain was created.
     */
    private final VirusOrigin origin;

    /**
     * Optional production context label (for example: Local Mix, Hybrid, or Cluster).
     */
    private final String productionContext;

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
            this(id, prefixFromDisplayName(name), suffixFromDisplayName(name), family, carrier,
                infectivity, resilience, chaos, mutation, genome, VirusOrigin.legacy(origin), 0);
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
            this(id, prefixFromDisplayName(name), suffixFromDisplayName(name), family, carrier,
                infectivity, resilience, chaos, mutation, genome, VirusOrigin.legacy(origin), infectionCount);
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
                 VirusOrigin origin) {
            this(id, prefixFromDisplayName(name), suffixFromDisplayName(name), family, carrier,
                infectivity, resilience, chaos, mutation, genome, origin, 0);
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
                 VirusOrigin origin,
                 int infectionCount) {
        this(id, prefixFromDisplayName(name), suffixFromDisplayName(name), family, carrier,
            infectivity, resilience, chaos, mutation, genome, origin, infectionCount);
    }

    public Virus(String id,
                 String prefix,
                 String suffix,
                 String family,
                 String carrier,
                 Infectivity infectivity,
                 Resilience resilience,
                 Chaos chaos,
                 boolean mutation,
                 String genome,
                 String origin) {
        this(id, prefix, suffix, family, carrier, infectivity, resilience, chaos, mutation, genome,
            VirusOrigin.legacy(origin), 0);
    }

    public Virus(String id,
                 String prefix,
                 String suffix,
                 String family,
                 String carrier,
                 Infectivity infectivity,
                 Resilience resilience,
                 Chaos chaos,
                 boolean mutation,
                 String genome,
                 String origin,
                 int infectionCount) {
        this(id, prefix, suffix, family, carrier, infectivity, resilience, chaos, mutation, genome,
            VirusOrigin.legacy(origin), infectionCount);
    }

    public Virus(String id,
                 String prefix,
                 String suffix,
                 String family,
                 String carrier,
                 Infectivity infectivity,
                 Resilience resilience,
                 Chaos chaos,
                 boolean mutation,
                 String genome,
                 VirusOrigin origin) {
        this(id, prefix, suffix, family, carrier, infectivity, resilience, chaos, mutation, genome, origin, 0);
    }

    public Virus(String id,
                 String prefix,
                 String suffix,
                 String family,
                 String carrier,
                 Infectivity infectivity,
                 Resilience resilience,
                 Chaos chaos,
                 boolean mutation,
                 String genome,
                 VirusOrigin origin,
                 int infectionCount) {
        this(id, prefix, suffix, family, carrier, infectivity, resilience, chaos, mutation, genome,
            origin, infectionCount, null);
    }

    public Virus(String id,
                 String prefix,
                 String suffix,
                 String family,
                 String carrier,
                 Infectivity infectivity,
                 Resilience resilience,
                 Chaos chaos,
                 boolean mutation,
                 String genome,
                 VirusOrigin origin,
                 int infectionCount,
                 String productionContext) {
        this.id = id;
        this.prefix = normalizeNamePart(prefix, "Unknown");
        this.suffix = normalizeNamePart(suffix, "Entity");
        this.family = family;
        this.carrier = carrier;
        this.infectivity = infectivity;
        this.resilience = resilience;
        this.chaos = chaos;
        this.mutation = mutation;
        this.genome = genome;
        this.origin = origin == null ? VirusOrigin.legacy("Unknown origin") : origin;
        this.infectionCount = Math.max(0, infectionCount);
        this.productionContext = normalizeOptionalMetadata(productionContext);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return (prefix + " " + suffix).trim();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
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
        return origin.getSummary();
    }

    public VirusOrigin getOriginInfo() {
        return origin;
    }

    public String getProductionContext() {
        return productionContext;
    }

    public String getOriginReport(String viewerId) {
        return origin.describeDetailedForViewer(viewerId);
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
    * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier:infectionCount[:originPayload]}</p>
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
            + (mutation ? "1" : "0") + ":" + genome + ":" + sanitize(getName()) + ":" + sanitize(carrier)
            + ":" + infectionCount + ":" + origin.toSharePayload();
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
        return getName() + "  |  " + family + "  |  " + mutationLabel + "  |  Rate " + getInfectionRate()
                + "  |  Infections " + infectionCount;
    }

    /**
     * Returns a copy of this strain with a replaced infection count.
     *
     * @param updatedInfectionCount new committed infection count for the strain
     * @return copied virus preserving all non-count fields
     */
    public Virus withInfectionCount(int updatedInfectionCount) {
        return new Virus(id, prefix, suffix, family, carrier, infectivity, resilience, chaos, mutation, genome, origin,
                updatedInfectionCount, productionContext);
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
        return (value == null ? "" : value).replace(':', '-').replace('|', '/');
    }

    private static String[] splitDisplayName(String name) {
        String normalized = (name == null ? "" : name.trim()).replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return new String[]{"Unknown", "Strain"};
        }
        int separator = normalized.indexOf(' ');
        if (separator < 0) {
            return new String[]{normalized, ""};
        }
        String prefix = normalized.substring(0, separator);
        String suffix = normalized.substring(separator + 1).trim();
        if (suffix.isEmpty()) {
            suffix = "Strain";
        }
        return new String[]{prefix, suffix};
    }

    private static String prefixFromDisplayName(String name) {
        return splitDisplayName(name)[0];
    }

    private static String suffixFromDisplayName(String name) {
        return splitDisplayName(name)[1];
    }

    private static String normalizeNamePart(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String normalizeOptionalMetadata(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}