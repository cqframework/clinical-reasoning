package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.common.HashSetForFhirResourcesAndCqlTypesTest.encounterInstance;

import java.util.List;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.Test;

/**
 * The wrapper-aware set keys on what it wraps, so a wrapper and the raw value inside it are the
 * same element. Before that, the wrapper carried no {@code equals}, so {@code add} deduplicated by
 * wrapper identity - not at all - while {@code contains} compared the wrapped values.
 */
class HashSetForCqlExpressionValuesTest {

    private static final String ENCOUNTER_ID = "encounter-1";
    private static final String EXPRESSION = "Qualifying Encounters";

    @Test
    void wrappersAroundTheSameResourceAreOneElement() {
        var set = new HashSetForCqlExpressionValues();

        assertTrue(set.add(wrap(encounterInstance(ENCOUNTER_ID))));
        assertFalse(set.add(wrap(encounterInstance(ENCOUNTER_ID))));
        assertEquals(1, set.size());
    }

    @Test
    void wrappersAroundDifferentResourcesAreDistinct() {
        var set = new HashSetForCqlExpressionValues();

        assertTrue(set.add(wrap(encounterInstance(ENCOUNTER_ID))));
        assertTrue(set.add(wrap(encounterInstance("encounter-2"))));
        assertEquals(2, set.size());
    }

    @Test
    void containsAcceptsARawResource() {
        var set = new HashSetForCqlExpressionValues();
        set.add(wrap(encounterInstance(ENCOUNTER_ID)));

        assertTrue(set.contains(encounterInstance(ENCOUNTER_ID)));
        assertFalse(set.contains(encounterInstance("encounter-2")));
    }

    @Test
    void containsAcceptsAHapiResourceForAnEngineNativeElement() {
        var set = new HashSetForCqlExpressionValues();
        set.add(wrap(encounterInstance(ENCOUNTER_ID)));

        var hapiEncounter = new Encounter();
        hapiEncounter.setId(ENCOUNTER_ID);

        assertTrue(set.contains(hapiEncounter));
    }

    @Test
    void removeAcceptsARawResource() {
        var set = new HashSetForCqlExpressionValues();
        set.add(wrap(encounterInstance(ENCOUNTER_ID)));
        set.add(wrap(encounterInstance("encounter-2")));

        assertTrue(set.remove(encounterInstance(ENCOUNTER_ID)));

        assertEquals(1, set.size());
        assertTrue(set.contains(encounterInstance("encounter-2")));
    }

    /**
     * {@code PopulationDef.retainAllResources} intersects one population's per-subject resources
     * against another's.
     */
    @Test
    void retainAllIntersectsByWrappedResource() {
        var set = new HashSetForCqlExpressionValues();
        set.add(wrap(encounterInstance(ENCOUNTER_ID)));
        set.add(wrap(encounterInstance("encounter-2")));

        set.retainAll(new HashSetForCqlExpressionValues(List.of(wrap(encounterInstance(ENCOUNTER_ID)))));

        assertEquals(1, set.size());
        assertTrue(set.contains(encounterInstance(ENCOUNTER_ID)));
    }

    @Test
    void retainAllAcceptsAPlainListOfRawResources() {
        var set = new HashSetForCqlExpressionValues();
        set.add(wrap(encounterInstance(ENCOUNTER_ID)));
        set.add(wrap(encounterInstance("encounter-2")));

        set.retainAll(List.of(encounterInstance(ENCOUNTER_ID)));

        assertEquals(1, set.size());
        assertTrue(set.contains(encounterInstance(ENCOUNTER_ID)));
    }

    @Test
    void addThenContainsAgreeForANonResourceValue() {
        var set = new HashSetForCqlExpressionValues();

        assertTrue(set.add(wrap("a plain string")));
        assertTrue(set.contains(wrap("a plain string")));
        assertTrue(set.contains("a plain string"));
        assertFalse(set.add(wrap("a plain string")));
        assertEquals(1, set.size());
    }

    private static CqlExpressionValue wrap(Object raw) {
        return CqlExpressionValue.ofRaw(EXPRESSION, raw, null);
    }
}
