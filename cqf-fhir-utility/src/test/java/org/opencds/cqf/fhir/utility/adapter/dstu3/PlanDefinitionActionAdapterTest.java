package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.dstu3.model.TriggerDefinition;
import org.hl7.fhir.dstu3.model.TriggerDefinition.TriggerType;
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
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testTrigger() {
        var type = "named-event";
        var trigger = new TriggerDefinition().setType(TriggerType.fromCode(type));
        var action = new PlanDefinitionActionComponent().addTriggerDefinition(trigger);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasTrigger());
        assertEquals(trigger, adapter.getTrigger().get(0).get());
        assertEquals(type, adapter.getTriggerType().get(0));
    }
}
