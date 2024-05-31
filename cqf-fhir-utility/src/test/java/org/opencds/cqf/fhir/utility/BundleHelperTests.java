package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryFirstRep;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResource;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResourceFirstRep;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class BundleHelperTests {
    @Test
    void unsupportedVersionShouldThrow() {
        var fhirVersion = FhirVersionEnum.DSTU2;
        var entry = new org.hl7.fhir.dstu2.model.Bundle.BundleEntryComponent();
        var bundle = new org.hl7.fhir.dstu2.model.Bundle().addEntry(entry);
        assertThrows(IllegalArgumentException.class, () -> {
            getEntryFirstRep(bundle);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            getEntryResourceFirstRep(bundle);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            getEntryResources(bundle);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            getEntryResource(fhirVersion, entry);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            getEntry(bundle);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            addEntry(bundle, null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            newBundle(fhirVersion);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            newEntryWithResource(fhirVersion, null);
        });
    }

    @Test
    void dstu3() {
        var fhirVersion = FhirVersionEnum.DSTU3;
        var bundle = newBundle(fhirVersion);
        assertTrue(bundle instanceof org.hl7.fhir.dstu3.model.Bundle);
        assertTrue(getEntry(bundle).isEmpty());
        var resource = new org.hl7.fhir.dstu3.model.Patient()
                .setName(Collections.singletonList(new org.hl7.fhir.dstu3.model.HumanName()
                        .setFamily("Test")
                        .addGiven("Test")));
        var entry = newEntryWithResource(fhirVersion, resource);
        assertTrue(entry instanceof org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent);
        addEntry(bundle, entry);
        assertFalse(getEntry(bundle).isEmpty());
        assertEquals(entry, getEntryFirstRep(bundle));
        assertEquals(resource, getEntryResourceFirstRep(bundle));
        assertEquals(resource, getEntryResource(fhirVersion, entry));
        assertFalse(getEntryResources(bundle).isEmpty());
    }

    @Test
    void r4() {
        var fhirVersion = FhirVersionEnum.R4;
        var bundle = newBundle(fhirVersion);
        assertTrue(bundle instanceof org.hl7.fhir.r4.model.Bundle);
        assertTrue(getEntry(bundle).isEmpty());
        var resource = new org.hl7.fhir.r4.model.Patient()
                .setName(Collections.singletonList(
                        new org.hl7.fhir.r4.model.HumanName().setFamily("Test").addGiven("Test")));
        var entry = newEntryWithResource(fhirVersion, resource);
        assertTrue(entry instanceof org.hl7.fhir.r4.model.Bundle.BundleEntryComponent);
        addEntry(bundle, entry);
        assertFalse(getEntry(bundle).isEmpty());
        assertEquals(entry, getEntryFirstRep(bundle));
        assertEquals(resource, getEntryResourceFirstRep(bundle));
        assertEquals(resource, getEntryResource(fhirVersion, entry));
        assertFalse(getEntryResources(bundle).isEmpty());
    }

    @Test
    void r5() {
        var fhirVersion = FhirVersionEnum.R5;
        var bundle = newBundle(fhirVersion);
        assertTrue(bundle instanceof org.hl7.fhir.r5.model.Bundle);
        assertTrue(getEntry(bundle).isEmpty());
        var resource = new org.hl7.fhir.r5.model.Patient()
                .setName(Collections.singletonList(
                        new org.hl7.fhir.r5.model.HumanName().setFamily("Test").addGiven("Test")));
        var entry = newEntryWithResource(fhirVersion, resource);
        assertTrue(entry instanceof org.hl7.fhir.r5.model.Bundle.BundleEntryComponent);
        addEntry(bundle, entry);
        assertFalse(getEntry(bundle).isEmpty());
        assertEquals(entry, getEntryFirstRep(bundle));
        assertEquals(resource, getEntryResourceFirstRep(bundle));
        assertEquals(resource, getEntryResource(fhirVersion, entry));
        assertFalse(getEntryResources(bundle).isEmpty());
    }
}
