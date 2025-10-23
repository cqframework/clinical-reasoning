package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.Enumerations.ActionConditionKind;
import org.hl7.fhir.r5.model.Enumerations.ActionRelationshipType;
import org.hl7.fhir.r5.model.Enumerations.ActionSelectionBehavior;
import org.hl7.fhir.r5.model.Enumerations.RequestPriority;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionInputComponent;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent;
import org.hl7.fhir.r5.model.Timing;
import org.hl7.fhir.r5.model.TriggerDefinition;
import org.hl7.fhir.r5.model.TriggerDefinition.TriggerType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class PlanDefinitionActionAdapterTest {
    private final IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var action = new RequestOrchestrationActionComponent();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createPlanDefinitionAction(action));
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
    void testTitleAndDescription() {
        var id = "test-action";
        var title = "test";
        var description = "test description";
        var action = new PlanDefinitionActionComponent().setTitle(title).setDescription(description);
        action.setId(id);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertEquals(id, adapter.getId());
        assertTrue(adapter.hasTitle());
        assertEquals(title, adapter.getTitle());
        assertTrue(adapter.hasDescription());
        assertEquals(description, adapter.getDescription());
    }

    @Test
    void testTextEquivalent() {
        var text = "test text";
        var action = new PlanDefinitionActionComponent().setTextEquivalent(text);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasTextEquivalent());
        assertEquals(text, adapter.getTextEquivalent());
    }

    @Test
    void testPriority() {
        var priority = "routine";
        var action = new PlanDefinitionActionComponent().setPriority(RequestPriority.fromCode(priority));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasPriority());
        assertEquals(priority, adapter.getPriority());
    }

    @Test
    void testCode() {
        var code = new CodeableConcept()
                .addCoding(new Coding().setSystem("test.com").setCode("test"));
        var action = new PlanDefinitionActionComponent().setCode(code);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasCode());
        assertEquals(code, adapter.getCode().get());
    }

    @Test
    void testDocumentation() {
        var documentation = new RelatedArtifact().setDisplay("test");
        var action = new PlanDefinitionActionComponent().addDocumentation(documentation);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasDocumentation());
        assertEquals(documentation, adapter.getDocumentation().get(0));
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

    @Test
    void testCondition() {
        var conditionExpression = "Test Expression";
        var action = new PlanDefinitionActionComponent()
                .addCondition(new PlanDefinitionActionConditionComponent(ActionConditionKind.APPLICABILITY)
                        .setExpression(new Expression()
                                .setLanguage("text/cql-identifier")
                                .setExpression(conditionExpression)));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasCondition());
        assertEquals(
                conditionExpression,
                adapter.getCondition().get(0).getExpression().getExpression());
    }

    @Test
    void testInput() {
        var profile = "StructureDefinition/test";
        var action = new PlanDefinitionActionComponent()
                .addInput(new PlanDefinitionActionInputComponent()
                        .setRequirement(new DataRequirement().addProfile(profile)));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasInput());
        assertEquals(
                profile,
                adapter.getInputDataRequirement().get(0).getProfile().get(0).getValueAsString());
    }

    @Test
    void testRelatedAction() {
        var relatedActionId = "related-action";
        var action = new PlanDefinitionActionComponent()
                .addRelatedAction(new PlanDefinitionActionRelatedActionComponent()
                        .setTargetId(relatedActionId)
                        .setRelationship(ActionRelationshipType.BEFORE));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasRelatedAction());
        assertEquals(relatedActionId, adapter.getRelatedAction().get(0).getTargetId());
    }

    @Test
    void testTiming() {
        var timing = new Timing().setCode(new CodeableConcept().addCoding(new Coding("system", "code", "display")));
        var action = new PlanDefinitionActionComponent().setTiming(timing);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasTiming());
        assertEquals(timing, adapter.getTiming());
    }

    @Test
    void testType() {
        var code = "test";
        var system = "test.com";
        var type = new CodeableConcept().addCoding(new Coding().setCode(code).setSystem(system));
        var action = new PlanDefinitionActionComponent().setType(type);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasType());
        assertEquals(type, adapter.getType().get());
        assertEquals(code, adapter.getType().getCoding().get(0).getCode());
    }

    @Test
    void testSelectionBehavior() {
        var selectionBehavior = "any";
        var action = new PlanDefinitionActionComponent()
                .setSelectionBehavior(ActionSelectionBehavior.fromCode(selectionBehavior));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasSelectionBehavior());
        assertEquals(selectionBehavior, adapter.getSelectionBehavior());
    }

    @Test
    void testDefinition() {
        var definition = new CanonicalType("test");
        var action = new PlanDefinitionActionComponent().setDefinition(definition);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasDefinition());
        assertEquals(definition, adapter.getDefinition());
    }

    @Test
    void testAction() {
        var childAction = new PlanDefinitionActionComponent();
        childAction.setId("child-action");
        var action = new PlanDefinitionActionComponent().addAction(childAction);
        action.setId("action");
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasAction());
        assertEquals(childAction, adapter.getAction().get(0).get());
        var newRequestAction = adapter.newRequestAction();
        assertInstanceOf(RequestOrchestrationActionComponent.class, newRequestAction.get());
    }
}
