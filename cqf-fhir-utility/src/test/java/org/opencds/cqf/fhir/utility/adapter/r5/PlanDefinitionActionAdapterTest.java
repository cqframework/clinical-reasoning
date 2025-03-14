package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent;
import org.hl7.fhir.r5.model.TriggerDefinition;
import org.hl7.fhir.r5.model.TriggerDefinition.TriggerType;
import org.junit.jupiter.api.Test;

class PlanDefinitionActionAdapterTest {

    @Test
    void invalid_object_fails() {
        var action = new RequestOrchestrationActionComponent();
        assertThrows(IllegalArgumentException.class, () -> new PlanDefinitionActionAdapter(action));
    }

    @Test
    void test() {
        var action = new PlanDefinitionActionComponent();
        var adapter = new PlanDefinitionActionAdapter(action);
        assertNotNull(adapter);
        assertEquals(action, adapter.get());
        assertEquals(FhirVersionEnum.R5, adapter.fhirContext().getVersion().getVersion());
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
