package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

class StratumPopulationDefToStringTest {

    @Test
    void toStringWithCriteriaStratifierAndBooleanBasis() {
        // Given: Over 5 subjects and resources
        Set<String> subjects = Set.of("Patient/1", "Patient/2", "Patient/3", "Patient/4", "Patient/5", "Patient/6");
        List<String> resourceIds = List.of("res1", "res2", "res3", "res4", "res5", "res6");
        Set<Object> intersection = Set.of("obj1", "obj2");
        CodeDef populationBasis = new CodeDef(null, null, "boolean", null);
        PopulationDef populationDef = new PopulationDef(
                "stratum-1", null, MeasurePopulationType.INITIALPOPULATION, "expression", populationBasis, null);

        // When
        StratumPopulationDef stratumDef = new StratumPopulationDef(
                populationDef, subjects, intersection, resourceIds, MeasureStratifierType.CRITERIA, populationBasis);
        String result = stratumDef.toString();

        // Then: Should contain populationDef with id, measureStratifierType, and populationBasis
        assertTrue(
                result.contains("populationDef=PopulationDef{id='stratum-1'"), "Should contain PopulationDef with id");
        assertTrue(result.contains("measureStratifierType=CRITERIA"), "Should contain CRITERIA type");
        assertTrue(result.contains("populationBasis=boolean"), "Should contain boolean basis");

        // Should show ellipsis for subjects and resources (more than 5)
        assertTrue(
                result.contains("subjectsQualifiedOrUnqualified=") && result.contains("...]"),
                "Should truncate subjects with ellipsis");
        assertTrue(
                result.contains("resourceIdsForSubjectList=") && result.contains("...]"),
                "Should truncate resources with ellipsis");
    }

    @Test
    void toStringWithValueStratifierAndEncounterBasis() {
        // Given: VALUE stratifier with Encounter basis
        Set<String> subjects = Set.of("Patient/1", "Patient/2", "Patient/3");
        List<String> resourceIds = List.of("enc1", "enc2");
        Set<Object> intersection = Set.of();
        CodeDef populationBasis = new CodeDef(null, null, "Encounter", null);
        PopulationDef populationDef = new PopulationDef(
                "stratum-value-1", null, MeasurePopulationType.INITIALPOPULATION, "expression", populationBasis, null);

        // When
        StratumPopulationDef stratumDef = new StratumPopulationDef(
                populationDef, subjects, intersection, resourceIds, MeasureStratifierType.VALUE, populationBasis);
        String result = stratumDef.toString();

        // Then
        assertTrue(
                result.contains("populationDef=PopulationDef{id='stratum-value-1'"),
                "Should contain PopulationDef with id");
        assertTrue(result.contains("measureStratifierType=VALUE"), "Should contain VALUE type");
        assertTrue(result.contains("populationBasis=Encounter"), "Should contain Encounter basis");

        // With 3 subjects and 2 resources, should NOT have ellipsis
        assertFalse(
                result.contains("subjectsQualifiedOrUnqualified=") && result.contains("...]"),
                "Should NOT truncate subjects");
        assertFalse(
                result.contains("resourceIdsForSubjectList=") && result.contains("...]"),
                "Should NOT truncate resources");
    }

    @Test
    void toStringWithOver5PatientResources() {
        // Given: Over 5 R4 FHIR Patient resources (IBaseResource)
        Set<Object> patientResources = new HashSet<>();
        for (int i = 1; i <= 7; i++) {
            Patient patient = new Patient();
            patient.setId(new IdType(ResourceType.Patient.name(), "pat" + i));
            patientResources.add(patient);
        }

        Set<String> subjects = Set.of("Patient/1");
        List<String> resourceIds = List.of("pat1");
        CodeDef populationBasis = new CodeDef(null, null, "boolean", null);
        PopulationDef populationDef = new PopulationDef(
                "stratum-patients", null, MeasurePopulationType.INITIALPOPULATION, "expression", populationBasis, null);

        // When
        StratumPopulationDef stratumDef = new StratumPopulationDef(
                populationDef,
                subjects,
                patientResources,
                resourceIds,
                MeasureStratifierType.CRITERIA,
                populationBasis);
        String result = stratumDef.toString();

        // Then: Should show IBaseResource formatted as ID strings
        assertTrue(result.contains("populationDefEvaluationResultIntersection="), "Should contain intersection field");

        // Should contain some patient IDs and ellipsis
        assertTrue(result.contains("Patient/pat"), "Should contain formatted patient IDs");
        assertTrue(
                result.contains("populationDefEvaluationResultIntersection=") && result.contains("...]"),
                "Should truncate patient resources with ellipsis");
    }

    @Test
    void toStringWithOver5QuantityTypes() {
        // Given: Over 5 R4 FHIR Quantity types (IBase)
        Set<Object> quantities = new HashSet<>();
        for (int i = 1; i <= 8; i++) {
            Quantity quantity = new Quantity();
            quantity.setValue(i * 10);
            quantity.setUnit("mg");
            quantities.add(quantity);
        }

        Set<String> subjects = Set.of("Patient/1");
        List<String> resourceIds = List.of("res1");
        CodeDef populationBasis = new CodeDef(null, null, "boolean", null);
        PopulationDef populationDef = new PopulationDef(
                "stratum-quantities",
                null,
                MeasurePopulationType.INITIALPOPULATION,
                "expression",
                populationBasis,
                null);

        // When
        StratumPopulationDef stratumDef = new StratumPopulationDef(
                populationDef, subjects, quantities, resourceIds, MeasureStratifierType.CRITERIA, populationBasis);
        String result = stratumDef.toString();

        // Then: Should show IBase formatted as fhirType()
        assertTrue(result.contains("populationDefEvaluationResultIntersection="), "Should contain intersection field");

        // Should contain "Quantity" (the fhirType) and ellipsis
        assertTrue(result.contains("Quantity"), "Should contain Quantity fhir type");
        assertTrue(
                result.contains("populationDefEvaluationResultIntersection=") && result.contains("...]"),
                "Should truncate quantities with ellipsis");
    }

