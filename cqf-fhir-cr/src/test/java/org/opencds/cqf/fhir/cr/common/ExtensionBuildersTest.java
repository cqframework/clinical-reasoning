package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.AbstractMap.SimpleEntry;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;

class ExtensionBuildersTest {

    @Test
    void buildDependencyRoleExt_r4_setsUrlAndCodeValue() {
        Extension ext = ExtensionBuilders.buildDependencyRoleExt(FhirVersionEnum.R4, "default");

        assertNotNull(ext);
        assertEquals(Constants.CRMI_DEPENDENCY_ROLE, ext.getUrl());
        assertEquals("default", ((CodeType) ext.getValue()).getValue());
    }

    @Test
    void buildPackageSourceExt_r4_setsUrlAndStringValue() {
        Extension ext = ExtensionBuilders.buildPackageSourceExt(FhirVersionEnum.R4, "my.package#1.0.0");

        assertNotNull(ext);
        assertEquals(Constants.PACKAGE_SOURCE, ext.getUrl());
        assertEquals("my.package#1.0.0", ((StringType) ext.getValue()).getValue());
    }

    @Test
    void buildComplexPackageSourceExt_r4_allSubExtensions() {
        Extension ext =
                ExtensionBuilders.buildComplexPackageSourceExt(FhirVersionEnum.R4, "my.pkg", "2.0.0", "http://ex.org");

        assertNotNull(ext);
        assertEquals(Constants.PACKAGE_SOURCE, ext.getUrl());

        var packageIdExt = ext.getExtensionByUrl("packageId");
        assertNotNull(packageIdExt);
        assertEquals("my.pkg", ((StringType) packageIdExt.getValue()).getValue());

        var versionExt = ext.getExtensionByUrl("version");
        assertNotNull(versionExt);
        assertEquals("2.0.0", ((StringType) versionExt.getValue()).getValue());

        var uriExt = ext.getExtensionByUrl("uri");
        assertNotNull(uriExt);
        assertEquals("http://ex.org", ((UriType) uriExt.getValue()).getValue());
    }

    @Test
    void buildComplexPackageSourceExt_r4_nullSubValuesOmitted() {
        Extension ext = ExtensionBuilders.buildComplexPackageSourceExt(FhirVersionEnum.R4, "my.pkg", null, null);

        assertNotNull(ext);
        assertEquals(Constants.PACKAGE_SOURCE, ext.getUrl());

        assertNotNull(ext.getExtensionByUrl("packageId"));
        assertNull(ext.getExtensionByUrl("version"));
        assertNull(ext.getExtensionByUrl("uri"));
    }

    @Test
    void buildReferenceSourceExt_r4_setsArtifactAndPath() {
        Extension ext = ExtensionBuilders.buildReferenceSourceExt(
                FhirVersionEnum.R4, "http://example.org/Library/lib1", "Library.relatedArtifact");

        assertNotNull(ext);
        assertEquals(Constants.CRMI_REFERENCE_SOURCE, ext.getUrl());

        var artifactExt = ext.getExtensionByUrl("artifact");
        assertNotNull(artifactExt);
        assertTrue(artifactExt.getValue() instanceof CanonicalType);
        assertEquals("http://example.org/Library/lib1", ((CanonicalType) artifactExt.getValue()).getValue());

        var pathExt = ext.getExtensionByUrl("path");
        assertNotNull(pathExt);
        assertEquals("Library.relatedArtifact", ((StringType) pathExt.getValue()).getValue());
    }

    @Test
    void buildReferenceExt_r4_containedPrefixesHash() {
        var entry = new SimpleEntry<>("http://example.org/ext", "some-id");
        Extension ext = ExtensionBuilders.buildReferenceExt(FhirVersionEnum.R4, entry, true);

        assertNotNull(ext);
        assertEquals("http://example.org/ext", ext.getUrl());
        assertEquals("#some-id", ((Reference) ext.getValue()).getReference());
    }

    @Test
    void buildReferenceExt_r4_notContainedNoHash() {
        var entry = new SimpleEntry<>("http://example.org/ext", "some-id");
        Extension ext = ExtensionBuilders.buildReferenceExt(FhirVersionEnum.R4, entry, false);

        assertNotNull(ext);
        assertEquals("some-id", ((Reference) ext.getValue()).getReference());
    }

    @Test
    void buildBooleanExt_r4_setsValue() {
        var entry = new SimpleEntry<>("http://example.org/boolext", true);
        Extension ext = ExtensionBuilders.buildBooleanExt(FhirVersionEnum.R4, entry);

        assertNotNull(ext);
        assertEquals("http://example.org/boolext", ext.getUrl());
        assertTrue(((BooleanType) ext.getValue()).booleanValue());
    }

    @Test
    void buildDependencyRoleExt_unsupportedVersion_returnsNull() {
        Extension ext = ExtensionBuilders.buildDependencyRoleExt(FhirVersionEnum.DSTU2, "default");

        assertNull(ext);
    }
}
