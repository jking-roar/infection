package com.kingjoshdavid.funfection.engine;

import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.model.*;

import java.nio.charset.StandardCharsets;
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
 * <p>Lineage generations start at one for seeded strains. New offspring generations are derived as
 * {@code max(parentGenerations) + 1}.</p>
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
     * <p>Stat roles in the final result:</p>
     * <p>{@code infectivity}: drives spread pressure and receives an extra +1 boost on mutation.</p>
     * <p>{@code resilience}: drives survivability/stability and is never mutation-boosted.</p>
     * <p>{@code chaos}: drives volatility and receives an extra +2 boost on mutation.</p>
     *
     * @param ownedViruses selected local viruses to use as the left-side parent pool
     * @param friendViruses imported or generated friend viruses to use as the right-side pool
     * @return newly combined offspring virus
     */
    public static Virus infect(List<Virus> ownedViruses, List<Virus> friendViruses) {
        String localUserName = UserProfileRepository.getCurrentUser().getUserName();
        Virus ownedTemplate = collapse(ownedViruses, localUserName);
        Virus friendTemplate = collapse(friendViruses, "Friend");
        return combine(ownedTemplate, friendTemplate);
    }

    /**
     * Produces an offspring virus from only locally selected strains.
     *
     * <p>This bypasses invite-code parsing and friend/random fallback behavior. The selected
     * strains are collapsed into one local template, then emitted as a local-only offspring.
     * Offspring generation is computed as the collapsed template generation plus one.</p>
     *
     * @param ownedViruses selected local viruses to combine
     * @return newly combined local offspring virus
     */
    public static Virus infectLocal(List<Virus> ownedViruses) {
        String localUserName = UserProfileRepository.getCurrentUser().getUserName();
        Virus merged = collapse(ownedViruses, localUserName);
        String id = UUID.nameUUIDFromBytes((merged.getId() + "-local")
            .getBytes(StandardCharsets.UTF_8)).toString();
        String prefix = merged.getPrefix();
        String suffix = merged.getSuffix();
        String carrier = merged.getCarrier();
        Infectivity infectivityRate = Infectivity.rate(merged.getInfectivity().score());
        Resilience resilienceValue = Resilience.of(merged.getResilience().score());
        Chaos chaosLevel = Chaos.level(merged.getChaos().score());
        String genome = VirusFactory.buildGenome(id, merged.getFamily(), infectivityRate, resilienceValue, chaosLevel,
                merged.hasMutation());
        VirusOrigin origin = VirusOrigin.combinedLocally(merged.getOriginInfo());
        int generation = merged.getGeneration() + 1;
        String rawSeed = "local:" + seedSourceOf(merged) + ":" + id;
        long seed = SeedUtil.seedFromString(rawSeed);
        return new Virus(id, prefix, suffix, merged.getFamily(), carrier, infectivityRate, resilienceValue, chaosLevel,
            merged.hasMutation(), genome, origin, generation, "Local Mix", rawSeed, seed);
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
     * <p>Per-stat averaging formula:</p>
     * <p>{@code averaged = clamp(max(1, sum / size))}</p>
     *
     * <p>This keeps each axis in the engine-normalized range {@code 1..10} before final merge.</p>
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
        int maxGeneration = 1;
        String family = normalizeFamily(viruses.get(0).getFamily());
        String carrier = normalizeCarrier(viruses.get(0).getCarrier(), defaultCarrier);
        boolean sameFamily = true;
        boolean mutated = false;

        for (Virus virus : viruses) {
            infectivity += virus.getInfectivity().score();
            resilience += virus.getResilience().score();
            chaos += virus.getChaos().score();
            maxGeneration = Math.max(maxGeneration, virus.getGeneration());
            mutated = mutated || virus.hasMutation();
            String candidateFamily = normalizeFamily(virus.getFamily());
            if (!family.equals(candidateFamily)) {
                sameFamily = false;
            }
        }

        int size = viruses.size();
        infectivity = average(infectivity, size);
        resilience = average(resilience, size);
        chaos = average(chaos, size);
        if (!sameFamily) {
            family = normalizeFamily(viruses.get(size - 1).getFamily());
        }

        String id = UUID.nameUUIDFromBytes((family + infectivity + resilience + chaos + carrier)
            .getBytes(StandardCharsets.UTF_8)).toString();
        String prefix = chooseLowerInfectionPrefix(viruses);
        String suffix = chooseHigherInfectionSuffix(viruses);
        String productionContext = sameFamily ? "Cluster" : "Hybrid";
        Infectivity infectivityRate = Infectivity.rate(infectivity);
        Resilience resilienceValue = Resilience.of(resilience);
        Chaos chaosLevel = Chaos.level(chaos);
        String genome = VirusFactory.buildGenome(id, family, infectivityRate, resilienceValue, chaosLevel, mutated);
        VirusOrigin origin = VirusOrigin.collapsed(viruses);
        String rawSeed = buildCollapsedRawSeed(viruses, family, infectivity, resilience, chaos, mutated);
        long seed = SeedUtil.seedFromString(rawSeed);
        return new Virus(id, prefix, suffix, family, carrier, infectivityRate, resilienceValue, chaosLevel, mutated, genome,
            origin, maxGeneration, productionContext, rawSeed, seed);
    }

    /**
     * Merges two template viruses into an offspring strain.
     *
     * <p>Family selection favors an exact family match and otherwise creates a mixed family code.
     * The three stats are merged independently, mutation is decided from parent similarity and
     * genome fingerprints, and mutation then applies a small infectivity and chaos boost before
     * the final genome string is rebuilt.</p>
     *
     * <p>Per-stat merge behavior:</p>
     * <p>{@code infectivity = mergeStat(left.infectivity, right.infectivity, true)}</p>
     * <p>{@code resilience = mergeStat(left.resilience, right.resilience, false)}</p>
     * <p>{@code chaos = mergeStat(left.chaos, right.chaos, true)}</p>
     * <p>{@code prefix = parent with lower generation (tie -> left)}</p>
     * <p>{@code suffix = parent with higher generation (tie -> right)}</p>
      * <p>{@code generation = max(left.generation, right.generation) + 1}</p>
     *
     * <p>If mutation occurs:</p>
     * <p>{@code infectivity = clamp(infectivity + 1)}</p>
     * <p>{@code chaos = clamp(chaos + 2)}</p>
     *
     * <p>Interpretation: infectivity and chaos are the mutation-sensitive axes (spread and
     * volatility), while resilience is the stabilizing axis that is merged but not mutation-buffed.</p>
     *
     * @param left collapsed left-side template
     * @param right collapsed right-side template
     * @return offspring virus representing the final infection result
     */
    static Virus combine(Virus left, Virus right) {
        String leftFamily = normalizeFamily(left.getFamily());
        String rightFamily = normalizeFamily(right.getFamily());
        String dominantFamily = leftFamily.equals(rightFamily) ? leftFamily : mixFamily(left, right);
        int infectivity = mergeStat(left.getInfectivity().score(), right.getInfectivity().score(), true);
        int resilience = mergeStat(left.getResilience().score(), right.getResilience().score(), false);
        int chaos = mergeStat(left.getChaos().score(), right.getChaos().score(), true);
        int generation = Math.max(left.getGeneration(), right.getGeneration()) + 1;
        boolean mutation = shouldMutate(left, right);

        if (mutation) {
            chaos = clamp(chaos + 2);
            infectivity = clamp(infectivity + 1);
        }

        String lineage = shortIdPrefix(left.getId()) + shortIdPrefix(right.getId());
        String id = UUID.nameUUIDFromBytes((lineage + dominantFamily + infectivity + resilience + chaos + mutation)
            .getBytes(StandardCharsets.UTF_8)).toString();
        String carrier = left.getCarrier() + " x " + right.getCarrier();
        String prefix = chooseLowerInfectionPrefix(left, right);
        String suffix = chooseHigherInfectionSuffix(left, right);
        Infectivity infectivityRate = Infectivity.rate(infectivity);
        Resilience resilienceValue = Resilience.of(resilience);
        Chaos chaosLevel = Chaos.level(chaos);
        String genome = VirusFactory.buildGenome(id, dominantFamily, infectivityRate, resilienceValue, chaosLevel, mutation);
        VirusOrigin origin = VirusOrigin.infectedFrom(left.getName(), left.getOriginInfo(), right.getName(), right.getOriginInfo());
        String rawSeed = "combine:" + seedSourceOf(left) + ":" + seedSourceOf(right) + ":" + id;
        long seed = SeedUtil.seedFromString(rawSeed);
        return new Virus(id, prefix, suffix, dominantFamily, carrier, infectivityRate, resilienceValue, chaosLevel, mutation,
                genome, origin, generation, null, rawSeed, seed);
    }

    private static String buildCollapsedRawSeed(List<Virus> viruses,
                                                String family,
                                                int infectivity,
                                                int resilience,
                                                int chaos,
                                                boolean mutated) {
        StringBuilder source = new StringBuilder();
        for (Virus virus : viruses) {
            if (source.length() > 0) {
                source.append('|');
            }
            source.append(seedSourceOf(virus));
        }
        return "collapse:" + family + ':' + infectivity + ':' + resilience + ':' + chaos + ':'
                + (mutated ? '1' : '0') + ':' + source;
    }

    private static long seedSourceOf(Virus virus) {
        return virus.getSeed();
    }

    private static String chooseLowerInfectionPrefix(Virus left, Virus right) {
        return left.getGeneration() <= right.getGeneration() ? left.getPrefix() : right.getPrefix();
    }

    private static String chooseHigherInfectionSuffix(Virus left, Virus right) {
        return left.getGeneration() > right.getGeneration() ? left.getSuffix() : right.getSuffix();
    }

    private static String chooseLowerInfectionPrefix(List<Virus> viruses) {
        Virus chosen = viruses.get(0);
        for (int i = 1; i < viruses.size(); i++) {
            Virus candidate = viruses.get(i);
            if (candidate.getGeneration() < chosen.getGeneration()) {
                chosen = candidate;
            }
        }
        return chosen.getPrefix();
    }

    private static String chooseHigherInfectionSuffix(List<Virus> viruses) {
        Virus chosen = viruses.get(0);
        for (int i = 1; i < viruses.size(); i++) {
            Virus candidate = viruses.get(i);
            if (candidate.getGeneration() >= chosen.getGeneration()) {
                chosen = candidate;
            }
        }
        return chosen.getSuffix();
    }

    /**
     * Decides whether two parent templates produce a mutation.
     *
     * <p>The mutation chance starts from a small base risk and increases when the parents are
     * dissimilar. Matching families and close stat values increase similarity and therefore lower
     * mutation pressure. The final random roll is deterministic because it is seeded from both
     * parent genome strings, making the outcome stable for the same pair of parents.</p>
     *
     * <p>Similarity formula:</p>
     * <p>{@code similarity = familyBonus + infectivityCloseness + resilienceCloseness + chaosCloseness}</p>
     * <p>{@code familyBonus = 35 when families match, otherwise 0}</p>
     * <p>{@code infectivityCloseness = 10 - abs(left.infectivity - right.infectivity)}</p>
     * <p>{@code resilienceCloseness = 10 - abs(left.resilience - right.resilience)}</p>
     * <p>{@code chaosCloseness = 10 - abs(left.chaos - right.chaos)}</p>
     *
     * <p>Chance formula:</p>
     * <p>{@code mutationChance = 12 + max(0, 18 - similarity)}</p>
     *
     * <p>Interpretation: large differences in any stat reduce similarity and raise mutation odds.
     * Chaos contributes equally to this pressure calculation, then has the largest post-mutation
     * boost in {@link #combine(Virus, Virus)}.</p>
     *
     * @param left left-side parent template
     * @param right right-side parent template
     * @return {@code true} when the combined strain mutates
     */
    static boolean shouldMutate(Virus left, Virus right) {
        int similarity = 0;
        if (normalizeFamily(left.getFamily()).equals(normalizeFamily(right.getFamily()))) {
            similarity += 35;
        }
        similarity += 10 - Math.abs(left.getInfectivity().score() - right.getInfectivity().score());
        similarity += 10 - Math.abs(left.getResilience().score() - right.getResilience().score());
        similarity += 10 - Math.abs(left.getChaos().score() - right.getChaos().score());
        int mutationChance = 12 + Math.max(0, 18 - similarity);
        long seed = (long) left.getGenome().hashCode() * 31L + right.getGenome().hashCode();
        Random random = new Random(seed);
        // Math.abs(Integer.MIN_VALUE) overflows to a negative value; Math.max(0, ...) corrects that.
        return (Math.max(0, Math.abs(random.nextInt())) % 100) < mutationChance;
    }

    /**
     * Builds a mixed-family label when parent families differ.
     *
     * <p>Formula: first two chars of left family + last two chars of right family.</p>
     */
    private static String mixFamily(Virus left, Virus right) {
        String leftFamily = normalizeFamily(left.getFamily());
        String rightFamily = normalizeFamily(right.getFamily());
        int leftEnd = Math.min(2, leftFamily.length());
        int rightStart = Math.max(0, rightFamily.length() - 2);
        return leftFamily.substring(0, leftEnd) + rightFamily.substring(rightStart);
    }

    private static String shortIdPrefix(String id) {
        String value = normalizeToken(id, "virus");
        return value.substring(0, Math.min(4, value.length()));
    }

    private static String normalizeFamily(String family) {
        return normalizeToken(family, "UnknownFamily");
    }

    private static String normalizeCarrier(String carrier, String fallbackCarrier) {
        return normalizeToken(carrier, normalizeToken(fallbackCarrier, "Unknown Carrier"));
    }

    private static String normalizeToken(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        String safeFallback = fallback == null ? "" : fallback.trim();
        return safeFallback.isEmpty() ? "Unknown" : safeFallback;
    }

    /**
     * Merges one stat axis from two parents, then clamps into gameplay bounds.
     *
     * <p>When values are close ({@code abs(left - right) <= 1}), use near-average merge:</p>
     * <p>{@code result = clamp((left + right) / 2 + energyBias)}</p>
     * <p>{@code energyBias = 1 when preferEnergy is true, otherwise 0}</p>
     *
     * <p>Otherwise use weighted merge biased toward the left template:</p>
     * <p>{@code result = clamp((left * 2 + right) / 3 + energyBias)}</p>
     * <p>{@code energyBias = 1 when preferEnergy is true, otherwise 0}</p>
     *
     * <p>Engine usage: {@code preferEnergy=true} for infectivity and chaos, and
     * {@code preferEnergy=false} for resilience.</p>
     */
    private static int mergeStat(int left, int right, boolean preferEnergy) {
        if (Math.abs(left - right) <= 1) {
            return clamp((left + right) / 2 + (preferEnergy ? 1 : 0));
        }
        return clamp((left * 2 + right) / 3 + (preferEnergy ? 1 : 0));
    }

    /**
     * Computes integer average for a collapsed stat axis and clamps it to valid bounds.
     *
     * <p>Formula: {@code clamp(max(1, item / size))}</p>
     */
    private static int average(int item, int size) {
        return clamp(item / size);
    }

    /**
     * Constrains any stat value to the engine gameplay range.
     *
     * <p>Formula: {@code min(10, max(1, value))}</p>
     */
    private static int clamp(int value) {
        return Math.min(10, Math.max(value, 1));
    }
}
