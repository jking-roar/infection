package com.kingjoshdavid.funfection.engine;

import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.model.*;
import com.kingjoshdavid.funfection.model.MadScientist;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Creates virus instances from seeds, invite strings, and compact genome metadata.
 *
 * <p>This class defines the two main string formats used by the engine:</p>
 *
 * <p>Invite code layout:</p>
 * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier[:generation]}</p>
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

    private static final String SIMULATED_FRIEND_FLAG = "[SIMULATED]";
    private static final int STARTER_GENERATION = 1;
    private static final MadScientist[] MAD_SCIENTISTS = {
            new MadScientist("Professor", "Tesla"),
            new MadScientist("Doctor", "Curie"),
            new MadScientist("Doc", "Brown"),
            new MadScientist("Professor", "Xavier"),
            new MadScientist("The", "Gutter Man")
    };

    private static final String[] FAMILIES = {
            "Spark","Echo","Mirth","Glitch","Bloom","Pulse",
            "Aether","Bramble","Cinder","Drizzle","Ember","Fable",
            "Glimmer","Hollow","Icicle","Jumble","Kindle","Lattice",
            "Murmur","Nimbus","Oracle","Pollen","Quiver","Ripple",
            "Sprocket","Tangle","Umber","Velour","Wobble","Xyloid",
            "Yonder","Zephyr",

            // breadth additions
            "Axon","Byway","Cwm","Djin","Eddy","Fjord",
            "Gyoza","Hyrax","Ibex","Jowl","Krait","Lyric",
            "Myrrh","Nexus","Oxbow","Pylon","Quark","Rhyme",
            "Skald","Tizzy","Uvula","Voxel","Wrack","Xebec",
            "Yolk","Zymic"
    };

    private static final String[] PREFIXES = {
            "Neon","Velvet","Wild","Lucky","Static","Sunny",
            "Amber","Brisk","Curly","Dizzy","Electric","Frosty",
            "Golden","Hazy","Icy","Jazzy","Keen","Lunar",
            "Misty","Nimble","Odd","Prickly","Quaint","Rusty",
            "Spicy","Twinkly","Ultra","Vivid","Whimsy","Xeno",
            "Young","Zesty",

            // breadth additions
            "Awkward","Breezy","Crux","Dapple","Ebon","Fuzzy",
            "Gawky","Hushed","Inky","Jaunty","Knobby","Lopsided",
            "Murky","Nubby","Ochre","Pithy","Quirky","Ragged",
            "Sylvan","Thorny","Uneven","Vortex","Wonky","Xylic",
            "Yappy","Zonal"
    };

    private static final String[] SUFFIXES = {
            "Sneezle","Wiggle","Giggle","Whisper","Bouncer","Fizzle",
            "Bumble","Crackle","Doodle","Flitter","Glimpse","Hiccup",
            "Jingle","Kabloo","Lobber","Mizzle","Nuzzle","Puffin",
            "Quibble","Razzle","Snicker","Tizzy","Uproar","Vizzle",
            "Wobble","Xizzle","Yapper","Zinger",

            // breadth additions
            "Babble","Chirp","Doink","Flump","Grizzle","Honk",
            "Jounce","Klaxon","Lurk","Mump","Nerp","Plonk",
            "Quonk","Rumpus","Skitter","Thunk","Unzip","Vroom",
            "Whomp","Xyzzle","Yowl","Zonk"
    };

    private VirusFactory() {
    }

    /**
     * Creates the initial set of lab-owned viruses.
     *
     * <p>Each starter virus is derived from a stable text seed so the same carrier and
      * seed string always produce the same family, stats, mutation flag, genome, and
      * generation baseline.</p>
     *
     * @return deterministic starter strains for a fresh repository
     */
    public static List<Virus> createStarterViruses() {
        UserProfile userProfile = UserProfileRepository.getCurrentUser();
        List<Virus> viruses = new ArrayList<>();
        viruses.add(createStarterVirus(userProfile, "starter-alpha"));
        viruses.add(createStarterVirus(userProfile, "starter-beta"));
        viruses.add(createStarterVirus(userProfile, "starter-gamma"));
        viruses.add(createStarterVirus(userProfile, "starter-delta"));
        return viruses;
    }

    private static Virus createStarterVirus(UserProfile userProfile, String seed) {
        Virus starter = fromSeed(userProfile.getUserName(), seed,
                VirusOrigin.seededByUser(userProfile.getId(), userProfile.getUserName()));
        return starter.withGeneration(STARTER_GENERATION);
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
        return fromSeed(carrier, seed, VirusOrigin.seededInLab());
    }

    private static Virus fromSeed(String carrier, String seed, VirusOrigin origin) {
        Random random = new Random(seed.hashCode());
        String id = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
        String family = FAMILIES[random.nextInt(FAMILIES.length)];
        String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
        String suffix = SUFFIXES[random.nextInt(SUFFIXES.length)];
        int infectivity = 1 + (random.nextInt(10));
        int resilience = 1 + (random.nextInt(10));
        int chaos = 1 + (random.nextInt(10));
        boolean mutation = random.nextDouble() < 0.12;
        Infectivity infectivityRate = Infectivity.rate(infectivity);
        Resilience resilienceValue = Resilience.of(resilience);
        Chaos chaosLevel = Chaos.level(chaos);
        String genome = buildGenome(id, family, infectivityRate, resilienceValue, chaosLevel, mutation);
        return new Virus(id, prefix, suffix, family, carrier, infectivityRate, resilienceValue, chaosLevel, mutation, genome, origin);
    }

    /**
     * Creates a virus discovered by scanning a QR code or barcode in the wild.
     *
     * <p>The scanned raw value is used as a deterministic seed so the same code always
     * produces the same strain. The wild-scan seed is stored internally on the virus for
     * duplicate-discovery tracking but is never shown to the player.</p>
     *
     * @param seed     raw scanned content from the QR code or barcode
     * @param isQrCode {@code true} if the source was a QR code; {@code false} for a barcode
     * @return a virus tagged as found in the wild, carrying the raw seed internally
     */
    public static Virus createWildVirus(String seed, boolean isQrCode) {
        String effectiveSeed = seed == null ? UUID.randomUUID().toString() : seed.trim();
        if (effectiveSeed.isEmpty()) {
            effectiveSeed = UUID.randomUUID().toString();
        }
        UserProfile userProfile = UserProfileRepository.getCurrentUser();
        VirusOrigin origin = isQrCode
                ? VirusOrigin.foundInWildFromQr()
                : VirusOrigin.foundInWildFromBarcode();
        Virus virus = fromSeed(userProfile.getUserName(), effectiveSeed, origin);
        return virus.withWildSeed(effectiveSeed);
    }

    /**
     * Creates a lab virus from user-provided seed text or a random fallback seed.
     *
     * <p>When the supplied text contains visible characters, the trimmed value is used as the
     * deterministic seed. Blank input generates a fresh random UUID seed so repeated taps can
     * still create distinct strains without any text entry.</p>
     *
     * @param seedInput optional player-entered seed text
     * @return lab-created virus based on the trimmed seed or a random fallback
     */
    public static Virus createLabVirus(String seedInput) {
        String normalizedSeed = seedInput == null ? "" : seedInput.trim();
        String effectiveSeed = normalizedSeed.isEmpty() ? UUID.randomUUID().toString() : normalizedSeed;
        UserProfile userProfile = UserProfileRepository.getCurrentUser();
        return fromSeed(userProfile.getUserName(), effectiveSeed,
                VirusOrigin.seededByUser(userProfile.getId(), userProfile.getUserName()));
    }

    /**
     * Parses one or more newline-delimited invite-code entries into virus instances.
     *
     * <p>Each non-empty line is expected to use the serialized share-code format from
      * {@code Virus.toShareCode()}:</p>
      *
    * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier[:generation[:originPayload]]}</p>
     *
     * <p>Malformed rows are ignored so a partially valid invite block can still import the
     * entries that parse cleanly.</p>
     *
     * @param inviteCode multiline block of invite-code rows
     * @return parsed viruses, excluding malformed entries
     */
    public static List<Virus> parseInviteCode(String inviteCode) {
        List<Virus> viruses = new ArrayList<>();
        if (inviteCode == null) {
            return viruses;
        }
        String[] entries = inviteCode.split("\\n");
        for (String entry : entries) {
            Virus virus = parseSingle(entry.trim());
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
    * <p>{@code id:family:infectivity:resilience:chaos:mutation:genome:name:carrier[:generation[:originPayload]]}</p>
     *
       * <p>The mutation field uses {@code 1} for mutated strains and {@code 0} for stable
       * strains. The optional trailing generation field preserves lineage depth across sharing.
       * When it is absent, legacy invite codes default to {@code 1}. The genome
      * token is trusted as imported metadata rather than recomputed during parsing, which
      * preserves the sender's exact shared fingerprint.</p>
     *
     * @param encoded single invite-code row
     * @return imported virus, or {@code null} when the row is empty or malformed
     */
    public static Virus parseSingle(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
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
            int generation = pieces.length > 9 ? Math.max(1, Integer.parseInt(pieces[9])) : 1;
            VirusOrigin sharedOrigin = pieces.length > 10 ? VirusOrigin.fromSharePayload(pieces[10]) : null;
            VirusOrigin importedOrigin = VirusOrigin.importedFromInvite(sharedOrigin, carrier);
            return new Virus(id, name, family, carrier,
                    Infectivity.rate(infectivity),
                    Resilience.of(resilience),
                    Chaos.level(chaos),
                    mutation,
                    genome,
                    importedOrigin,
                    generation);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /**
     * Creates a temporary friend strain when the user does not provide an invite code.
     *
     * <p>This uses the same deterministic seed pipeline as lab-created strains, but carries
     * explicit fallback provenance so random friend stand-ins are not presented as lab seeds.</p>
     *
     * @return seeded stand-in virus representing a random friend contribution
     */
    public static Virus createRandomFriendVirus() {
        Random random = new Random();
        MadScientist scientist = MAD_SCIENTISTS[Math.abs(random.nextInt()) % MAD_SCIENTISTS.length];
        String guestName = scientist.getDisplayName() + " " + SIMULATED_FRIEND_FLAG;
        String seed = guestName.toLowerCase(Locale.US) + ":" + UUID.randomUUID().toString();
        return fromSeed(guestName, seed, VirusOrigin.randomFriendFallback(guestName));
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
    * <p>The genome is not a biological simulation or a stable parse contract. It is a readable
    * fingerprint used by the infection engine for deterministic mutation seeding and by the UI
    * for flavor text. Code should treat the genome as display-oriented metadata, not as a field
    * that needs to be losslessly decoded back into gameplay state.</p>
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