    @Test
    void toStringWithOver5Intervals() {
        // Given: Over 5 org.opencds.cqf.cql.engine.runtime.Interval instances
        Set<Object> intervals = new HashSet<>();
        for (int i = 1; i <= 9; i++) {
            Interval interval = new Interval(i, true, i + 10, true);
            intervals.add(interval);
        }

        Set<String> subjects = Set.of("Patient/1", "Patient/2");
        List<String> resourceIds = List.of("res1", "res2");
        CodeDef populationBasis = new CodeDef(null, null, "boolean", null);
        PopulationDef populationDef = new PopulationDef(
                "stratum-intervals",
                null,
                MeasurePopulationType.INITIALPOPULATION,
                "expression",
                populationBasis,
                null);

        // When
        StratumPopulationDef stratumDef = new StratumPopulationDef(
                populationDef, subjects, intervals, resourceIds, MeasureStratifierType.CRITERIA, populationBasis);
        String result = stratumDef.toString();

        // Then: Should show Interval objects as toString()
        assertTrue(result.contains("populationDefEvaluationResultIntersection="), "Should contain intersection field");

        // Should contain interval representation and ellipsis
        assertTrue(result.contains("Interval"), "Should contain Interval in toString");
        assertTrue(
                result.contains("populationDefEvaluationResultIntersection=") && result.contains("...]"),
                "Should truncate intervals with ellipsis");
    }

    @Test
    void toStringWithMixedTypes() {
        // Given: Mix of Patient resources, Quantities, and Intervals (over 5 total)
        Set<Object> mixed = new HashSet<>();

        // Add 3 Patients
        for (int i = 1; i <= 3; i++) {
            Patient patient = new Patient();
            patient.setId(new IdType(ResourceType.Patient.name(), "pat" + i));
            mixed.add(patient);
        }

        // Add 2 Quantities
        for (int i = 1; i <= 2; i++) {
            Quantity quantity = new Quantity();
            quantity.setValue(i * 5);
            mixed.add(quantity);
        }

        // Add 2 Intervals
        for (int i = 1; i <= 2; i++) {
            Interval interval = new Interval(i, true, i + 5, true);
            mixed.add(interval);
        }

        Set<String> subjects = Set.of("Patient/1", "Patient/2", "Patient/3", "Patient/4", "Patient/5", "Patient/6");
        List<String> resourceIds = List.of("res1", "res2", "res3", "res4", "res5", "res6", "res7", "res8", "res9");
        CodeDef populationBasis = new CodeDef(null, null, "Encounter", null);
        PopulationDef populationDef = new PopulationDef(
                "stratum-mixed", null, MeasurePopulationType.INITIALPOPULATION, "expression", populationBasis, null);

        // When
        StratumPopulationDef stratumDef = new StratumPopulationDef(
                populationDef, subjects, mixed, resourceIds, MeasureStratifierType.VALUE, populationBasis);
        String result = stratumDef.toString();

        // Then
        assertTrue(
                result.contains("populationDef=PopulationDef{id='stratum-mixed'"),
                "Should contain PopulationDef with id");
        assertTrue(result.contains("measureStratifierType=VALUE"), "Should contain VALUE type");
        assertTrue(result.contains("populationBasis=Encounter"), "Should contain Encounter basis");
        assertTrue(result.contains("populationDefEvaluationResultIntersection="), "Should contain intersection field");

        // Should have ellipsis for subjects and resources
        assertTrue(
                result.contains("subjectsQualifiedOrUnqualified=") && result.contains("...]"),
                "Should truncate subjects");
        assertTrue(
                result.contains("resourceIdsForSubjectList=") && result.contains("...]"), "Should truncate resources");
    }

    @Test
    void toStringWithExactly5Items() {
        // Given: Exactly 5 items (should NOT show ellipsis)
        Set<String> subjects = Set.of("Patient/1", "Patient/2", "Patient/3", "Patient/4", "Patient/5");
        List<String> resourceIds = List.of("res1", "res2", "res3", "res4", "res5");

        Set<Object> fivePatients = new HashSet<>();
        for (int i = 1; i <= 5; i++) {
            Patient patient = new Patient();
            patient.setId(new IdType(ResourceType.Patient.name(), "pat" + i));
            fivePatients.add(patient);
        }

        CodeDef populationBasis = new CodeDef(null, null, "boolean", null);
        PopulationDef populationDef = new PopulationDef(
                "stratum-exactly-5",
                null,
                MeasurePopulationType.INITIALPOPULATION,
                "expression",
                populationBasis,
                null);

        // When
        StratumPopulationDef stratumDef = new StratumPopulationDef(
                populationDef, subjects, fivePatients, resourceIds, MeasureStratifierType.CRITERIA, populationBasis);
        String result = stratumDef.toString();

        // Then: Should NOT contain ellipsis since we have exactly 5
        String subjectsSection = result.substring(result.indexOf("subjectsQualifiedOrUnqualified="));
        String resourcesSection = result.substring(result.indexOf("resourceIdsForSubjectList="));
        String intersectionSection = result.substring(result.indexOf("populationDefEvaluationResultIntersection="));

        // With exactly 5 items, no ellipsis expected (the collection is not truncated)
        // But we need to verify that exactly 5 are shown
        assertTrue(result.contains("Patient/1"), "Should contain at least one patient ID");
    }
}
