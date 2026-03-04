package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.Enumerations.BindingStrength;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;

class KeyElementAnalyzerTest {

    private KeyElementAnalyzer createAnalyzer() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        var resolver = new ConformanceResourceResolver(repository);
        return new KeyElementAnalyzer(resolver, FhirVersionEnum.R4);
    }

    @Test
    void testGetKeyElementValueSets_EmptyProfile() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(result.isEmpty(), "Empty profile should have no key element ValueSets");
    }

    @Test
    void testGetKeyElementValueSets_SpecializationReturnsEmpty() {
        var analyzer = createAnalyzer();

        // Create a SPECIALIZATION (not CONSTRAINT) - should return empty
        var profile = new StructureDefinition();
        profile.setUrl("http://example.org/StructureDefinition/TestPatient");
        profile.setType("Patient");
        profile.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
        profile.setAbstract(false);
        profile.setDerivation(StructureDefinition.TypeDerivationRule.SPECIALIZATION);

        var rootElement = profile.getSnapshot().addElement();
        rootElement.setId("Patient");
        rootElement.setPath("Patient");

        var element = profile.getSnapshot().addElement();
        element.setId("Patient.maritalStatus");
        element.setPath("Patient.maritalStatus");
        element.setMustSupport(true);
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(result.isEmpty(), "SPECIALIZATION derivation should return empty ValueSets");
    }

    @Test
    void testGetKeyElementValueSets_MustSupportElement() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add mustSupport element with binding to snapshot
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.maritalStatus");
        element.setPath("Patient.maritalStatus");
        element.setMustSupport(true);
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(1, result.size(), "Should find one ValueSet bound to key element");
        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/marital-status"),
                "Should contain marital-status ValueSet");
    }

    @Test
    void testGetKeyElementValueSets_InDifferential() {
        var analyzer = createAnalyzer();
        var profile = createConstraintProfileWithSnapshot(
                "Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add element to both snapshot and differential
        var snapshotElement = profile.getSnapshot().addElement();
        snapshotElement.setId("Observation.status");
        snapshotElement.setPath("Observation.status");
        snapshotElement
                .getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/observation-status");

        var diffElement = profile.getDifferential().addElement();
        diffElement.setId("Observation.status");
        diffElement.setPath("Observation.status");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(1, result.size(), "Should find ValueSet for element in differential");
        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/observation-status"),
                "Should contain observation-status ValueSet");
    }

    @Test
    void testGetKeyElementValueSets_MandatoryChild() {
        var analyzer = createAnalyzer();
        var profile = createConstraintProfileWithSnapshot(
                "Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add parent element with mustSupport in snapshot
        var parentElement = profile.getSnapshot().addElement();
        parentElement.setId("Observation.component");
        parentElement.setPath("Observation.component");
        parentElement.setMustSupport(true);

        // Add mandatory child (min > 0) with binding in snapshot
        var childElement = profile.getSnapshot().addElement();
        childElement.setId("Observation.component.code");
        childElement.setPath("Observation.component.code");
        childElement.setMin(1); // Mandatory
        childElement
                .getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/component-codes");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/component-codes"),
                "Should include ValueSet from mandatory child of key element");
    }

    @Test
    void testGetKeyElementValueSets_SliceElement() {
        var analyzer = createAnalyzer();
        var profile = createConstraintProfileWithSnapshot(
                "Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add parent element with mustSupport in snapshot
        var parentElement = profile.getSnapshot().addElement();
        parentElement.setId("Observation.component");
        parentElement.setPath("Observation.component");
        parentElement.setMustSupport(true);

        // Add slice with binding in snapshot
        var sliceElement = profile.getSnapshot().addElement();
        sliceElement.setId("Observation.component:bloodPressure");
        sliceElement.setPath("Observation.component");
        sliceElement.setSliceName("bloodPressure");
        sliceElement
                .getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/bp-codes");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/bp-codes"),
                "Should include ValueSet from slice of key element");
    }

    @Test
    void testGetKeyElementValueSets_ModifierElement() {
        var analyzer = createAnalyzer();
        var profile = createConstraintProfileWithSnapshot(
                "AllergyIntolerance", "http://example.org/StructureDefinition/TestAllergy");

        // Add mustSupport parent element in snapshot
        var parentElement = profile.getSnapshot().addElement();
        parentElement.setId("AllergyIntolerance.clinicalStatus");
        parentElement.setPath("AllergyIntolerance.clinicalStatus");
        parentElement.setMustSupport(true);

        // Add modifier element in snapshot
        var modifierElement = profile.getSnapshot().addElement();
        modifierElement.setId("AllergyIntolerance.clinicalStatus.coding");
        modifierElement.setPath("AllergyIntolerance.clinicalStatus.coding");
        modifierElement.setIsModifier(true);
        modifierElement
                .getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/allergy-status");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/allergy-status"),
                "Should include ValueSet from modifier element");
    }

    @Test
    void testGetKeyElementValueSets_MaxConstrainedFromBase() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element where max differs from base max
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.identifier");
        element.setPath("Patient.identifier");
        element.setMax("1"); // Constrained from *
        element.getBase().setPath("Patient.identifier").setMin(0).setMax("*");
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/identifier-types");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/identifier-types"),
                "Should include ValueSet from element with max constrained from base");
    }

    @Test
    void testGetKeyElementValueSets_MaxNotConstrainedWhenSameAsBase() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element where max equals base max - NOT key by any criterion
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.identifier");
        element.setPath("Patient.identifier");
        element.setMax("1");
        element.getBase().setPath("Patient.identifier").setMin(0).setMax("1");
        // Use EXAMPLE strength which does not trigger binding comparison
        element.getBinding()
                .setStrength(BindingStrength.EXAMPLE)
                .setValueSet("http://example.org/ValueSet/identifier-types");

        var result = analyzer.getKeyElementValueSets(profile);

        assertFalse(
                result.contains("http://example.org/ValueSet/identifier-types"),
                "Should NOT include ValueSet when max equals base max and no other criteria met");
    }

    @Test
    void testGetKeyElementValueSets_FixedValue() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element with fixed value
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.gender");
        element.setPath("Patient.gender");
        element.setFixed(new org.hl7.fhir.r4.model.CodeType("male"));
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/administrative-gender");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/administrative-gender"),
                "Should include ValueSet from element with fixed value");
    }

    @Test
    void testGetKeyElementValueSets_PatternValue() {
        var analyzer = createAnalyzer();
        var profile = createConstraintProfileWithSnapshot(
                "Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add element with pattern value
        var element = profile.getSnapshot().addElement();
        element.setId("Observation.code");
        element.setPath("Observation.code");
        element.setPattern(new org.hl7.fhir.r4.model.CodeableConcept()
                .addCoding(new org.hl7.fhir.r4.model.Coding()
                        .setSystem("http://loinc.org")
                        .setCode("12345-6")));
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/observation-codes");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/observation-codes"),
                "Should include ValueSet from element with pattern value");
    }

    @Test
    void testGetKeyElementValueSets_MultipleValueSets() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add first mustSupport element
        var element1 = profile.getSnapshot().addElement();
        element1.setId("Patient.maritalStatus");
        element1.setPath("Patient.maritalStatus");
        element1.setMustSupport(true);
        element1.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/marital-status");

        // Add second mustSupport element
        var element2 = profile.getSnapshot().addElement();
        element2.setId("Patient.communication");
        element2.setPath("Patient.communication");
        element2.setMustSupport(true);

        var element3 = profile.getSnapshot().addElement();
        element3.setId("Patient.communication.language");
        element3.setPath("Patient.communication.language");
        element3.setMustSupport(true);
        element3.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/languages");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(2, result.size(), "Should find two ValueSets");
        assertTrue(result.contains("http://hl7.org/fhir/ValueSet/marital-status"));
        assertTrue(result.contains("http://hl7.org/fhir/ValueSet/languages"));
    }

    @Test
    void testGetKeyElementValueSets_VersionedValueSetUrl() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element with versioned ValueSet URL
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.gender");
        element.setPath("Patient.gender");
        element.setMustSupport(true);
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1");

        var result = analyzer.getKeyElementValueSets(profile);

        assertEquals(1, result.size());
        assertTrue(
                result.contains("http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1"),
                "Should preserve version in ValueSet URL");
    }

    @Test
    void testGetKeyElementValueSets_NullInput() {
        var analyzer = createAnalyzer();

        var result = analyzer.getKeyElementValueSets(null);

        assertTrue(result.isEmpty(), "Null input should return empty set");
    }

    @Test
    void testGetKeyElementValueSets_NoBindings() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element with mustSupport but no binding
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.active");
        element.setPath("Patient.active");
        element.setMustSupport(true);
        // No binding set

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(result.isEmpty(), "Should have no ValueSets when elements have no bindings");
    }

    @Test
    void testGetKeyElementValueSets_MinConstrainedFromBase() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element where min differs from base min
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.name");
        element.setPath("Patient.name");
        element.setMin(1);
        element.getBase().setPath("Patient.name").setMin(0).setMax("*");
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/name-types");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/name-types"),
                "Should include ValueSet from element with min constrained from base");
    }

    @Test
    void testGetKeyElementValueSets_HasCondition() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element with condition
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.deceased");
        element.setPath("Patient.deceased");
        element.addCondition("dom-1");
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/deceased-types");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/deceased-types"),
                "Should include ValueSet from element with condition");
    }

    @Test
    void testGetKeyElementValueSets_HasMaxLength() {
        var analyzer = createAnalyzer();
        var profile =
                createConstraintProfileWithSnapshot("Patient", "http://example.org/StructureDefinition/TestPatient");

        // Add element with maxLength
        var element = profile.getSnapshot().addElement();
        element.setId("Patient.name");
        element.setPath("Patient.name");
        element.setMaxLength(50);
        element.getBinding()
                .setStrength(BindingStrength.REQUIRED)
                .setValueSet("http://example.org/ValueSet/name-values");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/name-values"),
                "Should include ValueSet from element with maxLength");
    }

    @Test
    void testGetKeyElementValueSets_SignificantExtension() {
        var analyzer = createAnalyzer();
        var profile = createConstraintProfileWithSnapshot(
                "Observation", "http://example.org/StructureDefinition/TestObservation");

        // Add element with significant extension
        var element = profile.getSnapshot().addElement();
        element.setId("Observation.value");
        element.setPath("Observation.value");
        element.addExtension(
                "http://hl7.org/fhir/StructureDefinition/elementdefinition-allowedUnits",
                new org.hl7.fhir.r4.model.CodeableConcept());
        element.getBinding().setStrength(BindingStrength.REQUIRED).setValueSet("http://example.org/ValueSet/units");

        var result = analyzer.getKeyElementValueSets(profile);

        assertTrue(
                result.contains("http://example.org/ValueSet/units"),
                "Should include ValueSet from element with significant extension");
    }

    /**
     * Helper method to create a StructureDefinition with CONSTRAINT derivation and snapshot root element.
     */
    private StructureDefinition createConstraintProfileWithSnapshot(String baseType, String url) {
        var profile = new StructureDefinition();
        profile.setUrl(url);
        profile.setType(baseType);
        profile.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
        profile.setAbstract(false);
        profile.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);
        profile.setBaseDefinition("http://hl7.org/fhir/StructureDefinition/" + baseType);

        // Add root element to snapshot
        var rootElement = profile.getSnapshot().addElement();
        rootElement.setId(baseType);
        rootElement.setPath(baseType);

        return profile;
    }
}
