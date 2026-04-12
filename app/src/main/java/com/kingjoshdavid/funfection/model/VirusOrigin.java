package com.kingjoshdavid.funfection.model;

import android.annotation.SuppressLint;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Structured provenance metadata describing how a virus reached the current player.
 */
public final class VirusOrigin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final int MAX_PATIENT_ZEROS = 2;
    private static final String SHARE_VERSION = "1";
    private static final String LAB_CARRIER = "Lab";
    private static final String SIMULATED_FLAG = "[SIMULATED]";

    private final Type type;
    private final String summary;
    private final Source directSource;
    private final List<PatientZero> patientZeros;

    private VirusOrigin(Type type, String summary, Source directSource, List<PatientZero> patientZeros) {
        this.type = type == null ? Type.LEGACY : type;
        this.summary = summary == null ? "Unknown origin" : summary;
        this.directSource = directSource;
        this.patientZeros = Collections.unmodifiableList(selectTopPatientZeros(patientZeros));
    }

    public static VirusOrigin legacy(String summary) {
        return new VirusOrigin(Type.LEGACY, summary, null, Collections.emptyList());
    }

    public static VirusOrigin seededInLab() {
        return new VirusOrigin(Type.LAB, "Seeded in lab", null, Collections.emptyList());
    }

    public static VirusOrigin seededByUser(String userId, String userName) {
        Source source = Source.real(userId, userName, 0);
        return new VirusOrigin(Type.LAB, "Seeded in lab", source, Collections.emptyList());
    }

    public static VirusOrigin randomFriendFallback(String moniker) {
        return randomFriendFallback("", moniker);
    }

    public static VirusOrigin randomFriendFallback(String sourceId, String moniker) {
        String id = sourceId == null || sourceId.trim().isEmpty()
                ? stableId("fake:" + moniker)
                : sourceId;
        Source source = Source.fake(id, moniker);
        return new VirusOrigin(Type.RANDOM_FRIEND, "Generated as random friend fallback", source,
                Collections.emptyList());
    }

    public static VirusOrigin importedFromInvite(VirusOrigin sharedOrigin, String carrier) {
        VirusOrigin base = sharedOrigin == null ? legacy("Imported from invite") : sharedOrigin;
        PatientZero primary = resolvePrimaryPatientZero(base, carrier);
        PatientZero secondary = resolveSecondaryPatientZero(base, primary);
        Source source = primary == null
                ? (base.directSource == null ? null : base.directSource.withDegree(base.directSource.isRealFriend() ? 1 : 0))
                : Source.real(primary.getId(), primary.getDisplayName(), 1);

        List<PatientZero> lineage = new ArrayList<>();
        if (primary != null) {
            lineage.add(primary.withDegree(1));
        }
        if (secondary != null) {
            lineage.add(secondary.withDegree(Math.max(1, secondary.getDegreeOfSeparation())));
        }
        return new VirusOrigin(Type.IMPORTED_INVITE, "Imported from invite", source, lineage);
    }

    public static VirusOrigin collapsed(List<Virus> viruses) {
        List<PatientZero> lineage = new ArrayList<>();
        Source directSource = null;
        if (viruses != null) {
            for (Virus virus : viruses) {
                directSource = choosePreferredDirectSource(directSource, virus.getOriginInfo().directSource);
                lineage.addAll(virus.getOriginInfo().exportLineage());
            }
        }
        return new VirusOrigin(Type.COLLAPSED, "Collapsed host strain", directSource, lineage);
    }

    public static VirusOrigin foundInWildFromQr() {
        return new VirusOrigin(Type.WILD_QR, "Found in the wild (QR code)", null, Collections.emptyList());
    }

    public static VirusOrigin foundInWildFromBarcode() {
        return new VirusOrigin(Type.WILD_BARCODE, "Found in the wild (barcode)", null, Collections.emptyList());
    }

    public static VirusOrigin combinedLocally(VirusOrigin mergedOrigin) {
        List<PatientZero> lineage = mergedOrigin == null
                ? Collections.emptyList()
                : mergedOrigin.exportLineage();
        return new VirusOrigin(Type.LOCAL_COMBINE, "Combined from local strains", null, lineage);
    }

    public static VirusOrigin infectedFrom(String leftName,
                                           VirusOrigin leftOrigin,
                                           String rightName,
                                           VirusOrigin rightOrigin) {
        PatientZero leftPrimary = primaryPatientZeroOf(leftOrigin);
        PatientZero rightPrimary = primaryPatientZeroOf(rightOrigin);
        Source source = rightPrimary == null
                ? (rightOrigin == null || rightOrigin.directSource == null
                ? null
                : rightOrigin.directSource.withDegree(rightOrigin.directSource.isRealFriend() ? 1 : 0))
                : Source.real(rightPrimary.getId(), rightPrimary.getDisplayName(), 1);

        List<PatientZero> lineage = new ArrayList<>();
        if (leftPrimary != null) {
            lineage.add(leftPrimary.withDegree(Math.max(1, leftPrimary.getDegreeOfSeparation())));
        }
        if (rightPrimary != null) {
            lineage.add(rightPrimary.withDegree(Math.max(1, rightPrimary.getDegreeOfSeparation())));
        }
        if (lineage.isEmpty() && source != null && source.isRealFriend()) {
            lineage.add(PatientZero.fromSource(source));
        }

        return new VirusOrigin(Type.FRIEND_INFECTION,
                "Infected from " + leftName + " and " + rightName,
                source,
                lineage);
    }

    public String getSummary() {
        return summary;
    }

    public boolean hasDirectSource() {
        return directSource != null;
    }

    public Source getDirectSource() {
        return directSource;
    }

    public boolean isRealFriendSource() {
        return directSource != null && directSource.isRealFriend();
    }

    public int getDegreeOfSeparation() {
        return directSource == null ? 0 : directSource.getDegreeOfSeparation();
    }

    public List<PatientZero> getPatientZeros() {
        return patientZeros;
    }

    public boolean isLabSeed() {
        return type == Type.LAB;
    }

    public boolean isWildQrSeed() {
        return type == Type.WILD_QR;
    }

    public boolean isWildBarcodeSeed() {
        return type == Type.WILD_BARCODE;
    }

    public String describeDetailed() {
        return describeDetailedForViewer(null);
    }

    public String describeDetailedForViewer(String viewerId) {
        StringBuilder description = new StringBuilder(summary);
        if (directSource != null) {
            description.append("\nSource: ").append(viewerAwareName(directSource.getId(), directSource.getDisplayName(), viewerId));
            if (directSource.isRealFriend()) {
                description.append(" (real friend, ")
                        .append(directSource.getDegreeOfSeparation())
                        .append(directSource.getDegreeOfSeparation() == 1 ? " degree" : " degrees")
                        .append(" away)");
            } else {
                description.append(" (simulated friend, excluded from degree tracking)");
            }
        }
        if (!patientZeros.isEmpty()) {
            description.append("\nKnown patient zeroes: ");
            for (int index = 0; index < patientZeros.size(); index++) {
                PatientZero patientZero = patientZeros.get(index);
                if (index > 0) {
                    description.append(", ");
                }
                description.append(viewerAwareName(patientZero.getId(), patientZero.getDisplayName(), viewerId))
                        .append(" (")
                        .append(patientZero.getDegreeOfSeparation())
                        .append(patientZero.getDegreeOfSeparation() == 1 ? " degree" : " degrees")
                        .append(" away)");
            }
        }
        return description.toString();
    }

    @SuppressLint("NewApi")
    public String toSharePayload() {
        StringBuilder raw = new StringBuilder();
        raw.append(SHARE_VERSION).append('\n');
        raw.append(type.name()).append('\n');
        raw.append(summary).append('\n');
        if (directSource == null) {
            raw.append("\n\n0\n0\n");
        } else {
            raw.append(directSource.getId()).append('\n');
            raw.append(directSource.getDisplayName()).append('\n');
            raw.append(directSource.isRealFriend() ? '1' : '0').append('\n');
            raw.append(directSource.getDegreeOfSeparation()).append('\n');
        }
        raw.append(patientZeros.size()).append('\n');
        for (PatientZero patientZero : patientZeros) {
            raw.append(patientZero.getId())
                    .append('\t')
                    .append(patientZero.getDisplayName())
                    .append('\t')
                    .append(patientZero.getDegreeOfSeparation())
                    .append('\n');
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.toString().getBytes(StandardCharsets.UTF_8));
    }

    @SuppressLint("NewApi")
    public static VirusOrigin fromSharePayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            String[] lines = decoded.split("\\n", -1);
            if (lines.length < 8 || !SHARE_VERSION.equals(lines[0])) {
                return null;
            }

            Type type = Type.valueOf(lines[1]);
            String summary = lines[2];
            Source source = null;
            if (!lines[3].isEmpty() && !lines[4].isEmpty()) {
                source = "1".equals(lines[5])
                        ? Source.real(lines[3], lines[4], parsePositiveInt(lines[6]))
                        : Source.fake(lines[3], lines[4]);
            }

            int countIndex = 7;
            int patientZeroCount = parsePositiveInt(lines[countIndex]);
            List<PatientZero> patientZeros = new ArrayList<>();
            for (int index = 0; index < patientZeroCount; index++) {
                int lineIndex = countIndex + 1 + index;
                if (lineIndex >= lines.length) {
                    break;
                }
                String line = lines[lineIndex];
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\t", -1);
                if (parts.length < 3) {
                    continue;
                }
                patientZeros.add(new PatientZero(parts[0], parts[1], parsePositiveInt(parts[2])));
            }
            return new VirusOrigin(type, summary, source, patientZeros);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private List<PatientZero> exportLineage() {
        if (!patientZeros.isEmpty()) {
            return patientZeros;
        }
        if (directSource != null && directSource.isRealFriend()) {
            return Collections.singletonList(PatientZero.fromSource(directSource));
        }
        return Collections.emptyList();
    }

    private static List<PatientZero> advanceLineage(List<PatientZero> lineage, Source directSource) {
        List<PatientZero> advanced = new ArrayList<>();
        for (PatientZero patientZero : lineage) {
            if (directSource != null && directSource.isRealFriend()
                    && directSource.getId().equals(patientZero.getId())) {
                advanced.add(patientZero.withDegree(1));
            } else {
                advanced.add(patientZero.withDegree(patientZero.getDegreeOfSeparation() + 1));
            }
        }
        if (advanced.isEmpty() && directSource != null && directSource.isRealFriend()) {
            advanced.add(PatientZero.fromSource(directSource));
        }
        return selectTopPatientZeros(advanced);
    }

    private static List<PatientZero> selectTopPatientZeros(List<PatientZero> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        List<PatientZero> deduped = new ArrayList<>();
        for (PatientZero candidate : input) {
            if (candidate == null) {
                continue;
            }
            boolean replaced = false;
            for (int index = 0; index < deduped.size(); index++) {
                PatientZero existing = deduped.get(index);
                if (existing.getId().equals(candidate.getId())) {
                    if (candidate.getDegreeOfSeparation() < existing.getDegreeOfSeparation()) {
                        deduped.set(index, candidate);
                    } else if (candidate.getDegreeOfSeparation() == existing.getDegreeOfSeparation()
                            && !candidate.getDisplayName().trim().isEmpty()) {
                        deduped.set(index, candidate);
                    }
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                deduped.add(candidate);
            }
        }

        if (deduped.size() <= MAX_PATIENT_ZEROS) {
            return new ArrayList<>(deduped);
        }
        return new ArrayList<>(deduped.subList(0, MAX_PATIENT_ZEROS));
    }

    private static PatientZero primaryPatientZeroOf(VirusOrigin origin) {
        if (origin == null) {
            return null;
        }
        if (!origin.patientZeros.isEmpty()) {
            return origin.patientZeros.get(0);
        }
        if (origin.directSource != null && origin.directSource.isRealFriend()) {
            return PatientZero.fromSource(origin.directSource);
        }
        return null;
    }

    private static PatientZero resolvePrimaryPatientZero(VirusOrigin base, String carrier) {
        PatientZero fromOrigin = primaryPatientZeroOf(base);
        if (fromOrigin != null) {
            return fromOrigin;
        }
        if (isLikelyRealFriendCarrier(carrier)) {
            return new PatientZero(stableId("carrier:" + carrier), carrier, 1);
        }
        return null;
    }

    private static PatientZero resolveSecondaryPatientZero(VirusOrigin base, PatientZero primary) {
        if (base == null || base.patientZeros.isEmpty()) {
            return null;
        }
        for (PatientZero candidate : base.patientZeros) {
            if (candidate == null) {
                continue;
            }
            if (primary != null && primary.getId().equals(candidate.getId())) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static Source choosePreferredDirectSource(Source current, Source candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        if (candidate.isRealFriend() != current.isRealFriend()) {
            return candidate.isRealFriend() ? candidate : current;
        }
        if (candidate.getDegreeOfSeparation() != current.getDegreeOfSeparation()) {
            return candidate.getDegreeOfSeparation() < current.getDegreeOfSeparation() ? candidate : current;
        }
        return candidate.getDisplayName().compareTo(current.getDisplayName()) < 0 ? candidate : current;
    }

    private static boolean isLikelyRealFriendCarrier(String carrier) {
        if (carrier == null || carrier.trim().isEmpty()) {
            return false;
        }
        String normalized = carrier.trim();
        return !LAB_CARRIER.equalsIgnoreCase(normalized)
                && !normalized.contains(" x ")
                && !normalized.contains(SIMULATED_FLAG);
    }

    private static int parsePositiveInt(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static String stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String viewerAwareName(String sourceId, String displayName, String viewerId) {
        if (viewerId != null && !viewerId.trim().isEmpty() && viewerId.equals(sourceId)) {
            return "you";
        }
        return displayName;
    }

    private enum Type {
        LEGACY,
        LAB,
        RANDOM_FRIEND,
        IMPORTED_INVITE,
        COLLAPSED,
        LOCAL_COMBINE,
        FRIEND_INFECTION,
        WILD_QR,
        WILD_BARCODE
    }

    public static final class Source implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String id;
        private final String displayName;
        private final boolean realFriend;
        private final int degreeOfSeparation;

        private Source(String id, String displayName, boolean realFriend, int degreeOfSeparation) {
            this.id = id == null ? "" : id;
            this.displayName = displayName == null ? "Unknown" : displayName;
            this.realFriend = realFriend;
            this.degreeOfSeparation = Math.max(0, degreeOfSeparation);
        }

        public static Source real(String id, String displayName, int degreeOfSeparation) {
            return new Source(id, displayName, true, degreeOfSeparation);
        }

        public static Source fake(String id, String displayName) {
            return new Source(id, displayName, false, 0);
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isRealFriend() {
            return realFriend;
        }

        public int getDegreeOfSeparation() {
            return degreeOfSeparation;
        }

        private Source withDegree(int degree) {
            return new Source(id, displayName, realFriend, degree);
        }
    }

    public static final class PatientZero implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String id;
        private final String displayName;
        private final int degreeOfSeparation;

        private PatientZero(String id, String displayName, int degreeOfSeparation) {
            this.id = id == null ? "" : id;
            this.displayName = displayName == null ? "Unknown" : displayName;
            this.degreeOfSeparation = Math.max(1, degreeOfSeparation);
        }

        public static PatientZero fromSource(Source source) {
            return new PatientZero(source.getId(), source.getDisplayName(), source.getDegreeOfSeparation());
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getDegreeOfSeparation() {
            return degreeOfSeparation;
        }

        private PatientZero withDegree(int degree) {
            return new PatientZero(id, displayName, degree);
        }
    }
}

