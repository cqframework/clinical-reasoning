package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * Tests verifying that version resolution uses IG dependsOn declarations
 * to resolve versions for CodeSystem, ValueSet, and other metadata resources.
 * <p>
 * These tests use reflection to access protected methods and verify the core
 * version resolution logic without requiring full end-to-end $data-requirements flows.
 */
class DataRequirementsIgDependencyResolutionTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final AdapterFactory adapterFactory = new AdapterFactory();

    /**
     * Helper method to invoke protected resolveCanonicalWithIgVersion via reflection
     */
    private String resolveCanonicalWithIgVersion(
            BaseKnowledgeArtifactVisitor visitor, String canonical, Map<String, String> igDependencyVersions) {
        try {
            // Method is in BaseKnowledgeArtifactVisitor, so get it from the superclass
            Method method = BaseKnowledgeArtifactVisitor.class.getDeclaredMethod(
                    "resolveCanonicalWithIgVersion", String.class, Map.class);
            method.setAccessible(true);
            return (String) method.invoke(visitor, canonical, igDependencyVersions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void resolveCanonicalWithIgVersion_ResolvesCodeSystemVersionFromDependsOn() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create a CodeSystem that belongs to the us.core package
        var codeSystem = new CodeSystem();
        codeSystem.setId("us-core-race");
        codeSystem.setUrl("http://hl7.org/fhir/us/core/CodeSystem/us-core-race");
        codeSystem.setVersion("6.1.0");
        codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
        codeSystem.addExtension(Constants.PACKAGE_SOURCE, new StringType("hl7.fhir.us.core#6.1.0"));
        repository.create(codeSystem);

        // Create IG dependency version map
        Map<String, String> igDependencyVersions = new HashMap<>();
        igDependencyVersions.put("hl7.fhir.us.core", "6.1.0");

        // Create visitor
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        // Test: resolve unversioned canonical
        String unversionedCanonical = "http://hl7.org/fhir/us/core/CodeSystem/us-core-race";
        String resolved = resolveCanonicalWithIgVersion(visitor, unversionedCanonical, igDependencyVersions);

        assertEquals(
                "http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0",
                resolved,
                "Should append version from IG dependsOn");
    }

    @Test
    void resolveCanonicalWithIgVersion_ResolvesValueSetVersionFromDependsOn() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create a ValueSet that belongs to the SDC package
        var valueSet = new ValueSet();
        valueSet.setId("sdc-question-type");
        valueSet.setUrl("http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type");
        valueSet.setVersion("3.0.0");
        valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
        valueSet.addExtension(Constants.PACKAGE_SOURCE, new StringType("hl7.fhir.uv.sdc#3.0.0"));
        repository.create(valueSet);

        // Create IG dependency version map
        Map<String, String> igDependencyVersions = new HashMap<>();
        igDependencyVersions.put("hl7.fhir.uv.sdc", "3.0.0");

        // Create visitor
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        // Test: resolve unversioned canonical
        String unversionedCanonical = "http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type";
        String resolved = resolveCanonicalWithIgVersion(visitor, unversionedCanonical, igDependencyVersions);

        assertEquals(
                "http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0",
                resolved,
                "Should append version from IG dependsOn");
    }

    @Test
    void resolveCanonicalWithIgVersion_ResolvesMultipleDependenciesFromDifferentPackages() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create resources from different packages
        var codeSystem = new CodeSystem();
        codeSystem.setId("us-core-race");
        codeSystem.setUrl("http://hl7.org/fhir/us/core/CodeSystem/us-core-race");
        codeSystem.setVersion("6.1.0");
        codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
        codeSystem.addExtension(Constants.PACKAGE_SOURCE, new StringType("hl7.fhir.us.core#6.1.0"));
        repository.create(codeSystem);

        var valueSet = new ValueSet();
        valueSet.setId("sdc-question-type");
        valueSet.setUrl("http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type");
        valueSet.setVersion("3.0.0");
        valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
        valueSet.addExtension(Constants.PACKAGE_SOURCE, new StringType("hl7.fhir.uv.sdc#3.0.0"));
        repository.create(valueSet);

        var thxCodeSystem = new CodeSystem();
        thxCodeSystem.setId("v3-ActCode");
        thxCodeSystem.setUrl("http://terminology.hl7.org/CodeSystem/v3-ActCode");
        thxCodeSystem.setVersion("5.3.0");
        thxCodeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
        thxCodeSystem.addExtension(Constants.PACKAGE_SOURCE, new StringType("hl7.terminology#5.3.0"));
        repository.create(thxCodeSystem);

        // Create IG dependency version map
        Map<String, String> igDependencyVersions = new HashMap<>();
        igDependencyVersions.put("hl7.fhir.us.core", "6.1.0");
        igDependencyVersions.put("hl7.fhir.uv.sdc", "3.0.0");
        igDependencyVersions.put("hl7.terminology", "5.3.0");

        // Create visitor
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        // Test: resolve multiple unversioned canonicals
        String resolved1 = resolveCanonicalWithIgVersion(
                visitor, "http://hl7.org/fhir/us/core/CodeSystem/us-core-race", igDependencyVersions);
        assertEquals("http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0", resolved1);

        String resolved2 = resolveCanonicalWithIgVersion(
                visitor, "http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type", igDependencyVersions);
        assertEquals("http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0", resolved2);

        String resolved3 = resolveCanonicalWithIgVersion(
                visitor, "http://terminology.hl7.org/CodeSystem/v3-ActCode", igDependencyVersions);
        assertEquals("http://terminology.hl7.org/CodeSystem/v3-ActCode|5.3.0", resolved3);
    }

    @Test
    void resolveCanonicalWithIgVersion_PreservesExistingVersion() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create IG dependency version map
        Map<String, String> igDependencyVersions = new HashMap<>();
        igDependencyVersions.put("hl7.fhir.us.core", "6.1.0");

        // Create visitor
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        // Test: canonical already has a version - should NOT modify
        String versionedCanonical = "http://hl7.org/fhir/us/core/CodeSystem/us-core-race|5.0.0";
        String resolved = resolveCanonicalWithIgVersion(visitor, versionedCanonical, igDependencyVersions);

        assertEquals(
                "http://hl7.org/fhir/us/core/CodeSystem/us-core-race|5.0.0",
                resolved,
                "Should NOT modify canonical that already has version");
    }

    @Test
    void resolveCanonicalWithIgVersion_FallsBackToLatestWhenNoPackageSource() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create a CodeSystem WITHOUT package-source extension
        var codeSystem = new CodeSystem();
        codeSystem.setId("local-code-system");
        codeSystem.setUrl("http://example.org/CodeSystem/local");
        codeSystem.setVersion("2.0.0");
        codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
        // No package-source extension!
        repository.create(codeSystem);

        // Create IG dependency version map (even though it won't match)
        Map<String, String> igDependencyVersions = new HashMap<>();
        igDependencyVersions.put("hl7.fhir.us.core", "6.1.0");

        // Create visitor
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        // Test: resource has no package source, should return unchanged
        String unversionedCanonical = "http://example.org/CodeSystem/local";
        String resolved = resolveCanonicalWithIgVersion(visitor, unversionedCanonical, igDependencyVersions);

        assertEquals(
                "http://example.org/CodeSystem/local",
                resolved,
                "Should return unchanged when no package source found");
    }

    @Test
    void resolveCanonicalWithIgVersion_EmptyIgDependencies() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Empty IG dependency version map
        Map<String, String> igDependencyVersions = new HashMap<>();

        // Create visitor
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        // Test: empty dependencies map should return unchanged
        String unversionedCanonical = "http://hl7.org/fhir/us/core/CodeSystem/us-core-race";
        String resolved = resolveCanonicalWithIgVersion(visitor, unversionedCanonical, igDependencyVersions);

        assertEquals(
                "http://hl7.org/fhir/us/core/CodeSystem/us-core-race",
                resolved,
                "Should return unchanged when IG dependencies map is empty");
    }

    @Test
    void resolveCanonicalWithIgVersion_NullCanonical() {
        var repository = new InMemoryFhirRepository(fhirContext);

        Map<String, String> igDependencyVersions = new HashMap<>();
        igDependencyVersions.put("hl7.fhir.us.core", "6.1.0");

        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        // Test: null canonical should return null
        String resolved = resolveCanonicalWithIgVersion(visitor, null, igDependencyVersions);

        assertEquals(null, resolved, "Should return null for null canonical");
    }
}
