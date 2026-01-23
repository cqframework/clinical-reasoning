package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;

class KeyElementAnalyzerTest {

    @Test
    void testGetKeyElementValueSets_EmptyProfile() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(result.isEmpty(), "Empty profile should have no key element ValueSets");
    }

    @Test
    void testGetKeyElementValueSets_MustSupportElement() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add mustSupport element with binding
        var element = profile.getDifferential().addElement();
        element.setPath("Patient.maritalStatus");
        element.setMustSupport(true);
        element.getBinding().setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(1, result.size(), "Should find one ValueSet bound to key element");
        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/marital-status"),
                "Should contain marital-status ValueSet");
    }

    @Test
    void testGetKeyElementValueSets_DifferentialElement() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add element in differential with binding (even without mustSupport)
        var element = profile.getDifferential().addElement();
        element.setPath("Observation.status");
        element.getBinding().setValueSet("http://hl7.org/fhir/ValueSet/observation-status");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(1, result.size(), "Should find ValueSet for differential element");
        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/observation-status"),
                "Should contain observation-status ValueSet");
    }

    @Test
    void testGetKeyElementValueSets_MandatoryChild() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add parent element with mustSupport
        var parentElement = profile.getDifferential().addElement();
        parentElement.setPath("Observation.component");
        parentElement.setMustSupport(true);

        // Add mandatory child (min > 0) with binding
        var childElement = profile.getDifferential().addElement();
        childElement.setPath("Observation.component.code");
        childElement.setMin(1); // Mandatory
        childElement.getBinding().setValueSet("http://example.org/ValueSet/component-codes");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/component-codes"),
                "Should include ValueSet from mandatory child of key element");
    }

    @Test
    void testGetKeyElementValueSets_SliceElement() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add parent element
        var parentElement = profile.getDifferential().addElement();
        parentElement.setPath("Observation.component");
        parentElement.setMustSupport(true);

        // Add slice with binding
        var sliceElement = profile.getDifferential().addElement();
        sliceElement.setPath("Observation.component");
        sliceElement.setSliceName("bloodPressure");
        sliceElement.getBinding().setValueSet("http://example.org/ValueSet/bp-codes");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/bp-codes"),
                "Should include ValueSet from slice of key element");
    }

    @Test
    void testGetKeyElementValueSets_ConstrainedElement() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add parent element
        var parentElement = profile.getDifferential().addElement();
        parentElement.setPath("Patient.identifier");
        parentElement.setMustSupport(true);

        // Add child with constraint (max constrained from * to 1)
        var childElement = profile.getDifferential().addElement();
        childElement.setPath("Patient.identifier.type");
        childElement.setMax("1"); // Constrained from *
        childElement.getBinding().setValueSet("http://example.org/ValueSet/identifier-types");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/identifier-types"),
                "Should include ValueSet from constrained child");
    }

    @Test
    void testGetKeyElementValueSets_ModifierElement() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("AllergyIntolerance", "http://example.org/StructureDefinition/TestAllergy");

        // Add parent element
        var parentElement = profile.getDifferential().addElement();
        parentElement.setPath("AllergyIntolerance.clinicalStatus");
        parentElement.setMustSupport(true);

        // Add modifier element
        var modifierElement = profile.getDifferential().addElement();
        modifierElement.setPath("AllergyIntolerance.clinicalStatus.coding");
        modifierElement.setIsModifier(true);
        modifierElement.getBinding().setValueSet("http://example.org/ValueSet/allergy-status");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/allergy-status"),
                "Should include ValueSet from modifier element");
    }

    @Test
    void testGetKeyElementValueSets_MultipleValueSets() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add multiple elements with bindings
        var element1 = profile.getDifferential().addElement();
        element1.setPath("Patient.maritalStatus");
        element1.setMustSupport(true);
        element1.getBinding().setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        var element2 = profile.getDifferential().addElement();
        element2.setPath("Patient.communication.language");
        element2.setMustSupport(true);
        element2.getBinding().setValueSet("http://hl7.org/fhir/ValueSet/languages");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(2, result.size(), "Should find two ValueSets");
        assertTrue(result.contains("http://hl7.org/fhir/ValueSet/marital-status"));
        assertTrue(result.contains("http://hl7.org/fhir/ValueSet/languages"));
    }

    @Test
    void testGetKeyElementValueSets_VersionedValueSetUrl() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element with versioned ValueSet URL
        var element = profile.getDifferential().addElement();
        element.setPath("Patient.gender");
        element.setMustSupport(true);
        element.getBinding().setValueSet("http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(1, result.size());
        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1"),
                "Should preserve version in ValueSet URL");
    }

    @Test
    void testGetKeyElementValueSets_NullRepository() {
        var analyzer = new KeyElementAnalyzer(null);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        var element = profile.getDifferential().addElement();
        element.setPath("Patient.maritalStatus");
        element.setMustSupport(true);
        element.getBinding().setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        var result = analyzer.getKeyElementValueSets(profile);

        // Should still work for single profile without base definition resolution
        assertEquals(1, result.size());
        assertTrue(result.contains("http://hl7.org/fhir/ValueSet/marital-status"));
    }

    @Test
    void testGetKeyElementValueSets_CoreBaseDefinition() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Set base definition to core FHIR (should stop inheritance walk)
        profile.setBaseDefinition("http://hl7.org/fhir/StructureDefinition/Patient");

        var element = profile.getDifferential().addElement();
        element.setPath("Patient.active");
        element.setMustSupport(true);

        var result = analyzer.getKeyElementValueSets(profile);

        // Should not error even though base definition resolution stops at core
        assertFalse(
                result.contains("http://hl7.org/fhir/StructureDefinition/Patient"),
                "Should not include base definition in ValueSet results");
    }

    @Test
    void testGetKeyElementValueSets_NoBindings() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);
        var profile = createSimpleProfile("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element with mustSupport but no binding
        var element = profile.getDifferential().addElement();
        element.setPath("Patient.active");
        element.setMustSupport(true);
        // No binding set

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(result.isEmpty(), "Should have no ValueSets when elements have no bindings");
    }

    @Test
    void testGetKeyElementValueSets_WithSnapshot() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var analyzer = new KeyElementAnalyzer(repository);

        // Create profile with empty differential
        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        profile.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
        profile.setAbstract(false);
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);

        // Add element to snapshot only (differential is empty)
        var snapshotElement = profile.getSnapshot().addElement();
        snapshotElement.setPath("Patient.gender");
        snapshotElement.setMustSupport(true);
        snapshotElement.getBinding().setValueSet("http://hl7.org/fhir/ValueSet/administrative-gender");

        var result = analyzer.getKeyElementValueSets(profile);

        // Should work with snapshot if differential is empty
        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/administrative-gender"),
                "Should extract ValueSets from snapshot when differential is empty");
    }

    // Helper method to create a simple StructureDefinition
    private StructureDefinition createSimpleProfile(String baseType, String url) {
        var profile = new StructureDefinition();
        profile.setUrl(url);
        profile.setType(baseType);
        profile.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
        profile.setAbstract(false);
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);

        // Add root element to differential
        var rootElement = profile.getDifferential().addElement();
        rootElement.setPath(baseType);

        return profile;
    }
}
