package com.example.funfection;

import java.io.Serializable;

public class Virus implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String family;
    private final String carrier;
    private final int infectivity;
    private final int resilience;
    private final int chaos;
    private final boolean mutation;
    private final String genome;
    private final String origin;

    public Virus(String id,
                 String name,
                 String family,
                 String carrier,
                 int infectivity,
                 int resilience,
                 int chaos,
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

    public int getInfectivity() {
        return infectivity;
    }

    public int getResilience() {
        return resilience;
    }

    public int getChaos() {
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
        return InfectionRates.fromScore(infectivity + resilience + chaos);
    }

    public String toShareCode() {
        return id + ":" + family + ":" + infectivity + ":" + resilience + ":" + chaos + ":"
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