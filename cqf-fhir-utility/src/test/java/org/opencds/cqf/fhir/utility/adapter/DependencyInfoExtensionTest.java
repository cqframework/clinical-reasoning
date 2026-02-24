package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;

class DependencyInfoExtensionTest {

    @Test
    void testBuildDependencyExtensions_R4_WithRolesOnly() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addRole("key");
        dependency.addRole("default");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R4, "http://example.org/Measure/test");

        assertEquals(2, extensions.size(), "Should have 2 role extensions");
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_DEPENDENCY_ROLE));
    }

    @Test
    void testBuildDependencyExtensions_R4_WithPackageSource() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addRole("default");
        dependency.setReferencePackageId("hl7.fhir.us.core#6.1.0");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R4, "http://example.org/Measure/test");

        assertEquals(2, extensions.size(), "Should have 1 role + 1 package-source extension");
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_DEPENDENCY_ROLE));
        assertTrue(hasExtensionWithUrl(extensions, Constants.PACKAGE_SOURCE));
    }

    @Test
    void testBuildDependencyExtensions_R4_WithFhirPaths() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addRole("key");
        dependency.addFhirPath("library[0]");
        dependency.addFhirPath("population[0].criteria");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R4, "http://example.org/Measure/test");

        assertEquals(3, extensions.size(), "Should have 1 role + 2 reference-source extensions");
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_DEPENDENCY_ROLE));
        assertEquals(2, countExtensionsWithUrl(extensions, Constants.CRMI_REFERENCE_SOURCE));
    }

    @Test
    void testBuildDependencyExtensions_R4_AllExtensions() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addRole("key");
        dependency.addRole("default");
        dependency.setReferencePackageId("hl7.fhir.us.core#6.1.0");
        dependency.addFhirPath("library[0]");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R4, "http://example.org/Measure/test");

        assertEquals(4, extensions.size(), "Should have 2 roles + 1 package + 1 reference-source");
        assertEquals(2, countExtensionsWithUrl(extensions, Constants.CRMI_DEPENDENCY_ROLE));
        assertTrue(hasExtensionWithUrl(extensions, Constants.PACKAGE_SOURCE));
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_REFERENCE_SOURCE));
    }

    @Test
    void testBuildDependencyExtensions_DSTU3() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addRole("default");
        dependency.setReferencePackageId("hl7.fhir.us.core#3.1.0");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.DSTU3, "http://example.org/Measure/test");

        assertEquals(2, extensions.size(), "Should work with DSTU3");
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_DEPENDENCY_ROLE));
        assertTrue(hasExtensionWithUrl(extensions, Constants.PACKAGE_SOURCE));
    }

    @Test
    void testBuildDependencyExtensions_R5() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addRole("key");
        dependency.addFhirPath("library[0]");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R5, "http://example.org/Measure/test");

        assertEquals(2, extensions.size(), "Should work with R5");
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_DEPENDENCY_ROLE));
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_REFERENCE_SOURCE));
    }

    @Test
    void testBuildDependencyExtensions_EmptyDependency() {
        var dependency = createDependency("http://example.org/Library/helper");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R4, "http://example.org/Measure/test");

        assertEquals(0, extensions.size(), "Should have no extensions when dependency has no metadata");
    }

    @Test
    void testBuildDependencyExtensions_NoSourceArtifactUrl() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addRole("default");
        dependency.addFhirPath("library[0]");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R4, null);

        assertEquals(1, extensions.size(), "Should only have role extension, no reference-source without source URL");
        assertTrue(hasExtensionWithUrl(extensions, Constants.CRMI_DEPENDENCY_ROLE));
    }

    @Test
    void testReferenceSourceExtension_HasSubExtensions() {
        var dependency = createDependency("http://example.org/Library/helper");
        dependency.addFhirPath("library[0]");

        var extensions = dependency.buildDependencyExtensions(FhirVersionEnum.R4, "http://example.org/Measure/test");

        assertEquals(1, extensions.size());
        var ext = extensions.get(0);
        assertEquals(Constants.CRMI_REFERENCE_SOURCE, ext.getUrl());

        // Check that it has artifact and path sub-extensions
        var r4Ext = (org.hl7.fhir.r4.model.Extension) ext;
        assertEquals(2, r4Ext.getExtension().size(), "Should have 2 sub-extensions");
        assertTrue(
                r4Ext.getExtension().stream().anyMatch(e -> "artifact".equals(e.getUrl())),
                "Should have 'artifact' sub-extension");
        assertTrue(
                r4Ext.getExtension().stream().anyMatch(e -> "path".equals(e.getUrl())),
                "Should have 'path' sub-extension");
    }

    // Helper methods
    private DependencyInfo createDependency(String reference) {
        return new DependencyInfo("source", reference, new ArrayList<>(), ref -> {});
    }

    private boolean hasExtensionWithUrl(List<? extends IBaseExtension<?, ?>> extensions, String url) {
        return extensions.stream().anyMatch(ext -> url.equals(ext.getUrl()));
    }

    private long countExtensionsWithUrl(List<? extends IBaseExtension<?, ?>> extensions, String url) {
        return extensions.stream().filter(ext -> url.equals(ext.getUrl())).count();
    }
}
