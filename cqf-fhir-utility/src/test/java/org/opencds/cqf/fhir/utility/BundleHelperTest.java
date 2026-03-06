package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BundleHelperTest {

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newBundleDefaultType(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        assertNotNull(bundle);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newBundleWithType(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version, "transaction");
        assertNotNull(bundle);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newBundleWithIdAndType(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version, "test-id", "searchset");
        assertNotNull(bundle);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newBundleWithNullId(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version, null, null);
        assertNotNull(bundle);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newBundleWithEmptyId(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version, "", "");
        assertNotNull(bundle);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntryFirstRep(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var entry = BundleHelper.getEntryFirstRep(bundle);
        assertNotNull(entry);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntryResourceFirstRepEmptyBundle(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var resource = BundleHelper.getEntryResourceFirstRep(bundle);
        assertNull(resource);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntryResourceFirstRepWithResource(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var patient = createPatient(version);
        var entry = BundleHelper.newEntryWithResource(patient);
        BundleHelper.addEntry(bundle, entry);
        var resource = BundleHelper.getEntryResourceFirstRep(bundle);
        assertNotNull(resource);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntryResources(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var patient = createPatient(version);
        BundleHelper.addEntry(bundle, BundleHelper.newEntryWithResource(patient));
        var resources = BundleHelper.getEntryResources(bundle);
        assertEquals(1, resources.size());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntry(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var entries = BundleHelper.getEntry(bundle);
        assertNotNull(entries);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setEntry(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var patient = createPatient(version);
        var entry = BundleHelper.newEntryWithResource(patient);
        BundleHelper.setEntry(bundle, List.of(entry));
        assertEquals(1, BundleHelper.getEntry(bundle).size());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntryResource(FhirVersionEnum version) {
        var patient = createPatient(version);
        var entry = BundleHelper.newEntryWithResource(patient);
        var resource = BundleHelper.getEntryResource(version, entry);
        assertNotNull(resource);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setBundleType(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var result = BundleHelper.setBundleType(bundle, "transaction");
        assertNotNull(result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setBundleTotal(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var result = BundleHelper.setBundleTotal(bundle, 42);
        assertNotNull(result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newEntry(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        assertNotNull(entry);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newEntryWithResponse(FhirVersionEnum version) {
        var response = BundleHelper.newResponseWithLocation(version, "Patient/123");
        var entry = BundleHelper.newEntryWithResponse(version, response);
        assertNotNull(entry);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newResponseWithLocation(FhirVersionEnum version) {
        var response = BundleHelper.newResponseWithLocation(version, "Patient/123");
        assertNotNull(response);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newRequestWithMethodAndUrl(FhirVersionEnum version) {
        var request = BundleHelper.newRequest(version, "PUT", "Patient/123");
        assertNotNull(request);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void newRequestWithMethodOnly(FhirVersionEnum version) {
        var request = BundleHelper.newRequest(version, "POST");
        assertNotNull(request);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setRequestUrl(FhirVersionEnum version) {
        var request = BundleHelper.newRequest(version, "PUT");
        var result = BundleHelper.setRequestUrl(version, request, "Patient/456");
        assertNotNull(result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setRequestIfNoneExist(FhirVersionEnum version) {
        var request = BundleHelper.newRequest(version, "POST");
        var result = BundleHelper.setRequestIfNoneExist(version, request, "identifier=123");
        assertNotNull(result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setEntryFullUrl(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var result = BundleHelper.setEntryFullUrl(version, entry, "urn:uuid:1234");
        assertNotNull(result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setEntryRequest(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var request = BundleHelper.newRequest(version, "PUT", "Patient/123");
        var result = BundleHelper.setEntryRequest(version, entry, request);
        assertNotNull(result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void setEntryRequestNull(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var result = BundleHelper.setEntryRequest(version, entry, null);
        assertNotNull(result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void isEntryRequestPut(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var request = BundleHelper.newRequest(version, "PUT", "Patient/123");
        BundleHelper.setEntryRequest(version, entry, request);
        assertTrue(BundleHelper.isEntryRequestPut(version, entry));
        assertFalse(BundleHelper.isEntryRequestPost(version, entry));
        assertFalse(BundleHelper.isEntryRequestDelete(version, entry));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void isEntryRequestPost(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var request = BundleHelper.newRequest(version, "POST", "Patient");
        BundleHelper.setEntryRequest(version, entry, request);
        assertTrue(BundleHelper.isEntryRequestPost(version, entry));
        assertFalse(BundleHelper.isEntryRequestPut(version, entry));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void isEntryRequestDelete(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var request = BundleHelper.newRequest(version, "DELETE", "Patient/123");
        BundleHelper.setEntryRequest(version, entry, request);
        assertTrue(BundleHelper.isEntryRequestDelete(version, entry));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void isEntryRequestNoRequest(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        assertFalse(BundleHelper.isEntryRequestPut(version, entry));
        assertFalse(BundleHelper.isEntryRequestPost(version, entry));
        assertFalse(BundleHelper.isEntryRequestDelete(version, entry));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntryRequestUrl(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var request = BundleHelper.newRequest(version, "PUT", "Patient/123");
        BundleHelper.setEntryRequest(version, entry, request);
        assertEquals("Patient/123", BundleHelper.getEntryRequestUrl(version, entry));
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getEntryRequestId(FhirVersionEnum version) {
        var entry = BundleHelper.newEntry(version);
        var request = BundleHelper.newRequest(version, "PUT", "Patient/123");
        BundleHelper.setEntryRequest(version, entry, request);
        var id = BundleHelper.getEntryRequestId(version, entry);
        assertTrue(id.isPresent());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void getBundleEntryResourceIds(FhirVersionEnum version) {
        var bundle = BundleHelper.newBundle(version);
        var ids = BundleHelper.getBundleEntryResourceIds(version, bundle);
        assertTrue(ids.isEmpty());
    }

    @Test
    void getBundleEntryResourceIdsWithContext() {
        var ctx = FhirContext.forR4Cached();
        var bundle = BundleHelper.newBundle(FhirVersionEnum.R4);
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setId("test-123");
        BundleHelper.addEntry(bundle, BundleHelper.newEntryWithResource(patient));
        var ids = BundleHelper.getBundleEntryResourceIds(ctx, bundle);
        assertEquals(1, ids.size());
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void resourceToRuntimeSearchParam(FhirVersionEnum version) {
        var sp = createSearchParameter(version);
        var result = BundleHelper.resourceToRuntimeSearchParam(sp);
        assertNotNull(result);
        assertEquals("test-code", result.getName());
    }

    @Test
    void unsupportedVersionThrows() {
        assertThrows(IllegalArgumentException.class, () -> BundleHelper.newBundle(FhirVersionEnum.DSTU2));
    }

    private org.hl7.fhir.instance.model.api.IBaseResource createPatient(FhirVersionEnum version) {
        return switch (version) {
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Patient().setId("test");
            case R4 -> new org.hl7.fhir.r4.model.Patient().setId("test");
            case R5 -> new org.hl7.fhir.r5.model.Patient().setId("test");
            default -> throw new IllegalArgumentException("Unsupported");
        };
    }

    private org.hl7.fhir.instance.model.api.IBaseResource createSearchParameter(FhirVersionEnum version) {
        return switch (version) {
            case DSTU3 -> {
                var sp = new org.hl7.fhir.dstu3.model.SearchParameter();
                sp.setId("test-sp");
                sp.setUrl("http://example.org/sp");
                sp.setCode("test-code");
                sp.setDescription("Test");
                sp.setExpression("Patient.name");
                yield sp;
            }
            case R4 -> {
                var sp = new org.hl7.fhir.r4.model.SearchParameter();
                sp.setId("test-sp");
                sp.setUrl("http://example.org/sp");
                sp.setCode("test-code");
                sp.setDescription("Test");
                sp.setExpression("Patient.name");
                yield sp;
            }
            case R5 -> {
                var sp = new org.hl7.fhir.r5.model.SearchParameter();
                sp.setId("test-sp");
                sp.setUrl("http://example.org/sp");
                sp.setCode("test-code");
                sp.setDescription("Test");
                sp.setExpression("Patient.name");
                yield sp;
            }
            default -> throw new IllegalArgumentException("Unsupported");
        };
    }
}
