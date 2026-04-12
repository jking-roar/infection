package com.kingjoshdavid.funfection.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VirusOriginTest {

    @Test
    public void importedFromInvitePrefersCreatorPrimaryAndRightSideSecondary() {
        VirusOrigin combined = VirusOrigin.infectedFrom(
                "Left",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Creator"),
                "Right",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "RightZero"));

        VirusOrigin imported = VirusOrigin.importedFromInvite(combined, "Carrier");

        assertEquals(2, imported.getPatientZeros().size());
        assertEquals("Creator", imported.getPatientZeros().get(0).getDisplayName());
        assertEquals("RightZero", imported.getPatientZeros().get(1).getDisplayName());
    }

    @Test
    public void infectedFromUsesLeftThenRightPrimaryPatientZeroOrder() {
        VirusOrigin result = VirusOrigin.infectedFrom(
                "Left",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Primary"),
                "Right",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Secondary"));

        assertEquals(2, result.getPatientZeros().size());
        assertEquals("Primary", result.getPatientZeros().get(0).getDisplayName());
        assertEquals("Secondary", result.getPatientZeros().get(1).getDisplayName());
        assertNotNull(result.getDirectSource());
        assertEquals("Secondary", result.getDirectSource().getDisplayName());
    }

    @Test
    public void sharePayloadRoundTripsIdsAndHandlesForTwoPatientZeros() {
        VirusOrigin origin = VirusOrigin.infectedFrom(
                "Left",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Alpha"),
                "Right",
                VirusOrigin.importedFromInvite(VirusOrigin.seededInLab(), "Beta"));

        VirusOrigin decoded = VirusOrigin.fromSharePayload(origin.toSharePayload());

        assertNotNull(decoded);
        assertEquals(2, decoded.getPatientZeros().size());
        assertEquals(origin.getPatientZeros().get(0).getId(), decoded.getPatientZeros().get(0).getId());
        assertEquals("Alpha", decoded.getPatientZeros().get(0).getDisplayName());
        assertEquals(origin.getPatientZeros().get(1).getId(), decoded.getPatientZeros().get(1).getId());
        assertEquals("Beta", decoded.getPatientZeros().get(1).getDisplayName());
    }
}

