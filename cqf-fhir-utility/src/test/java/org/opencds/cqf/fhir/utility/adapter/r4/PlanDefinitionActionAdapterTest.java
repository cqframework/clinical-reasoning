package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.ActionConditionKind;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.r4.model.PlanDefinition.ActionSelectionBehavior;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.RequestPriority;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.Timing;
import org.hl7.fhir.r4.model.TriggerDefinition;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class PlanDefinitionActionAdapterTest {
    private final IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var action = new RequestGroupActionComponent();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createPlanDefinitionAction(action));
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
    void testTitleAndDescription() {
        var id = "test-action";
        var title = "test";
        var description = "test description";
        var action = new PlanDefinition.PlanDefinitionActionComponent()
                .setTitle(title)
                .setDescription(description);
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
        var action = new PlanDefinition.PlanDefinitionActionComponent().setTextEquivalent(text);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasTextEquivalent());
        assertEquals(text, adapter.getTextEquivalent());
    }

    @Test
    void testPriority() {
        var priority = "routine";
        var action = new PlanDefinition.PlanDefinitionActionComponent().setPriority(RequestPriority.fromCode(priority));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasPriority());
        assertEquals(priority, adapter.getPriority());
    }

    @Test
    void testCode() {
        var code = new CodeableConcept()
                .addCoding(new Coding().setSystem("test.com").setCode("test"));
        var action = new PlanDefinition.PlanDefinitionActionComponent().addCode(code);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasCode());
        assertEquals(code, adapter.getCode().get());
    }

    @Test
    void testDocumentation() {
        var documentation = new RelatedArtifact().setDisplay("test");
        var action = new PlanDefinition.PlanDefinitionActionComponent().addDocumentation(documentation);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasDocumentation());
        assertEquals(documentation, adapter.getDocumentation().get(0));
    }

    @Test
    void testTrigger() {
        var type = "named-event";
        var trigger = new TriggerDefinition().setType(TriggerType.fromCode(type));
        var action = new PlanDefinition.PlanDefinitionActionComponent().addTrigger(trigger);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasTrigger());
        assertEquals(trigger, adapter.getTrigger().get(0).get());
        assertEquals(type, adapter.getTriggerType().get(0));
    }

    @Test
    void testCondition() {
        var conditionExpression = "Test Expression";
        var action = new PlanDefinition.PlanDefinitionActionComponent()
                .addCondition(new PlanDefinitionActionConditionComponent()
                        .setKind(ActionConditionKind.APPLICABILITY)
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
        var action =
                new PlanDefinition.PlanDefinitionActionComponent().addInput(new DataRequirement().addProfile(profile));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasInput());
        assertEquals(
                profile,
                adapter.getInputDataRequirement().get(0).getProfile().get(0).getValueAsString());
    }

    @Test
    void testRelatedAction() {
        var relatedActionId = "related-action";
        var action = new PlanDefinition.PlanDefinitionActionComponent()
                .addRelatedAction(new PlanDefinitionActionRelatedActionComponent()
                        .setActionId(relatedActionId)
                        .setRelationship(ActionRelationshipType.BEFORE));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasRelatedAction());
        assertEquals(relatedActionId, adapter.getRelatedAction().get(0).getActionId());
    }

    @Test
    void testTiming() {
        var timing = new Timing().setCode(new CodeableConcept().addCoding(new Coding("system", "code", "display")));
        var action = new PlanDefinition.PlanDefinitionActionComponent().setTiming(timing);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasTiming());
        assertEquals(timing, adapter.getTiming());
    }

    @Test
    void testType() {
        var code = "test";
        var system = "test.com";
        var type = new CodeableConcept().addCoding(new Coding().setCode(code).setSystem(system));
        var action = new PlanDefinition.PlanDefinitionActionComponent().setType(type);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasType());
        assertEquals(type, adapter.getType().get());
        assertEquals(code, adapter.getType().getCoding().get(0).getCode());
    }

    @Test
    void testSelectionBehavior() {
        var selectionBehavior = "any";
        var action = new PlanDefinition.PlanDefinitionActionComponent()
                .setSelectionBehavior(ActionSelectionBehavior.fromCode(selectionBehavior));
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasSelectionBehavior());
        assertEquals(selectionBehavior, adapter.getSelectionBehavior());
    }

    @Test
    void testDefinition() {
        var definition = new CanonicalType("test");
        var action = new PlanDefinition.PlanDefinitionActionComponent().setDefinition(definition);
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasDefinition());
        assertEquals(definition, adapter.getDefinition());
    }

    @Test
    void testAction() {
        var childAction = new PlanDefinition.PlanDefinitionActionComponent();
        childAction.setId("child-action");
        var action = new PlanDefinition.PlanDefinitionActionComponent().addAction(childAction);
        action.setId("action");
        var adapter = new PlanDefinitionActionAdapter(action);
        assertTrue(adapter.hasAction());
        assertEquals(childAction, adapter.getAction().get(0).get());
        var newRequestAction = adapter.newRequestAction();
        assertInstanceOf(RequestGroupActionComponent.class, newRequestAction.get());
    }
}
