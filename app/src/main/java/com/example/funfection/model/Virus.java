package com.example.funfection.model;

import java.io.Serializable;

public class Virus implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String family;
    private final String carrier;
    private final Infectivity infectivity;
    private final Resilience resilience;
    private final Chaos chaos;
    private final boolean mutation;
    private final String genome;
    private final String origin;

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