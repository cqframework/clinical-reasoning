package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.TriggerDefinition;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerType;
import org.junit.jupiter.api.Test;

class PlanDefinitionActionAdapterTest {

    @Test
    void invalid_object_fails() {
        var action = new RequestGroupActionComponent();
        assertThrows(IllegalArgumentException.class, () -> new PlanDefinitionActionAdapter(action));
    }

    @Test
    void test() {
        var action = new PlanDefinitionActionComponent();
        var adapter = new PlanDefinitionActionAdapter(action);
        assertNotNull(adapter);
        assertEquals(action, adapter.get());
        assertEquals(FhirVersionEnum.R4, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testTrigger() {
        var type = "named-event";
        var trigger = new TriggerDefinition().setType(TriggerType.fromCode(type));
        var action = new PlanDefinitionActionComponent().addTrigger(trigger);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasTrigger());
        assertEquals(trigger, adapter.getTrigger().get(0).get());
        assertEquals(type, adapter.getTriggerType().get(0));
    }
}
