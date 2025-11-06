package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

class PopulationDefTest {

    @Test
    void setHandlingStrings() {
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null);

        popDef1.addResource("subj1", "string1");
        popDef2.addResource("subj1", "string1");

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getResourcesList().size());
        assertTrue(popDef1.getResourcesList().contains("string1"));
    }

    @Test
    void setHandlingIntegers() {
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null);

        popDef1.addResource("subj1", 123);
        popDef2.addResource("subj1", 123);

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getResourcesList().size());
        assertTrue(popDef1.getResourcesList().contains(123));
    }

    @Test
    void setHandlingEncounters() {
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null);

        final Encounter enc1a = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));
        final Encounter enc1b = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));

        popDef1.addResource("subj1", enc1a);
        popDef2.addResource("subj1", enc1b);

        popDef1.retainAllResources("subj1", popDef2);

        assertEquals(1, popDef1.getResourcesList().size());

        // LUKETODO:  figure out how to redo these assertions
        assertTrue(popDef1.getResources().contains(enc1a));
        assertTrue(popDef1.getResources().contains(enc1b));
    }
}
