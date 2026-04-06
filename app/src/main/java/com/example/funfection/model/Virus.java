package com.example.funfection.model;

import java.io.Serializable;

/**
 * Immutable domain record describing a single virus strain in the app.
 */
public class Virus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Stable identifier used for repository lookups and intent extras.
     */
    private final String id;

    /**
     * Display name shown to the player for this strain.
     */
    private final String name;

    /**
     * Family label grouping related strains, such as starter and offspring lineages.
     */
    private final String family;

    /**
     * Fictional host or patient-zero label associated with the strain.
     */
    private final String carrier;

    /**
     * Transmission strength score representing how easily the strain spreads.
     */
    private final Infectivity infectivity;

    /**
     * Durability score representing how well the strain survives resistance.
     */
    private final Resilience resilience;

    /**
     * Volatility score representing how unstable or unpredictable the strain is.
     */
    private final Chaos chaos;

    /**
     * Flag indicating whether the strain is a mutated offspring rather than a stable merge.
     */
    private final boolean mutation;

    /**
     * Compact genome signature used to describe and deterministically combine strains.
     */
    private final String genome;

    /**
     * Human-readable provenance text describing how or where this strain was created.
     */
    private final String origin;

    /**
     * Creates a virus strain with its identity, stat profile, and provenance metadata.
     *
     * @param id stable identifier used for lookups and sharing inside the app
     * @param name display name shown in lists and detail views
     * @param family lineage label used when grouping or combining strains
     * @param carrier fictional host label for flavor text and detail screens
     * @param infectivity spread strength score for the strain
     * @param resilience resistance and survivability score for the strain
     * @param chaos instability score used in outbreak and mutation behavior
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

    public InfectionRates getInfectionRate() {
        return InfectionRates.fromScore(infectivity.score() + resilience.score() + chaos.score());
    }

    public String toShareCode() {
        return id + ":" + family + ":" + infectivity.score() + ":" + resilience.score() + ":" + chaos.score() + ":"
                + (mutation ? "1" : "0") + ":" + genome + ":" + sanitize(name) + ":" + sanitize(carrier);
    }

    public String getSummaryLine() {
        String mutationLabel = mutation ? "Mutated" : "Stable";
        return name + "  |  " + family + "  |  " + mutationLabel + "  |  Rate " + getInfectionRate();
    }

    private String sanitize(String value) {
        return value.replace(':', '-').replace('|', '/');
    }
}