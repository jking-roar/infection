package com.example.funfection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VirusRepository {

    private static final List<Virus> COLLECTION = new ArrayList<Virus>();

    private VirusRepository() {
    }

    public static void ensureSeeded() {
        if (COLLECTION.isEmpty()) {
            COLLECTION.addAll(VirusFactory.createStarterViruses());
        }
    }

    public static List<Virus> getViruses() {
        ensureSeeded();
        return new ArrayList<Virus>(COLLECTION);
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

    public static List<Virus> pickByIds(List<String> ids) {
        ensureSeeded();
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Virus> matches = new ArrayList<Virus>();
        for (String id : ids) {
            Virus virus = getVirusById(id);
            if (virus != null) {
                matches.add(virus);
            }
        }
        return matches;
    }
}