package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Enumerations.ActionConditionKind;
import org.hl7.fhir.r5.model.Enumerations.ActionRelationshipType;
import org.hl7.fhir.r5.model.Enumerations.ActionSelectionBehavior;
import org.hl7.fhir.r5.model.Enumerations.RequestPriority;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionConditionComponent;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionRelatedActionComponent;
import org.hl7.fhir.r5.model.Timing;
import org.junit.jupiter.api.Test;

class RequestActionAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var action = new PlanDefinitionActionComponent();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createRequestAction(action));
    }

    @Test
    void test() {
        var action = new RequestOrchestrationActionComponent();
        var adapter = adapterFactory.createRequestAction(action);
        assertNotNull(adapterFactory.createBase(action));
        assertNotNull(adapter);
        assertEquals(action, adapter.get());
        assertEquals(FhirVersionEnum.R5, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testTitleAndDescription() {
        var id = "test-action";
        var title = "test";
        var description = "test description";
        var action = new RequestOrchestrationActionComponent().setTitle(title).setDescription(description);
        action.setId(id);
        var adapter = new RequestActionAdapter(action);
        assertEquals(id, adapter.getId());
        var newId = "test-action2";
        adapter.setId(newId);
        assertEquals(newId, adapter.getId());
        assertTrue(adapter.hasTitle());
        assertEquals(title, adapter.getTitle());
        var newTitle = "test2";
        adapter.setTitle(newTitle);
        assertEquals(newTitle, adapter.getTitle());
        assertTrue(adapter.hasDescription());
        assertEquals(description, adapter.getDescription());
        var newDescription = "test description2";
        adapter.setDescription(newDescription);
        assertEquals(newDescription, adapter.getDescription());
    }

    @Test
    void testTextEquivalent() {
        var text = "test text";
        var action = new RequestOrchestrationActionComponent().setTextEquivalent(text);
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasTextEquivalent());
        assertEquals(text, adapter.getTextEquivalent());
        var newText = "new test text";
        adapter.setTextEquivalent(newText);
        assertEquals(newText, adapter.getTextEquivalent());
    }

    @Test
    void testPriority() {
        var priority = "routine";
        var action = new RequestOrchestrationActionComponent().setPriority(RequestPriority.fromCode(priority));
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasPriority());
        assertEquals(priority, adapter.getPriority());
        var newPriority = "urgent";
        adapter.setPriority(newPriority);
        assertEquals(newPriority, adapter.getPriority());
        assertEquals(RequestPriority.fromCode(newPriority), adapter.get().getPriority());
    }

    @Test
    void testDocumentation() {
        var documentation = new RelatedArtifact().setDisplay("test");
        var action = new RequestOrchestrationActionComponent().addDocumentation(documentation);
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasDocumentation());
        assertEquals(documentation, adapter.getDocumentation().get(0));
        var newDocumentation = new RelatedArtifact().setCitation("test");
        adapter.setDocumentation(List.of(newDocumentation));
        assertEquals(newDocumentation, adapter.getDocumentation().get(0));
    }

    @Test
    void testCondition() {
        var conditionExpression = "Test Expression";
        var action = new RequestOrchestrationActionComponent()
                .addCondition(new RequestOrchestrationActionConditionComponent(ActionConditionKind.APPLICABILITY)
                        .setExpression(new Expression()
                                .setLanguage("text/cql-identifier")
                                .setExpression(conditionExpression)));
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasCondition());
        assertEquals(
                conditionExpression,
                adapter.getCondition().get(0).getExpression().getExpression());
        var newConditionExpression = "New Test Expression";
        adapter.addCondition(new PlanDefinitionActionConditionComponent(ActionConditionKind.APPLICABILITY)
                .setExpression(
                        new Expression().setLanguage("text/cql-identifier").setExpression(newConditionExpression)));
        assertEquals(2, adapter.getCondition().size());
        assertEquals(
                newConditionExpression,
                adapter.getCondition().get(1).getExpression().getExpression());
    }

    @Test
    void testRelatedAction() {
        var relatedActionId = "related-action";
        var action = new RequestOrchestrationActionComponent()
                .addRelatedAction(new RequestOrchestrationActionRelatedActionComponent()
                        .setTargetId(relatedActionId)
                        .setRelationship(ActionRelationshipType.BEFORE));
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasRelatedAction());
        assertEquals(relatedActionId, adapter.getRelatedAction().get(0).getTargetId());
        var newRelatedActionId = "new-related-action";
        adapter.addRelatedAction(new PlanDefinitionActionRelatedActionComponent()
                .setTargetId(newRelatedActionId)
                .setRelationship(ActionRelationshipType.AFTER));
        assertEquals(2, adapter.getRelatedAction().size());
        assertEquals(newRelatedActionId, adapter.getRelatedAction().get(1).getTargetId());
    }

    @Test
    void testTiming() {
        var action = new RequestOrchestrationActionComponent();
        var adapter = adapterFactory.createRequestAction(action);
        assertFalse(adapter.hasTiming());
        var timing = new Timing().setCode(new CodeableConcept().addCoding(new Coding("system", "code", "display")));
        adapter.setTiming(timing);
        assertTrue(adapter.hasTiming());
        assertEquals(timing, adapter.getTiming());
    }

    @Test
    void testType() {
        var code = "test";
        var system = "test.com";
        var type = new CodeableConcept().addCoding(new Coding().setCode(code).setSystem(system));
        var action = new RequestOrchestrationActionComponent().setType(type);
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasType());
        assertEquals(type, adapter.getType().get());
        assertEquals(code, adapter.getType().getCoding().get(0).getCode());
        var newType = adapterFactory.createCodeableConcept(
                new CodeableConcept().addCoding(new Coding().setCode("code").setSystem(system)));
        adapter.setType(newType);
        assertEquals(newType.get(), adapter.getType().get());
        assertNotEquals(code, adapter.getType().getCoding().get(0).getCode());
    }

    @Test
    void testSelectionBehavior() {
        var selectionBehavior = "any";
        var action = new RequestOrchestrationActionComponent()
                .setSelectionBehavior(ActionSelectionBehavior.fromCode(selectionBehavior));
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasSelectionBehavior());
        assertEquals(selectionBehavior, adapter.getSelectionBehavior());
        var newSelectionBehavior = "all";
        adapter.setSelectionBehavior(newSelectionBehavior);
        assertEquals(newSelectionBehavior, adapter.getSelectionBehavior());
    }

    @Test
    void testResource() {
        var reference = new Reference("test");
        var action = new RequestOrchestrationActionComponent().setResource(reference);
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasResource());
        assertEquals(reference, adapter.getResource());
        var newReference = new Reference("new");
        adapter.setResource(newReference);
        assertEquals(newReference, adapter.getResource());
    }

    @Test
    void testAction() {
        var childAction = new RequestOrchestrationActionComponent();
        childAction.setId("child-action");
        var action = new RequestOrchestrationActionComponent().addAction(childAction);
        action.setId("action");
        var adapter = new RequestActionAdapter(action);
        assertTrue(adapter.hasAction());
        assertEquals(childAction, adapter.getAction().get(0).get());
        adapter.setAction(null);
        assertFalse(adapter.hasAction());
        assertTrue(adapter.getAction().isEmpty());
        adapter.addAction(childAction);
        assertTrue(adapter.hasAction());
        assertEquals(childAction, adapter.getAction().get(0).get());
    }
}
