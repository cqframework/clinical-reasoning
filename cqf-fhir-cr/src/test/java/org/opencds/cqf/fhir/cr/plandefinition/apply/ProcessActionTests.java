package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.opencds.cqf.fhir.utility.Constants.CQF_APPLICABILITY_BEHAVIOR;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.PlanDefinition.ActionConditionKind;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ProcessActionTests {
    @Mock
    IRepository repository;

    @Mock
    LibraryEngine libraryEngine;

    @Mock
    ModelResolver modelResolver;

    @Mock
    ApplyProcessor applyProcessor;

    @Mock
    GenerateProcessor generateProcessor;

    @Mock
    private IInputParameterResolver inputParameterResolver;

    @Spy
    @InjectMocks
    ProcessAction fixture;

    @Test
    void dstu3Request() {
        var action = new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent();
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.DSTU3, action);
        var requestAction = fixture.generateRequestAction(actionAdapter);
        assertInstanceOf(org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent.class, requestAction.get());
    }

    @Test
    void r4Request() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var requestAction = fixture.generateRequestAction(actionAdapter);
        assertInstanceOf(RequestGroupActionComponent.class, requestAction.get());
    }

    @Test
    void r5Request() {
        var action = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent();
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R5, action);
        var requestAction = fixture.generateRequestAction(actionAdapter);
        assertInstanceOf(RequestOrchestrationActionComponent.class, requestAction.get());
    }

    @Test
    void testExceptionDuringItemGeneration() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        action.addInput(new org.hl7.fhir.r4.model.DataRequirement()
                .addProfile("http://fhir.org/test/StructureDefinition/test"));
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver);
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        fixture.addQuestionnaireItemForInput(request, actionAdapter);
        var oc = (org.hl7.fhir.r4.model.OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
        assertTrue(oc.getIssueFirstRep()
                .getDiagnosticsElement()
                .getValue()
                .contains("An error occurred while generating Questionnaire items for action input:"));
    }

    @Test
    void testExceptionDuringMeetsCondition() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var expression = "test";
        action.addCondition()
                .setKind(ActionConditionKind.APPLICABILITY)
                .setExpression(
                        new Expression().setLanguage("text/cql-expression").setExpression(expression));
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver)
                .setPauseOnUnknownApplicability(false);
        doThrow(new IllegalArgumentException())
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), eq(null), eq(null), any(), eq(null));
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        fixture.meetsConditions(request, actionAdapter);
        var oc = (org.hl7.fhir.r4.model.OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
        assertTrue(oc.getIssueFirstRep()
                .getDiagnosticsElement()
                .getValue()
                .contains("Condition expression %s encountered exception:".formatted(expression)));
    }

    @Test
    void testConditionResultNull() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var expression = new Expression().setLanguage("text/fhirpath").setExpression("null");
        action.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver)
                .setPauseOnUnknownApplicability(false);
        doReturn(null)
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var result = fixture.meetsConditions(request, actionAdapter);
        Assertions.assertFalse(result);
        assertNull(request.getOperationOutcome());
    }

    @Test
    void testConditionResultNotBoolean() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var expression = new Expression().setLanguage("text/fhirpath").setExpression("null");
        action.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver)
                .setPauseOnUnknownApplicability(false);
        doReturn(List.of(new StringType("Test")))
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var result = fixture.meetsConditions(request, actionAdapter);
        Assertions.assertFalse(result);
        assertNull(request.getOperationOutcome());
    }

    org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent actionWithChildren() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        action.setId("action1");
        var expression = new Expression().setLanguage("text/cql-expression").setExpression("1 = 1");
        var childAction1 = new PlanDefinitionActionComponent();
        childAction1.setId("child1");
        childAction1.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        var childAction2 = new PlanDefinitionActionComponent();
        childAction2.setId("child2");
        childAction2.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        var childAction3 = new PlanDefinitionActionComponent();
        childAction3.setId("child3");
        childAction3.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        action.setAction(List.of(childAction1, childAction2, childAction3));
        return action;
    }

    @Test
    void testProcessChildActionsApplicabilityBehavior() {
        var action = actionWithChildren();
        var requestOrchestration = new RequestGroup();
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var requestAction = fixture.generateRequestAction(actionAdapter);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver);
        var metConditions = new ArrayList<String>();
        doReturn(List.of(new BooleanType(true)))
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
        fixture.processChildActions(request, requestOrchestration, metConditions, actionAdapter, requestAction);
        assertEquals(3, requestAction.getAction().size());

        requestAction.setAction(null);
        assertTrue(requestAction.getAction().isEmpty());
        metConditions = new ArrayList<String>();
        action.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType("any"));
        fixture.processChildActions(request, requestOrchestration, metConditions, actionAdapter, requestAction);
        assertEquals(1, requestAction.getAction().size());
    }

    @Test
    void testProcessChildActionsDoesNotThrowOnInvalidApplicabilityBehavior() {
        var action = actionWithChildren();
        action.addExtension(CQF_APPLICABILITY_BEHAVIOR, new BooleanType(true));
        var requestOrchestration = new RequestGroup();
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var requestAction = fixture.generateRequestAction(actionAdapter);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver);
        var metConditions = new ArrayList<String>();
        fixture.processChildActions(request, requestOrchestration, metConditions, actionAdapter, requestAction);
        assertTrue(requestAction.getAction().isEmpty());

        action.setExtension(null);
        action.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType("bad"));
        fixture.processChildActions(request, requestOrchestration, metConditions, actionAdapter, requestAction);
        assertTrue(requestAction.getAction().isEmpty());
    }

    private PlanDefinitionActionComponent conditionAction(String id, String... expressions) {
        var action = new PlanDefinitionActionComponent();
        action.setId(id);
        for (var expression : expressions) {
            action.addCondition()
                    .setKind(ActionConditionKind.APPLICABILITY)
                    .setExpression(
                            new Expression().setLanguage("text/cql-expression").setExpression(expression));
        }
        return action;
    }

    private ApplyRequest interactiveRequest(boolean enabled) {
        return RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver)
                .setPauseOnUnknownApplicability(enabled);
    }

    private IPlanDefinitionActionAdapter adapt(PlanDefinitionActionComponent action) {
        return (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
    }

    private void literalResults() {
        org.mockito.Mockito.doAnswer(invocation -> {
                    var expression = (org.opencds.cqf.fhir.utility.CqfExpression) invocation.getArgument(1);
                    return switch (expression.getExpression()) {
                        case "true" -> List.of(new BooleanType(true));
                        case "false" -> List.of(new BooleanType(false));
                        case "null" -> List.of();
                        case "emptyBoolean" -> List.of(new BooleanType());
                        case "multiple" -> List.of(new BooleanType(true), new BooleanType(false));
                        case "multipleWithNull" -> java.util.Arrays.asList(new BooleanType(true), new BooleanType());
                        case "nonBoolean" -> List.of(new StringType("invalid"));
                        case "throws" -> throw new IllegalArgumentException("test evaluation failure");
                        default -> throw new IllegalArgumentException("unexpected test expression");
                    };
                })
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
    }

    @Test
    void interactiveConjunctionPreservesFalseAndUnknown() {
        literalResults();
        for (var pair : List.of(List.of("false", "null"), List.of("null", "false"))) {
            var request = interactiveRequest(true);
            Assertions.assertFalse(
                    fixture.meetsConditions(request, adapt(conditionAction("a", pair.toArray(String[]::new)))));
            assertNull(request.getOperationOutcome());
        }
        assertNull(fixture.meetsConditions(interactiveRequest(true), adapt(conditionAction("a", "true", "null"))));
        assertNull(fixture.meetsConditions(interactiveRequest(true), adapt(conditionAction("a", "emptyBoolean"))));
        Assertions.assertTrue(fixture.meetsConditions(interactiveRequest(true), adapt(conditionAction("a", "true"))));
        Assertions.assertTrue(fixture.meetsConditions(interactiveRequest(true), adapt(conditionAction("a"))));
    }

    @Test
    void interactiveErrorsBlockEvenWhenAnotherConditionIsFalse() {
        literalResults();
        for (var invalid : List.of("throws", "nonBoolean", "multiple", "multipleWithNull")) {
            for (var pair : List.of(List.of("false", invalid), List.of(invalid, "false"))) {
                var request = interactiveRequest(true);
                assertNull(fixture.meetsConditions(request, adapt(conditionAction("a", pair.toArray(String[]::new)))));
                Assertions.assertNotNull(request.getOperationOutcome(), pair.toString());
            }
        }
    }

    @Test
    void interactiveMissingExpressionIsAnError() {
        var action = conditionAction("a");
        action.addCondition().setKind(ActionConditionKind.APPLICABILITY);
        var request = interactiveRequest(true);
        assertNull(fixture.meetsConditions(request, adapt(action)));
        Assertions.assertNotNull(request.getOperationOutcome());
    }

    private List<String> visitGroup(String behavior, boolean enabled, String expression) {
        var first = conditionAction("first", expression);
        first.addAction(conditionAction("descendant"));
        var group = conditionAction("group");
        group.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType(behavior));
        group.setAction(List.of(first, conditionAction("fallback")));
        var request = interactiveRequest(enabled).setQuestionnaire(new org.hl7.fhir.r4.model.Questionnaire());
        var visited = new ArrayList<String>();
        org.mockito.Mockito.doAnswer(invocation -> {
                    visited.add(((IPlanDefinitionActionAdapter) invocation.getArgument(1)).getId());
                    return null;
                })
                .when(fixture)
                .addQuestionnaireItemForInput(eq(request), any());
        var result = fixture.generateRequestAction(adapt(group));
        fixture.processChildActions(request, new RequestGroup(), new ArrayList<>(), adapt(group), result);
        if (enabled && behavior.equals("any") && expression.equals("null")) {
            assertTrue(result.getAction().isEmpty());
        }
        return visited;
    }

    @Test
    void interactiveAnyStopsAtUnknownAndKeepsOnlyReachedQuestion() {
        literalResults();
        assertEquals(List.of("first"), visitGroup("any", true, "null"));
        org.mockito.Mockito.verify(libraryEngine, org.mockito.Mockito.times(1))
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
    }

    @Test
    void disabledPauseAnyStillFallsThroughUnknown() {
        literalResults();
        assertEquals(List.of("first", "fallback"), visitGroup("any", false, "null"));
    }

    @Test
    void interactiveFalsePrunesDescendantsAndReachesSibling() {
        literalResults();
        assertEquals(List.of("first", "fallback"), visitGroup("any", true, "false"));
    }

    @Test
    void interactiveTruePrunesLaterSibling() {
        literalResults();
        assertEquals(List.of("first", "descendant"), visitGroup("any", true, "true"));
    }

    @Test
    void interactiveAllKeepsIndependentSibling() {
        literalResults();
        assertEquals(List.of("first", "fallback"), visitGroup("all", true, "null"));
    }

    @Test
    void interactiveNestedAnyDoesNotEscapePausedSelectedBranch() {
        literalResults();
        var inner = conditionAction("selected");
        inner.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType("any"));
        inner.setAction(List.of(conditionAction("unknown", "null"), conditionAction("innerFallback")));
        var outer = conditionAction("outer");
        outer.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType("any"));
        outer.setAction(List.of(inner, conditionAction("outerFallback")));
        var result = fixture.generateRequestAction(adapt(outer));
        fixture.processChildActions(
                interactiveRequest(true), new RequestGroup(), new ArrayList<>(), adapt(outer), result);
        assertEquals(1, result.getAction().size());
        assertEquals("selected", result.getAction().get(0).getId());
        assertTrue(result.getAction().get(0).getAction().isEmpty());
    }

    @Test
    void interactiveSettingIsRequestScopedAndCopiedToNestedPlan() {
        assertTrue(RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver)
                .isPauseOnUnknownApplicability());
        var request = interactiveRequest(false);
        Assertions.assertFalse(request.isPauseOnUnknownApplicability());
        request.setPauseOnUnknownApplicability(true);
        assertTrue(request.copy(request.getPlanDefinition()).isPauseOnUnknownApplicability());
        Assertions.assertFalse(interactiveRequest(false).isPauseOnUnknownApplicability());
        var settings = org.opencds.cqf.fhir.cr.CrSettings.getDefault();
        assertTrue(settings.isPauseOnUnknownApplicability());
        assertTrue(settings.withPauseOnUnknownApplicability(true).isPauseOnUnknownApplicability());
        settings.setPauseOnUnknownApplicability(false);
        Assertions.assertFalse(settings.isPauseOnUnknownApplicability());
    }
}
