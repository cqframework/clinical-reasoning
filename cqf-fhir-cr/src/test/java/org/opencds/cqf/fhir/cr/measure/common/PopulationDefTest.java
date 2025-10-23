package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

// LUKETODO:  once we get rid of the standalone resources, fix these tests
class PopulationDefTest {

    @Test
    void setHandlingStrings() {
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null);

        popDef1.addResource("string1");
        popDef2.addResource("string1");

        popDef1.getResources().retainAll(popDef2.getResources());
        assertEquals(1, popDef1.getResources().size());
        assertTrue(popDef1.getResources().contains("string1"));
    }

    @Test
    void setHandlingIntegers() {
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null);

        popDef1.addResource(123);
        popDef2.addResource(123);

        popDef1.getResources().retainAll(popDef2.getResources());
        assertEquals(1, popDef1.getResources().size());
        assertTrue(popDef1.getResources().contains(123));
    }

    @Test
    void setHandlingEncounters() {
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null);

        final Encounter enc1a = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));
        final Encounter enc1b = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));

        popDef1.addResource(enc1a);
        popDef2.addResource(enc1b);

        popDef1.getResources().retainAll(popDef2.getResources());

        assertEquals(1, popDef1.getResources().size());

        assertTrue(popDef1.getResources().contains(enc1a));
        assertTrue(popDef1.getResources().contains(enc1b));
    }
}
