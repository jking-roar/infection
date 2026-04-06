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
 * Creates virus instances from seeds, invite strings, and compact genome metadata.
 *
 * <p>This class defines the two main string formats used by the engine:</p>
 *
 * <p>Invite code layout:</p>
 * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier}</p>
 *
 * <p>Genome layout:</p>
 * <p>{@code FAM-ABC123-xyz-M}</p>
 *
 * <p>Where the genome sections mean:</p>
 * <p>{@code FAM} is the first three letters of the family in upper case.</p>
 * <p>{@code ABC123} is the first six characters of the UUID with dashes removed.</p>
 * <p>{@code xyz} is the concatenated infectivity, resilience, and chaos scores.</p>
 * <p>{@code M} or {@code S} marks a mutated or stable strain.</p>
 */
public final class VirusFactory {

    private static final String[] FAMILIES = {"Spark", "Echo", "Mirth", "Glitch", "Bloom", "Pulse"};
    private static final String[] PREFIXES = {"Neon", "Velvet", "Wild", "Lucky", "Static", "Sunny"};
    private static final String[] SUFFIXES = {"Sneezle", "Wiggle", "Giggle", "Whisper", "Bouncer", "Fizzle"};

    private VirusFactory() {
    }

    /**
     * Creates the initial set of lab-owned viruses.
     *
     * <p>Each starter virus is derived from a stable text seed so the same carrier and
     * seed string always produce the same family, stats, mutation flag, and genome.</p>
     *
     * @return deterministic starter strains for a fresh repository
     */
    public static List<Virus> createStarterViruses() {
        List<Virus> viruses = new ArrayList<Virus>();
        viruses.add(fromSeed("Jerry", "starter-alpha"));
        viruses.add(fromSeed("Sam", "starter-beta"));
        viruses.add(fromSeed("Alex", "starter-gamma"));
        viruses.add(fromSeed("Morgan", "starter-delta"));
        return viruses;
    }

    /**
     * Creates a deterministic virus from a carrier name and arbitrary seed string.
     *
     * <p>The seed drives a pseudo-random generator that selects the family, display name,
     * scores, and initial mutation state. The resulting UUID and genome are therefore stable
     * for the same input seed.</p>
     *
     * @param carrier fictional host label attached to the resulting virus
     * @param seed deterministic source used to generate all virus properties
     * @return generated virus with reproducible identity and stats
     */
    public static Virus fromSeed(String carrier, String seed) {
        Random random = new Random(seed.hashCode());
        String id = UUID.nameUUIDFromBytes(seed.getBytes()).toString();
        String family = FAMILIES[Math.abs(random.nextInt()) % FAMILIES.length];
        String name = PREFIXES[Math.abs(random.nextInt()) % PREFIXES.length] + " "
                + SUFFIXES[Math.abs(random.nextInt()) % SUFFIXES.length];
        int infectivity = 1 + Math.abs(random.nextInt()) % 10;
        int resilience = 1 + Math.abs(random.nextInt()) % 10;
        int chaos = 1 + Math.abs(random.nextInt()) % 10;
        boolean mutation = (Math.abs(random.nextInt()) % 100) < 12;
        Infectivity infectivityRate = Infectivity.rate(infectivity);
        Resilience resilienceValue = Resilience.of(resilience);
        Chaos chaosLevel = Chaos.level(chaos);
        String genome = buildGenome(id, family, infectivityRate, resilienceValue, chaosLevel, mutation);
        return new Virus(id, name, family, carrier, infectivityRate, resilienceValue, chaosLevel, mutation, genome, "Seeded in lab");
    }

