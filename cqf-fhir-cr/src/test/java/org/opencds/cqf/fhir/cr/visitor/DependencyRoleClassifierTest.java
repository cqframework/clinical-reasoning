package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import org.hl7.fhir.r4.model.Enumerations.BindingStrength;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;

class DependencyRoleClassifierTest {

    private final AdapterFactory adapterFactory = new AdapterFactory();

    private ConformanceResourceResolver createResolver() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        return new ConformanceResourceResolver(repository);
    }

    @Test
    void testAllDependenciesGetDefaultRole() {
        var resolver = createResolver();

        var measure = new Measure();
        measure.setUrl("http://example.org/Measure/test");
        var measureAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(measure);

        var dependency = createDependency("http://example.org/Library/helper");

        var roles = DependencyRoleClassifier.classifyDependencyRoles(dependency, measureAdapter, null, resolver);

        assertTrue(roles.contains("default"), "All dependencies should get default role");
    }

    // Helper method to create DependencyInfo with required updateReferenceConsumer
    private DependencyInfo createDependency(String reference) {
        return new DependencyInfo("source", reference, new ArrayList<>(), ref -> {});
    }

    @Test
    void testMeasureDependencyGetsOnlyDefault() {
        var resolver = createResolver();

        var measure = new Measure();
        measure.setUrl("http://example.org/Measure/test");
        var measureAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(measure);

        var library = new Library();
        library.setUrl("http://example.org/Library/primary");
        var libraryAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        var dependency = createDependency("http://example.org/Library/primary");
        dependency.addFhirPath("library[0]");

        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, measureAdapter, libraryAdapter, resolver);

        assertEquals(1, roles.size(), "Measure library dependency should only get default role (no heuristics)");
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("key"), "Should not use heuristics to classify as key");
    }

    @Test
    void testLibraryDependencyGetsOnlyDefault() {
        var resolver = createResolver();

        var library = new Library();
        library.setUrl("http://example.org/Library/main");
        var libraryAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        var helperLibrary = new Library();
        helperLibrary.setUrl("http://example.org/Library/helper");
        var helperAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(helperLibrary);

        var dependency = createDependency("http://example.org/Library/helper");

        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, libraryAdapter, helperAdapter, resolver);

        assertEquals(1, roles.size());
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("key"));
    }

    @Test
    void testStructureDefinitionWithNonValueSetDependency() {
        var resolver = createResolver();

        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        var profileAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(profile);

        // Dependency is not a ValueSet (e.g., another profile)
        var dependency = createDependency("http://hl7.org/fhir/StructureDefinition/Patient");

        var roles = DependencyRoleClassifier.classifyDependencyRoles(dependency, profileAdapter, null, resolver);

        assertEquals(1, roles.size());
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("key"), "Non-ValueSet dependencies should not be classified as key");
    }

    @Test
    void testStructureDefinitionWithValueSetNotBoundToKeyElement() {
        var resolver = createResolver();

        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);

        // Add non-key element with binding in snapshot
        var rootElement = profile.getSnapshot().addElement();
        rootElement.setId("Patient");
        rootElement.setPath("Patient");

        var element = profile.getSnapshot().addElement();
        element.setId("Patient.contact.relationship");
        element.setPath("Patient.contact.relationship");
        element.setMustSupport(false); // Not a key element
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/contact-relationship");

        var profileAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(profile);

        var valueSet = new ValueSet();
        valueSet.setUrl("http://example.org/ValueSet/different-valueset");
        var valueSetAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(valueSet);

        var dependency = createDependency("http://example.org/ValueSet/different-valueset");

        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, profileAdapter, valueSetAdapter, resolver);

        assertEquals(1, roles.size());
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("key"), "ValueSet not bound to key element should not be classified as key");
    }

    @Test
    void testStructureDefinitionWithValueSetBoundToKeyElement() {
        var resolver = createResolver();

        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);

        // Add root element to snapshot
        var rootElement = profile.getSnapshot().addElement();
        rootElement.setId("Patient");
        rootElement.setPath("Patient");

        // Add key element with binding in snapshot
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.maritalStatus");
        element.setPath("Patient.maritalStatus");
        element.setMustSupport(true); // Key element
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        var profileAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(profile);

        var valueSet = new ValueSet();
        valueSet.setUrl("http://hl7.org/fhir/ValueSet/marital-status");
        var valueSetAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(valueSet);

        var dependency = createDependency("http://hl7.org/fhir/ValueSet/marital-status");

        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, profileAdapter, valueSetAdapter, resolver);

        assertEquals(2, roles.size(), "ValueSet bound to key element should get both key and default roles");
        assertTrue(roles.contains("key"));
        assertTrue(roles.contains("default"));
    }

    @Test
    void testStructureDefinitionWithVersionedValueSetUrl() {
        var resolver = createResolver();

        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);

        // Add root element to snapshot
        var rootElement = profile.getSnapshot().addElement();
        rootElement.setId("Patient");
        rootElement.setPath("Patient");

        // Add key element with versioned ValueSet binding in snapshot
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.gender");
        element.setPath("Patient.gender");
        element.setMustSupport(true);
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1");

        var profileAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(profile);

        var valueSet = new ValueSet();
        valueSet.setUrl("http://hl7.org/fhir/ValueSet/administrative-gender");
        var valueSetAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(valueSet);

        // Dependency reference without version
        var dependency = createDependency("http://hl7.org/fhir/ValueSet/administrative-gender");

        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, profileAdapter, valueSetAdapter, resolver);

        assertTrue(roles.contains("key"), "Should match ValueSet ignoring version");
        assertTrue(roles.contains("default"));
    }

    @Test
    void testStructureDefinitionWithMultipleValueSets() {
        var resolver = createResolver();

        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);

        // Add root element to snapshot
        var rootElement = profile.getSnapshot().addElement();
        rootElement.setId("Patient");
        rootElement.setPath("Patient");

        // Add first key element
        var element1 = profile.getSnapshot().addElement();
        element1.setId("Patient.maritalStatus");
        element1.setPath("Patient.maritalStatus");
        element1.setMustSupport(true);
        element1.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        // Add second key element
        var element2 = profile.getSnapshot().addElement();
        element2.setId("Patient.gender");
        element2.setPath("Patient.gender");
        element2.setMustSupport(true);
        element2.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/administrative-gender");

        var profileAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(profile);

        // Test first ValueSet — must provide resolved ValueSet adapter
        var vs1 = new ValueSet();
        vs1.setUrl("http://hl7.org/fhir/ValueSet/marital-status");
        var vs1Adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(vs1);
        var dependency1 = createDependency("http://hl7.org/fhir/ValueSet/marital-status");
        var roles1 =
                DependencyRoleClassifier.classifyDependencyRoles(dependency1, profileAdapter, vs1Adapter, resolver);

        assertTrue(roles1.contains("key"));
        assertTrue(roles1.contains("default"));

        // Test second ValueSet
        var vs2 = new ValueSet();
        vs2.setUrl("http://hl7.org/fhir/ValueSet/administrative-gender");
        var vs2Adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(vs2);
        var dependency2 = createDependency("http://hl7.org/fhir/ValueSet/administrative-gender");
        var roles2 =
                DependencyRoleClassifier.classifyDependencyRoles(dependency2, profileAdapter, vs2Adapter, resolver);

        assertTrue(roles2.contains("key"));
        assertTrue(roles2.contains("default"));
    }

    @Test
    void testNonFhirUrlValueSetClassifiedAsKeyWhenResolved() {
        var resolver = createResolver();

        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);

        // Add root element and key element with binding to a non-FHIR-URL ValueSet
        var rootElement = profile.getSnapshot().addElement();
        rootElement.setId("Patient");
        rootElement.setPath("Patient");

        var element = profile.getSnapshot().addElement();
        element.setId("Patient.maritalStatus");
        element.setPath("Patient.maritalStatus");
        element.setMustSupport(true);
        element.getBinding().setStrength(BindingStrength.REQUIRED).setValueSet("http://www.ada.org/cdt");

        var profileAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(profile);

        // Provide a resolved ValueSet adapter with a non-FHIR canonical URL
        var valueSet = new ValueSet();
        valueSet.setUrl("http://www.ada.org/cdt");
        var valueSetAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(valueSet);

        var dependency = createDependency("http://www.ada.org/cdt");

        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, profileAdapter, valueSetAdapter, resolver);

        assertTrue(roles.contains("key"), "Non-FHIR-URL ValueSet should be classified as key when resolved");
        assertTrue(roles.contains("default"));
    }

    @Test
    void testNullResolver() {
        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        var profileAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(profile);

        var dependency = createDependency("http://example.org/ValueSet/test");

        var roles = DependencyRoleClassifier.classifyDependencyRoles(dependency, profileAdapter, null, null);

        assertTrue(roles.contains("default"), "Should handle null resolver gracefully");
    }

    @Test
    void testTransitiveKeyRoleNotPropagatedByClassifier() {
        // DependencyRoleClassifier only classifies direct relationships
        // Transitive propagation is handled by ReleaseVisitor
        var resolver = createResolver();

        var library = new Library();
        library.setUrl("http://example.org/Library/parent");
        var libraryAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        var childVs = new ValueSet();
        childVs.setUrl("http://example.org/ValueSet/child");
        var childAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(childVs);

        var dependency = createDependency("http://example.org/ValueSet/child");

        // Direct classification - library to valueset - should only be default
        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, libraryAdapter, childAdapter, resolver);

        assertEquals(1, roles.size(), "Direct library->valueset dependency should only get default role");
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("key"), "Transitive propagation is not classifier's responsibility");
    }

    @Test
    void testNullDependencyArtifact() {
        var resolver = createResolver();

        var measure = new Measure();
        measure.setUrl("http://example.org/Measure/test");
        var measureAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(measure);

        var dependency = createDependency("http://example.org/Library/helper");

        var roles = DependencyRoleClassifier.classifyDependencyRoles(dependency, measureAdapter, null, resolver);

        assertTrue(roles.contains("default"), "Should handle null dependency artifact");
        assertEquals(1, roles.size());
    }

    @Test
    void testNoHeuristicsForUrlPatterns() {
        var resolver = createResolver();

        var measure = new Measure();
        measure.setUrl("http://example.org/Measure/test");
        var measureAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(measure);

        // Create library with URL containing "test"
        var testLibrary = new Library();
        testLibrary.setUrl("http://example.org/Library/test-helper");
        testLibrary.setName("TestLibrary");
        testLibrary.setTitle("Test Library for Testing");
        var testLibraryAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(testLibrary);

        var dependency = createDependency("http://example.org/Library/test-helper");

        var roles = DependencyRoleClassifier.classifyDependencyRoles(
                dependency, measureAdapter, testLibraryAdapter, resolver);

        assertEquals(1, roles.size(), "Should not apply heuristics based on URL/name/title patterns");
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("test"), "Should not classify as 'test' based on URL pattern");
        assertFalse(roles.contains("example"), "Should not classify as 'example' based on URL pattern");
    }

    @Test
    void testNoHeuristicsForFhirPathPatterns() {
        var resolver = createResolver();

        var measure = new Measure();
        measure.setUrl("http://example.org/Measure/test");
        var measureAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(measure);

        var library = new Library();
        library.setUrl("http://example.org/Library/primary");
        var libraryAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        var dependency = createDependency("http://example.org/Library/primary");
        // Add FHIRPath that would have triggered heuristics before
        dependency.addFhirPath("library[0]");
        dependency.addFhirPath("population[0].criteria");

        var roles =
                DependencyRoleClassifier.classifyDependencyRoles(dependency, measureAdapter, libraryAdapter, resolver);

        assertEquals(1, roles.size(), "Should not apply heuristics based on FHIRPath patterns");
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("key"), "Should not classify as 'key' based on FHIRPath patterns");
    }

    @Test
    void testNoHeuristicsForExperimentalStatus() {
        var resolver = createResolver();

        var measure = new Measure();
        measure.setUrl("http://example.org/Measure/production");
        measure.setExperimental(false);
        var measureAdapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(measure);

        var experimentalLibrary = new Library();
        experimentalLibrary.setUrl("http://example.org/Library/experimental");
        experimentalLibrary.setExperimental(true);
        var experimentalAdapter =
                (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(experimentalLibrary);

        var dependency = createDependency("http://example.org/Library/experimental");

        var roles = DependencyRoleClassifier.classifyDependencyRoles(
                dependency, measureAdapter, experimentalAdapter, resolver);

        assertEquals(1, roles.size(), "Should not apply heuristics based on experimental status");
        assertTrue(roles.contains("default"));
        assertFalse(roles.contains("test"), "Should not classify as 'test' based on experimental status");
    }
}
