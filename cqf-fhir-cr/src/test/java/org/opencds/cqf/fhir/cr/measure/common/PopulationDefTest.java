package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

class PopulationDefTest {

    @Test
    void setHandlingStrings() {
        final PopulationDef popDef1 = new PopulationDef(
                "one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));
        final PopulationDef popDef2 = new PopulationDef(
                "two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));

        popDef1.addResource("subj1", "string1");
        popDef2.addResource("subj1", "string1");

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains("string1"));
    }

    @Test
    void setHandlingIntegers() {
        final PopulationDef popDef1 = new PopulationDef(
                "one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));
        final PopulationDef popDef2 = new PopulationDef(
                "two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));

        popDef1.addResource("subj1", 123);
        popDef2.addResource("subj1", 123);

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains(123));
    }

    @Test
    void setHandlingEncounters() {
        final PopulationDef popDef1 = new PopulationDef(
                "one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));
        final PopulationDef popDef2 = new PopulationDef(
                "two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));

        final Encounter enc1a = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));
        final Encounter enc1b = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));

        popDef1.addResource("subj1", enc1a);
        popDef2.addResource("subj1", enc1b);

        popDef1.retainAllResources("subj1", popDef2);

        assertEquals(1, popDef1.getAllSubjectResources().size());

        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1a));
        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1b));
    }

    private Set<Object> getResourcesDistinctAcrossAllSubjects(PopulationDef popDef) {
        return new HashSetForFhirResourcesAndCqlTypes<>(popDef.getSubjectResources().values().stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()));
    }

    // Added by Claude Sonnet 4.5 - test different population basis types

    @Test
    void setHandlingStringsWithEncounterBasis() {
        final PopulationDef popDef1 = new PopulationDef(
                "one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter"));
        final PopulationDef popDef2 = new PopulationDef(
                "two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter"));

        popDef1.addResource("subj1", "string1");
        popDef2.addResource("subj1", "string1");

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains("string1"));
    }

    @Test
    void setHandlingStringsWithDateBasis() {
        final PopulationDef popDef1 =
                new PopulationDef("one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "date"));
        final PopulationDef popDef2 =
                new PopulationDef("two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "date"));

        popDef1.addResource("subj1", "string1");
        popDef2.addResource("subj1", "string1");

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains("string1"));
    }

    @Test
    void setHandlingIntegersWithEncounterBasis() {
        final PopulationDef popDef1 = new PopulationDef(
                "one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter"));
        final PopulationDef popDef2 = new PopulationDef(
                "two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter"));

        popDef1.addResource("subj1", 123);
        popDef2.addResource("subj1", 123);

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains(123));
    }

    @Test
    void setHandlingIntegersWithDateBasis() {
        final PopulationDef popDef1 =
                new PopulationDef("one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "date"));
        final PopulationDef popDef2 =
                new PopulationDef("two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "date"));

        popDef1.addResource("subj1", 123);
        popDef2.addResource("subj1", 123);

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains(123));
    }

    @Test
    void setHandlingEncountersWithEncounterBasis() {
        final PopulationDef popDef1 = new PopulationDef(
                "one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter"));
        final PopulationDef popDef2 = new PopulationDef(
                "two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter"));

        final Encounter enc1a = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));
        final Encounter enc1b = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));

        popDef1.addResource("subj1", enc1a);
        popDef2.addResource("subj1", enc1b);

        popDef1.retainAllResources("subj1", popDef2);

        assertEquals(1, popDef1.getAllSubjectResources().size());

        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1a));
        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1b));
    }

    @Test
    void setHandlingEncountersWithDateBasis() {
        final PopulationDef popDef1 =
                new PopulationDef("one", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "date"));
        final PopulationDef popDef2 =
                new PopulationDef("two", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "date"));

        final Encounter enc1a = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));
        final Encounter enc1b = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));

        popDef1.addResource("subj1", enc1a);
        popDef2.addResource("subj1", enc1b);

        popDef1.retainAllResources("subj1", popDef2);

        assertEquals(1, popDef1.getAllSubjectResources().size());

        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1a));
        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1b));
    }

    @Test
    void testIsBooleanBasisTrue() {
        final PopulationDef popDef = new PopulationDef(
                "test", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));

        assertTrue(popDef.isBooleanBasis());
    }

    @Test
    void testIsBooleanBasisFalseWithEncounter() {
        final PopulationDef popDef = new PopulationDef(
                "test", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter"));

        assertTrue(!popDef.isBooleanBasis());
    }

    @Test
    void testIsBooleanBasisFalseWithDate() {
        final PopulationDef popDef =
                new PopulationDef("test", null, null, null, new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "date"));

        assertTrue(!popDef.isBooleanBasis());
    }

    @Test
    void testGetCountForScoringWithBooleanBasis() {
        final PopulationDef popDef = new PopulationDef(
                "test",
                null,
                MeasurePopulationType.INITIALPOPULATION,
                null,
                new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));

        popDef.addResource("subj1", "resource1");
        popDef.addResource("subj2", "resource2");

        assertEquals(2, popDef.getCountForScoring());
    }
}
