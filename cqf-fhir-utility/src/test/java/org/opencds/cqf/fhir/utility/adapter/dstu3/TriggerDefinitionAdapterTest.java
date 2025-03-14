package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.TriggerDefinition;
import org.hl7.fhir.dstu3.model.TriggerDefinition.TriggerType;
import org.junit.jupiter.api.Test;

class TriggerDefinitionAdapterTest {

    @Test
    void invalid_object_fails() {
        var coding = new Coding();
        assertThrows(IllegalArgumentException.class, () -> new TriggerDefinitionAdapter(coding));
    }

    @Test
    void test() {
        var triggerDef = new TriggerDefinition();
        var adapter = new TriggerDefinitionAdapter(triggerDef);
        assertNotNull(adapter);
        assertEquals(triggerDef, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testNameAndType() {
        var name = "test";
        var type = "named-event";
        var triggerDef = new TriggerDefinition().setEventName(name).setType(TriggerType.fromCode(type));
        var adapter = new TriggerDefinitionAdapter(triggerDef);
        assertTrue(adapter.hasName());
        assertEquals(name, adapter.getName());
        assertTrue(adapter.hasType());
        assertEquals(type, adapter.getType());
    }
}
