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
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.IdType;
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
            newEntry(fhirVersion);
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
        var entry = newEntryWithResource(resource);
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
        var entry = newEntryWithResource(resource);
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
        var entry = newEntryWithResource(resource);
        assertTrue(entry instanceof org.hl7.fhir.r5.model.Bundle.BundleEntryComponent);
        addEntry(bundle, entry);
        assertFalse(getEntry(bundle).isEmpty());
        assertEquals(entry, getEntryFirstRep(bundle));
        assertEquals(resource, getEntryResourceFirstRep(bundle));
        assertEquals(resource, getEntryResource(fhirVersion, entry));
        assertFalse(getEntryResources(bundle).isEmpty());
    }

    @Test
    void isEntryRequestDeleteDstu3() {
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent bundle =
                new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
                        .setRequest(new org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent()
                                .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.DELETE));
        var res = BundleHelper.isEntryRequestDelete(FhirVersionEnum.DSTU3, bundle);

        assertTrue(res);
    }

    @Test
    void isEntryRequestDeleteR4() {
        BundleEntryComponent bundle = new Bundle.BundleEntryComponent()
                .setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.DELETE));
        var res = BundleHelper.isEntryRequestDelete(FhirVersionEnum.R4, bundle);

        assertTrue(res);
    }

    @Test
    void isEntryRequestDeleteR5() {
        org.hl7.fhir.r5.model.Bundle.BundleEntryComponent bundle =
                new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                        .setRequest(new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                                .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.DELETE));
        var res = BundleHelper.isEntryRequestDelete(FhirVersionEnum.R5, bundle);

        assertTrue(res);
    }

    @Test
    void isEntryRequestDeleteUnsupported() {
        try {
            org.hl7.fhir.r5.model.Bundle.BundleEntryComponent bundle =
                    new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                            .setRequest(new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                                    .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.DELETE));

            BundleHelper.isEntryRequestDelete(FhirVersionEnum.DSTU2, bundle);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unsupported version of FHIR"));
        }
    }

    @Test
    void getEntryRequestIdDstu3() {
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent bundle =
                new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
                        .setRequest(new org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent()
                                .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.GET));

        bundle.getRequest().setUrl("Library/123");

        var res = BundleHelper.getEntryRequestId(FhirVersionEnum.DSTU3, bundle);

        assertEquals(new org.hl7.fhir.dstu3.model.IdType("123"), res.get());
    }

    @Test
    void getEntryRequestIdR4() {
        BundleEntryComponent bundle =
                new Bundle.BundleEntryComponent().setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.GET));

        bundle.getRequest().setUrl("Library/123");

        var res = BundleHelper.getEntryRequestId(FhirVersionEnum.R4, bundle);

        assertEquals(new IdType("123"), res.get());
    }

    @Test
    void getEntryRequestIdR5() {
        org.hl7.fhir.r5.model.Bundle.BundleEntryComponent bundle =
                new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                        .setRequest(new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                                .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.GET));

        bundle.getRequest().setUrl("Library/123");

        var res = BundleHelper.getEntryRequestId(FhirVersionEnum.R5, bundle);

        assertEquals(new org.hl7.fhir.r5.model.IdType("123"), res.get());
    }

    @Test
    void getEntryRequestIdUnsupported() {
        try {
            org.hl7.fhir.r5.model.Bundle.BundleEntryComponent bundle =
                    new org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                            .setRequest(new org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                                    .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.DELETE));

            BundleHelper.getEntryRequestId(FhirVersionEnum.DSTU2, bundle);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unsupported version of FHIR"));
        }
    }

    @Test
    void testgetBundleEntryResourceIds_whenBundleHasNoEntries_shouldReturnEmptyList(){
        Bundle bundle = new Bundle();
        List<IIdType> bundleEntryResourceIds = BundleHelper.getBundleEntryResourceIds(
            FhirVersionEnum.R4, bundle);

        assertTrue(bundleEntryResourceIds.isEmpty());
    }
}
