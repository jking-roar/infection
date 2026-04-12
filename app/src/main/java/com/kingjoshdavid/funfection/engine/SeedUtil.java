package com.kingjoshdavid.funfection.engine;

import java.nio.charset.StandardCharsets;

public final class SeedUtil {
    private SeedUtil() {}

    // FNV-1a 64-bit, then Murmur3 finalizer for better avalanche.
    public static long seedFromString(String input) {
        byte[] bytes = (input == null ? "" : input).getBytes(StandardCharsets.UTF_8);

        long hash = 0xcbf29ce484222325L; // FNV offset basis
        for (byte b : bytes) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;      // FNV prime
        }

        // Final mix (MurmurHash3 fmix64-style)
        hash ^= (hash >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= (hash >>> 33);
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= (hash >>> 33);

        return hash;
    }
}