    /**
     * Parses one or more newline-delimited invite-code entries into virus instances.
     *
     * <p>Each non-empty line is expected to use the serialized share-code format from
     * {@code Virus.toShareCode()}:</p>
     *
     * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier}</p>
     *
     * <p>Malformed rows are ignored so a partially valid invite block can still import the
     * entries that parse cleanly.</p>
     *
     * @param inviteCode multiline block of invite-code rows
     * @return parsed viruses, excluding malformed entries
     */
    public static List<Virus> parseInviteCode(String inviteCode) {
        List<Virus> viruses = new ArrayList<Virus>();
        if (inviteCode == null) {
            return viruses;
        }
        String[] entries = inviteCode.split("\\n");
        for (int index = 0; index < entries.length; index++) {
            Virus virus = parseSingle(entries[index].trim());
            if (virus != null) {
                viruses.add(virus);
            }
        }
        return viruses;
    }

    /**
     * Parses a single serialized invite-code row.
     *
     * <p>The expected field order is:</p>
     * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier}</p>
     *
     * <p>The mutation field uses {@code 1} for mutated strains and {@code 0} for stable
     * strains. The genome token is trusted as imported metadata rather than recomputed during
     * parsing, which preserves the sender's exact shared fingerprint.</p>
     *
     * @param encoded single invite-code row
     * @return imported virus, or {@code null} when the row is empty or malformed
     */
    public static Virus parseSingle(String encoded) {
        if (encoded == null || encoded.length() == 0) {
            return null;
        }
        String[] pieces = encoded.split(":");
        if (pieces.length < 9) {
            return null;
        }
        try {
            String id = pieces[0];
            String family = pieces[1];
            int infectivity = Integer.parseInt(pieces[2]);
            int resilience = Integer.parseInt(pieces[3]);
            int chaos = Integer.parseInt(pieces[4]);
            boolean mutation = "1".equals(pieces[5]);
            String genome = pieces[6];
            String name = pieces[7];
            String carrier = pieces[8];
                return new Virus(id, name, family, carrier,
                    Infectivity.rate(infectivity),
                    Resilience.of(resilience),
                    Chaos.level(chaos),
                    mutation,
                    genome,
                    "Imported from invite");
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /**
     * Creates a temporary friend strain when the user does not provide an invite code.
     *
     * @return seeded stand-in virus representing a random friend contribution
     */
    public static Virus createRandomFriendVirus() {
        String guestName = "Friend-" + Integer.toString(Math.abs(new Random().nextInt()) % 99 + 1, 10);
        return fromSeed(guestName, guestName.toLowerCase(Locale.US));
    }

    /**
     * Builds the compact genome string stored on every virus.
     *
     * <p>Genome format:</p>
     * <p>{@code FAMILYCODE-IDPREFIX-IRC-MARKER}</p>
     *
     * <p>Section meanings:</p>
     * <p>{@code FAMILYCODE}: first three characters of the family, upper-cased.</p>
     * <p>{@code IDPREFIX}: first six characters of the UUID with dashes removed.</p>
     * <p>{@code IRC}: infectivity, resilience, and chaos scores concatenated in that order.</p>
     * <p>{@code MARKER}: {@code M} for mutated or {@code S} for stable.</p>
     *
     * <p>The genome is not a biological simulation. It is a readable fingerprint used by the
     * infection engine for deterministic mutation seeding and by the UI for flavor text.</p>
     *
     * @param id unique virus identifier
     * @param family virus lineage label
     * @param infectivity spread-strength stat
     * @param resilience survivability stat
     * @param chaos instability stat
     * @param mutation whether the strain is mutated
     * @return compact genome token summarizing the strain
     */
    public static String buildGenome(String id,
                                     String family,
                                     Infectivity infectivity,
                                     Resilience resilience,
                                     Chaos chaos,
                                     boolean mutation) {
        String compactId = id.replace("-", "");
        String familyCode = family.substring(0, Math.min(3, family.length())).toUpperCase(Locale.US);
        String mutationCode = mutation ? "M" : "S";
        return familyCode + "-" + compactId.substring(0, 6) + "-"
                + infectivity.score() + resilience.score() + chaos.score() + "-" + mutationCode;
    }
}