package com.example.funfection.engine;

import com.example.funfection.model.Chaos;
import com.example.funfection.model.Infectivity;
import com.example.funfection.model.Resilience;
import com.example.funfection.model.Virus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Combines owned and imported viruses into deterministic offspring strains.
 *
 * <p>The engine works in two stages:</p>
 * <p>First, each side of an infection is collapsed into a template strain by averaging its
 * stats and reducing the selected family set to one representative family label.</p>
 * <p>Second, the two templates are merged into an offspring whose genome and mutation state
 * are derived from both parents.</p>
 *
 * <p>The engine interprets virus strings in two ways:</p>
 * <p>Invite-code strings are parsed earlier by {@link VirusFactory} into {@link Virus} values.</p>
 * <p>Genome strings are then used here as stable fingerprints when computing mutation chance,
 * so the same pair of parent genomes always yields the same mutation roll.</p>
 */
public final class InfectionEngine {

    private InfectionEngine() {
    }

    /**
     * Produces an offspring virus from the player's selected strains and the friend's strains.
     *
     * <p>Each list is first collapsed into one template virus. Those templates are then merged
     * into a single offspring strain whose family, stats, mutation flag, genome, and origin are
     * calculated deterministically from the template inputs.</p>
     *
     * @param ownedViruses selected local viruses to use as the left-side parent pool
     * @param friendViruses imported or generated friend viruses to use as the right-side pool
     * @return newly combined offspring virus
     */
    public static Virus infect(List<Virus> ownedViruses, List<Virus> friendViruses) {
        Virus ownedTemplate = collapse(ownedViruses, "You");
        Virus friendTemplate = collapse(friendViruses, "Friend");
        return combine(ownedTemplate, friendTemplate);
    }

    /**
     * Reduces a list of viruses into a single template strain.
     *
     * <p>The template is a normalized parent used for the final merge step. Infectivity,
     * resilience, and chaos are averaged across the selected viruses. If all selected viruses
     * share a family, that family is preserved; otherwise the last selected family becomes the
     * representative family label. Any mutation on any input marks the collapsed template as
     * mutated, which is then reflected in the generated genome marker.</p>
     *
     * @param viruses source set to collapse
     * @param defaultCarrier fallback carrier name used when the source set is empty
     * @return collapsed template virus used as a parent in final combination
     */
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

    /**
     * Merges two template viruses into an offspring strain.
     *
     * <p>Family selection favors an exact family match and otherwise creates a mixed family code.
     * The three stats are merged independently, mutation is decided from parent similarity and
     * genome fingerprints, and mutation then applies a small infectivity and chaos boost before
     * the final genome string is rebuilt.</p>
     *
     * @param left collapsed left-side template
     * @param right collapsed right-side template
     * @return offspring virus representing the final infection result
     */
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

    /**
     * Decides whether two parent templates produce a mutation.
     *
     * <p>The mutation chance starts from a small base risk and increases when the parents are
     * dissimilar. Matching families and close stat values increase similarity and therefore lower
     * mutation pressure. The final random roll is deterministic because it is seeded from both
     * parent genome strings, making the outcome stable for the same pair of parents.</p>
     *
     * @param left left-side parent template
     * @param right right-side parent template
     * @return {@code true} when the combined strain mutates
     */
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

    private static int average(int item, int size) {
        return clamp(Math.max(1, item / size));
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