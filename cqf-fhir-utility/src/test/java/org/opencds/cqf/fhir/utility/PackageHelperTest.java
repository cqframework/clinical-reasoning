package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.r4.model.IntegerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PackageHelperTest {

    @Test
    void packageParametersWithAllFields() {
        var actual = packageParameters(
                FhirVersionEnum.R4, new IntegerType(1), new IntegerType(1), "searchset", null, null, false);
        assertInstanceOf(org.hl7.fhir.r4.model.Parameters.class, actual);
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("count"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("offset"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("bundleType"));
        assertFalse(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("include"));
        assertFalse(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("terminologyEndpoint"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("isPut"));
    }

    @Test
    void r4PackageParametersIncludeOnly() {
        var actual = packageParameters(
                FhirVersionEnum.R4, null, null, null, List.of("PlanDefinition", "ValueSet"), null, false);
        assertInstanceOf(org.hl7.fhir.r4.model.Parameters.class, actual);
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("include"));
        assertEquals(
                2,
                ((org.hl7.fhir.r4.model.Parameters) actual)
                        .getParameters("include")
                        .size());
        assertFalse(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("terminologyEndpoint"));
        assertTrue(((org.hl7.fhir.r4.model.Parameters) actual).hasParameter("isPut"));
    }

    @Test
    void r5PackageParametersIncludeOnly() {
        var actual = packageParameters(
                FhirVersionEnum.R5, null, null, null, List.of("PlanDefinition", "ValueSet"), null, false);
        assertInstanceOf(org.hl7.fhir.r5.model.Parameters.class, actual);
        assertTrue(((org.hl7.fhir.r5.model.Parameters) actual).hasParameter("include"));
        assertEquals(
                2,
                ((org.hl7.fhir.r5.model.Parameters) actual)
                        .getParameters("include")
                        .size());
        assertFalse(((org.hl7.fhir.r5.model.Parameters) actual).hasParameter("terminologyEndpoint"));
        assertTrue(((org.hl7.fhir.r5.model.Parameters) actual).hasParameter("isPut"));
    }

    @Test
    void createEntryWithPutR4() {
        var vs = new org.hl7.fhir.r4.model.ValueSet();
        vs.setId("test-vs");
        vs.setUrl("http://example.org/ValueSet/test");
        vs.setVersion("1.0");
        var entry = PackageHelper.createEntry(vs, true);
        var r4Entry = (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry;
        assertEquals("PUT", r4Entry.getRequest().getMethod().toCode());
        assertTrue(r4Entry.getRequest().getUrl().contains("test-vs"));
        assertEquals("http://example.org/ValueSet/test", r4Entry.getFullUrl());
    }

    @Test
    void createEntryWithPostR4() {
        var vs = new org.hl7.fhir.r4.model.ValueSet();
        vs.setId("test-vs");
        vs.setUrl("http://example.org/ValueSet/test");
        vs.setVersion("1.0");
        var entry = PackageHelper.createEntry(vs, false);
        var r4Entry = (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry;
        assertEquals("POST", r4Entry.getRequest().getMethod().toCode());
        assertTrue(r4Entry.getRequest().getIfNoneExist().contains("url="));
        assertTrue(r4Entry.getRequest().getIfNoneExist().contains("version=1.0"));
    }

    @Test
    void createEntryPostWithoutVersion() {
        var vs = new org.hl7.fhir.r4.model.ValueSet();
        vs.setUrl("http://example.org/ValueSet/test");
        var entry = PackageHelper.createEntry(vs, false);
        var r4Entry = (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry;
        assertTrue(r4Entry.getRequest().getIfNoneExist().startsWith("url="));
        assertFalse(r4Entry.getRequest().getIfNoneExist().contains("version="));
    }

    @Test
    void createEntryNonMetadataResource() {
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setId("p1");
        var entry = PackageHelper.createEntry(patient, true);
        var r4Entry = (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry;
        assertEquals("PUT", r4Entry.getRequest().getMethod().toCode());
        assertEquals("Patient/p1", r4Entry.getRequest().getUrl());
    }

    @Test
    void deleteEntry() {
        var vs = new org.hl7.fhir.r4.model.ValueSet();
        vs.setId("test-vs");
        var entry = PackageHelper.deleteEntry(vs);
        var r4Entry = (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entry;
        assertEquals("DELETE", r4Entry.getRequest().getMethod().toCode());
        assertEquals("ValueSet/test-vs", r4Entry.getRequest().getUrl());
    }

    @Test
    void createEntryR5() {
        var vs = new org.hl7.fhir.r5.model.ValueSet();
        vs.setId("r5-vs");
        vs.setUrl("http://example.org/ValueSet/r5");
        var entry = PackageHelper.createEntry(vs, true);
        assertInstanceOf(org.hl7.fhir.r5.model.Bundle.BundleEntryComponent.class, entry);
    }

    @Test
    void createEntryDstu3() {
        var vs = new org.hl7.fhir.dstu3.model.ValueSet();
        vs.setId("dstu3-vs");
        vs.setUrl("http://example.org/ValueSet/dstu3");
        var entry = PackageHelper.createEntry(vs, false);
        assertInstanceOf(org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent.class, entry);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"})
    void packageParametersMinimal(FhirVersionEnum version) {
        var params = PackageHelper.packageParameters(version, null, false);
        assertInstanceOf(org.hl7.fhir.instance.model.api.IBaseParameters.class, params);
    }
}
