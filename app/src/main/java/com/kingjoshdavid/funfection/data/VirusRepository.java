package com.kingjoshdavid.funfection.data;

import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VirusRepository {

    private static final List<Virus> COLLECTION = new ArrayList<>();

    public enum PurgeResult {
        REMOVED,
        MISSING,
        BLOCKED_LAST
    }

    private VirusRepository() {
    }

    public static void ensureSeeded() {
        if (COLLECTION.isEmpty()) {
            COLLECTION.addAll(VirusFactory.createStarterViruses());
        }
    }

    public static List<Virus> getViruses() {
        ensureSeeded();
        return new ArrayList<>(COLLECTION);
    }

    public static Virus getVirusById(String id) {
        ensureSeeded();
        for (Virus virus : COLLECTION) {
            if (virus.getId().equals(id)) {
                return virus;
            }
        }
        return null;
    }

    public static void addVirus(Virus virus) {
        ensureSeeded();
        COLLECTION.add(0, virus);
    }

    public static void replaceVirus(Virus virus) {
        ensureSeeded();
        for (int index = 0; index < COLLECTION.size(); index++) {
            if (COLLECTION.get(index).getId().equals(virus.getId())) {
                COLLECTION.set(index, virus);
                return;
            }
        }
        COLLECTION.add(0, virus);
    }

    public static boolean removeVirusById(String id) {
        return purgeVirusById(id) == PurgeResult.REMOVED;
    }

    public static PurgeResult getPurgeStatus(String id) {
        ensureSeeded();
        if (id == null || id.trim().isEmpty()) {
            return PurgeResult.MISSING;
        }
        if (COLLECTION.size() <= 1) {
            return findVirusIndexById(id) >= 0 ? PurgeResult.BLOCKED_LAST : PurgeResult.MISSING;
        }
        return findVirusIndexById(id) >= 0 ? PurgeResult.REMOVED : PurgeResult.MISSING;
    }

    public static PurgeResult purgeVirusById(String id) {
        ensureSeeded();
        PurgeResult status = getPurgeStatus(id);
        if (status != PurgeResult.REMOVED) {
            return status;
        }
        int index = findVirusIndexById(id);
        if (index < 0) {
            return PurgeResult.MISSING;
        }
        COLLECTION.remove(index);
        return PurgeResult.REMOVED;
    }

    private static int findVirusIndexById(String id) {
        if (id == null) {
            return -1;
        }
        for (int index = 0; index < COLLECTION.size(); index++) {
            if (COLLECTION.get(index).getId().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    public static List<Virus> pickByIds(List<String> ids) {
        ensureSeeded();
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Virus> matches = new ArrayList<>();
        for (String id : ids) {
            Virus virus = getVirusById(id);
            if (virus != null) {
                matches.add(virus);
            }
        }
        return matches;
    }
}