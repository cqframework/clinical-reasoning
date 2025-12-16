package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.def.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.PopulationReportDef;

class PopulationReportDefTest {

    @Test
    void setHandlingStrings() {
        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        final PopulationReportDef popDef1 = new PopulationReportDef("one", null, null, null, stringBasis);
        final PopulationReportDef popDef2 = new PopulationReportDef("two", null, null, null, stringBasis);

        assertFalse(popDef1.isBooleanBasis());
        assertFalse(popDef2.isBooleanBasis());

        popDef1.addResource("subj1", "string1");
        popDef2.addResource("subj1", "string1");

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains("string1"));
    }

    @Test
    void setHandlingIntegers() {
        CodeDef integerBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "integer");
        final PopulationReportDef popDef1 = new PopulationReportDef("one", null, null, null, integerBasis);
        final PopulationReportDef popDef2 = new PopulationReportDef("two", null, null, null, integerBasis);

        assertFalse(popDef1.isBooleanBasis());
        assertFalse(popDef2.isBooleanBasis());

        popDef1.addResource("subj1", 123);
        popDef2.addResource("subj1", 123);

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains(123));
    }

    @Test
    void setHandlingEncounters() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        final PopulationReportDef popDef1 = new PopulationReportDef("one", null, null, null, encounterBasis);
        final PopulationReportDef popDef2 = new PopulationReportDef("two", null, null, null, encounterBasis);

        assertFalse(popDef1.isBooleanBasis());
        assertFalse(popDef2.isBooleanBasis());

        final Encounter enc1a = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));
        final Encounter enc1b = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));

        popDef1.addResource("subj1", enc1a);
        popDef2.addResource("subj1", enc1b);

        popDef1.retainAllResources("subj1", popDef2);

        assertEquals(1, popDef1.getAllSubjectResources().size());

        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1a));
        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1b));
    }

    private Set<Object> getResourcesDistinctAcrossAllSubjects(PopulationReportDef popDef) {
        return new HashSetForFhirResourcesAndCqlTypes<>(popDef.getSubjectResources().values().stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Test isBooleanBasis() returns true for boolean basis.
     */
    @Test
    void testIsBooleanBasis_WithBooleanBasis() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationReportDef popDef = new PopulationReportDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis);

        assertTrue(popDef.isBooleanBasis(), "Expected isBooleanBasis() to return true for boolean basis");
        assertEquals(booleanBasis, popDef.getPopulationBasis());
    }

    /**
     * Test isBooleanBasis() returns false for non-boolean basis types.
     */
    @Test
    void testIsBooleanBasis_WithNonBooleanBasis() {
        // Test various non-boolean basis types
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationReportDef encounterPop = new PopulationReportDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis);
        assertFalse(encounterPop.isBooleanBasis(), "Expected isBooleanBasis() to return false for Encounter basis");

        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        PopulationReportDef stringPop =
                new PopulationReportDef("pop-2", null, MeasurePopulationType.DENOMINATOR, "Denominator", stringBasis);
        assertFalse(stringPop.isBooleanBasis(), "Expected isBooleanBasis() to return false for String basis");

        CodeDef dateBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "date");
        PopulationReportDef datePop =
                new PopulationReportDef("pop-3", null, MeasurePopulationType.NUMERATOR, "Numerator", dateBasis);
        assertFalse(datePop.isBooleanBasis(), "Expected isBooleanBasis() to return false for date basis");
    }

    /**
     * Test getCount() with boolean basis - counts unique subjects.
     */
    @Test
    void testGetCount_BooleanBasis_CountsUniqueSubjects() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationReportDef popDef = new PopulationReportDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis);

        // Add 3 unique subjects
        popDef.addResource("Patient/1", true);
        popDef.addResource("Patient/2", true);
        popDef.addResource("Patient/3", true);

        assertTrue(popDef.isBooleanBasis());
        assertEquals(3, popDef.getCount(), "Boolean basis should count unique subjects");
        assertEquals(3, popDef.getSubjects().size());
    }

    /**
     * Test getCount() with non-boolean basis - counts all resources including duplicates across subjects.
     */
    @Test
    void testGetCount_EncounterBasis_CountsAllResources() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationReportDef popDef = new PopulationReportDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis);

        // Subject 1 has 2 encounters
        popDef.addResource("Patient/1", new Encounter().setId("Encounter/1"));
        popDef.addResource("Patient/1", new Encounter().setId("Encounter/2"));
        // Subject 2 has 3 encounters
        popDef.addResource("Patient/2", new Encounter().setId("Encounter/3"));
        popDef.addResource("Patient/2", new Encounter().setId("Encounter/4"));
        popDef.addResource("Patient/2", new Encounter().setId("Encounter/5"));

        assertFalse(popDef.isBooleanBasis());
        assertEquals(5, popDef.getCount(), "Encounter basis should count all resources");
        assertEquals(2, popDef.getSubjects().size(), "Should have 2 unique subjects");
    }

    /**
     * Test getCount() with String basis - counts all string resources.
     */
    @Test
    void testGetCount_StringBasis_CountsAllResources() {
        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        PopulationReportDef popDef =
                new PopulationReportDef("pop-1", null, MeasurePopulationType.NUMERATOR, "Numerator", stringBasis);

        // Add string values for different subjects
        // Even if the same string value appears for different subjects, count all
        popDef.addResource("Patient/1", "value1");
        popDef.addResource("Patient/2", "value2");
        popDef.addResource("Patient/3", "value1"); // Duplicate value but different subject

        assertFalse(popDef.isBooleanBasis());
        assertEquals(3, popDef.getCount(), "String basis should count all resources including duplicates");
        assertEquals(3, popDef.getSubjects().size());
    }

    /**
     * Test getCount() with date basis - counts all date resources.
     */
    @Test
    void testGetCount_DateBasis_CountsAllResources() {
        CodeDef dateBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "date");
        PopulationReportDef popDef =
                new PopulationReportDef("pop-1", null, MeasurePopulationType.DENOMINATOR, "Denominator", dateBasis);

        // Add date values for subjects
        popDef.addResource("Patient/1", "2024-01-01");
        popDef.addResource("Patient/2", "2024-01-02");
        popDef.addResource("Patient/3", "2024-01-01"); // Duplicate date value

        assertFalse(popDef.isBooleanBasis());
        assertEquals(3, popDef.getCount(), "Date basis should count all resources");
        assertEquals(3, popDef.getSubjects().size());
    }

    /**
     * Test getCount() for MEASUREOBSERVATION type - counts observations.
     */
    @Test
    void testGetCount_MeasureObservation_CountsObservations() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationReportDef popDef = new PopulationReportDef(
                "pop-obs", null, MeasurePopulationType.MEASUREOBSERVATION, "MeasureObservation", booleanBasis);

        // Add observations (Maps) for subjects
        // Each observation is a Map with key-value pairs
        Map<String, Object> obs1 = Map.of("value", 10.0);
        Map<String, Object> obs2 = Map.of("value", 20.0, "unit", "mg");
        Map<String, Object> obs3 = Map.of("value", 30.0);

        popDef.addResource("Patient/1", obs1);
        popDef.addResource("Patient/2", obs2);
        popDef.addResource("Patient/3", obs3);

        // For MEASUREOBSERVATION, getCount() should count observations (sum of map sizes)
        int expectedCount = obs1.size() + obs2.size() + obs3.size(); // 1 + 2 + 1 = 4
        assertEquals(expectedCount, popDef.getCount(), "MEASUREOBSERVATION should count observation entries");
    }
}
