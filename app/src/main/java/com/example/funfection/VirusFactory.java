package com.example.funfection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public final class VirusFactory {

    private static final String[] FAMILIES = {"Spark", "Echo", "Mirth", "Glitch", "Bloom", "Pulse"};
    private static final String[] PREFIXES = {"Neon", "Velvet", "Wild", "Lucky", "Static", "Sunny"};
    private static final String[] SUFFIXES = {"Sneezle", "Wiggle", "Giggle", "Whisper", "Bouncer", "Fizzle"};

    private VirusFactory() {
    }

    public static List<Virus> createStarterViruses() {
        List<Virus> viruses = new ArrayList<Virus>();
        viruses.add(fromSeed("Jerry", "starter-alpha"));
        viruses.add(fromSeed("Sam", "starter-beta"));
        viruses.add(fromSeed("Alex", "starter-gamma"));
        viruses.add(fromSeed("Morgan", "starter-delta"));
        return viruses;
    }

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
        String genome = buildGenome(id, family, infectivity, resilience, chaos, mutation);
        return new Virus(id, name, family, carrier, infectivity, resilience, chaos, mutation, genome, "Seeded in lab");
    }

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
            return new Virus(id, name, family, carrier, infectivity, resilience, chaos, mutation, genome, "Imported from invite");
        } catch (NumberFormatException error) {
            return null;
        }
    }

    public static Virus createRandomFriendVirus() {
        String guestName = "Friend-" + Integer.toString(Math.abs(new Random().nextInt()) % 99 + 1, 10);
        return fromSeed(guestName, guestName.toLowerCase(Locale.US));
    }

    public static String buildGenome(String id,
                                     String family,
                                     int infectivity,
                                     int resilience,
                                     int chaos,
                                     boolean mutation) {
        String compactId = id.replace("-", "");
        String familyCode = family.substring(0, Math.min(3, family.length())).toUpperCase(Locale.US);
        String mutationCode = mutation ? "M" : "S";
        return familyCode + "-" + compactId.substring(0, 6) + "-" + infectivity + resilience + chaos + "-" + mutationCode;
    }
}