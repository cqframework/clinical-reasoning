package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup.ActionSelectionBehavior;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestPriority;
import org.junit.jupiter.api.Test;

class RequestActionAdapterTest {

    @Test
    void invalid_object_fails() {
        var action = new PlanDefinitionActionComponent();
        assertThrows(IllegalArgumentException.class, () -> new AdapterFactory().createRequestAction(action));
    }

    @Test
    void test() {
        var action = new RequestGroupActionComponent();
        var adapter = new RequestActionAdapter(action);
        assertNotNull(adapter);
        assertEquals(action, adapter.get());
        assertEquals(FhirVersionEnum.R4, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testTitleAndDescription() {
        var id = "test-action";
        var title = "test";
        var description = "test description";
        var action = new RequestGroupActionComponent().setTitle(title).setDescription(description);
        action.setId(id);
        var adapter = new RequestActionAdapter(action);
        assertEquals(id, action.getId());
        assertTrue(adapter.hasTitle());
        assertEquals(title, adapter.getTitle());
        assertTrue(adapter.hasDescription());
        assertEquals(description, adapter.getDescription());
    }

    @Test
    void testType() {
        var code = "test";
        var system = "test.com";
        var type = new CodeableConcept().addCoding(new Coding().setCode(code).setSystem(system));
        var action = new RequestGroupActionComponent().setType(type);
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasType());
        assertEquals(type, adapter.getType().get());
    }

    @Test
    void testPriority() {
        var priority = "routine";
        var action = new RequestGroupActionComponent().setPriority(RequestPriority.fromCode(priority));
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasPriority());
        assertEquals(priority, adapter.getPriority());
    }

    @Test
    void testDocumentation() {
        var documentation = new RelatedArtifact().setDisplay("test");
        var action = new RequestGroupActionComponent().addDocumentation(documentation);
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasDocumentation());
        assertEquals(documentation, adapter.getDocumentation().get(0));
    }

    @Test
    void testSelectionBehavior() {
        var selectionBehavior = "any";
        var action = new RequestGroupActionComponent()
                .setSelectionBehavior(ActionSelectionBehavior.fromCode(selectionBehavior));
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasSelectionBehavior());
        assertEquals(selectionBehavior, adapter.getSelectionBehavior());
    }

    @Test
    void testResource() {
        var reference = new Reference("test");
        var action = new RequestGroupActionComponent().setResource(reference);
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasResource());
        assertEquals(reference, adapter.getResource());
    }

    @Test
    void testAction() {
        var childAction = new RequestGroupActionComponent();
        childAction.setId("child-action");
        var action = new RequestGroupActionComponent().addAction(childAction);
        action.setId("action");
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasAction());
        assertEquals(childAction, adapter.getAction().get(0).get());
    }
}
