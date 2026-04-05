package com.example.funfection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public final class InfectionEngine {

    private InfectionEngine() {
    }

    public static Virus infect(List<Virus> ownedViruses, List<Virus> friendViruses) {
        Virus ownedTemplate = collapse(ownedViruses, "You");
        Virus friendTemplate = collapse(friendViruses, "Friend");
        return combine(ownedTemplate, friendTemplate);
    }

    private static Virus collapse(List<Virus> viruses, String defaultCarrier) {
        if (viruses == null || viruses.isEmpty()) {
            return VirusFactory.fromSeed(defaultCarrier, defaultCarrier.toLowerCase(Locale.US) + "-fallback");
        }

        int infectivity = 0;
        int resilience = 0;
        int chaos = 0;
        String family = viruses.get(0).getFamily();
        String carrier = viruses.get(0).getCarrier();
        boolean sameFamily = true;
        boolean mutated = false;

        for (Virus virus : viruses) {
            infectivity += virus.getInfectivity().score();
            resilience += virus.getResilience().score();
            chaos += virus.getChaos().score();
            mutated = mutated || virus.hasMutation();
            if (!family.equals(virus.getFamily())) {
                sameFamily = false;
            }
        }

        int size = viruses.size();
        infectivity = average(infectivity, size);
        resilience = average(resilience, size);
        chaos = average(chaos, size);
        if (!sameFamily) {
            family = viruses.get(size - 1).getFamily();
        }

        String id = UUID.nameUUIDFromBytes((family + infectivity + resilience + chaos + carrier).getBytes()).toString();
        String name = (sameFamily ? family : "Hybrid") + " Cluster";
        Infectivity infectivityRate = Infectivity.rate(infectivity);
        Resilience resilienceValue = Resilience.of(resilience);
        Chaos chaosLevel = Chaos.level(chaos);
        String genome = VirusFactory.buildGenome(id, family, infectivityRate, resilienceValue, chaosLevel, mutated);
        return new Virus(id, name, family, carrier, infectivityRate, resilienceValue, chaosLevel, mutated, genome, "Collapsed host strain");
    }

    static Virus combine(Virus left, Virus right) {
        String dominantFamily = left.getFamily().equals(right.getFamily()) ? left.getFamily() : mixFamily(left, right);
        int infectivity = mergeStat(left.getInfectivity().score(), right.getInfectivity().score(), true);
        int resilience = mergeStat(left.getResilience().score(), right.getResilience().score(), false);
        int chaos = mergeStat(left.getChaos().score(), right.getChaos().score(), true);
        boolean mutation = shouldMutate(left, right);

        if (mutation) {
            chaos = clamp(chaos + 2);
            infectivity = clamp(infectivity + 1);
        }

        String lineage = left.getId().substring(0, 4) + right.getId().substring(0, 4);
        String id = UUID.nameUUIDFromBytes((lineage + dominantFamily + infectivity + resilience + chaos + mutation).getBytes()).toString();
        String carrier = left.getCarrier() + " x " + right.getCarrier();
        String name = dominantFamily + (mutation ? " Chimera" : " Remix");
        Infectivity infectivityRate = Infectivity.rate(infectivity);
        Resilience resilienceValue = Resilience.of(resilience);
        Chaos chaosLevel = Chaos.level(chaos);
        String genome = VirusFactory.buildGenome(id, dominantFamily, infectivityRate, resilienceValue, chaosLevel, mutation);
        String origin = "Infected from " + left.getName() + " and " + right.getName();
        return new Virus(id, name, dominantFamily, carrier, infectivityRate, resilienceValue, chaosLevel, mutation, genome, origin);
    }

    static boolean shouldMutate(Virus left, Virus right) {
        int similarity = 0;
        if (left.getFamily().equals(right.getFamily())) {
            similarity += 35;
        }
        similarity += 10 - Math.abs(left.getInfectivity().score() - right.getInfectivity().score());
        similarity += 10 - Math.abs(left.getResilience().score() - right.getResilience().score());
        similarity += 10 - Math.abs(left.getChaos().score() - right.getChaos().score());
        int mutationChance = 12 + Math.max(0, 18 - similarity);
        long seed = (long) left.getGenome().hashCode() * 31L + right.getGenome().hashCode();
        Random random = new Random(seed);
        return (Math.abs(random.nextInt()) % 100) < mutationChance;
    }

    private static String mixFamily(Virus left, Virus right) {
        return left.getFamily().substring(0, 2) + right.getFamily().substring(Math.max(0, right.getFamily().length() - 2));
    }

    private static int mergeStat(int left, int right, boolean preferEnergy) {
        if (Math.abs(left - right) <= 1) {
            return clamp((left + right) / 2 + (preferEnergy ? 1 : 0));
        }
        return clamp((left * 2 + right) / 3);
    }

    private static int average(int total, int size) {
        return clamp(Math.max(1, total / size));
    }

    private static int clamp(int value) {
        if (value < 1) {
            return 1;
        }
        if (value > 10) {
            return 10;
        }
        return value;
    }
}