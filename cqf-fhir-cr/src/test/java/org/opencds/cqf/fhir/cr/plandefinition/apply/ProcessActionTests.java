package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import kotlin.Pair;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.PlanDefinition.ActionConditionKind;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent;
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
        var requestAction = fixture.generateRequestAction(actionAdapter, null);
        assertInstanceOf(org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent.class, requestAction.get());
    }

    @Test
    void r4Request() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var requestAction = fixture.generateRequestAction(actionAdapter, null);
        assertInstanceOf(RequestGroupActionComponent.class, requestAction.get());
    }

    @Test
    void r5Request() {
        var action = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent();
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R5, action);
        var requestAction = fixture.generateRequestAction(actionAdapter, null);
        assertInstanceOf(RequestOrchestrationActionComponent.class, requestAction.get());
    }

    @Test
    void testExceptionDuringItemGeneration() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        action.addInput(new org.hl7.fhir.r4.model.DataRequirement()
                .addProfile("http://fhir.org/test/StructureDefinition/test"));
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
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
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        doThrow(new IllegalArgumentException())
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), eq(null), eq(null), any(), eq(null));
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        fixture.evaluateConditions(request, actionAdapter);
        var oc = (org.hl7.fhir.r4.model.OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
        assertTrue(oc.getIssueFirstRep()
                .getDiagnosticsElement()
                .getValue()
                .contains("Condition expression '%s' encountered exception:".formatted(expression)));
    }

    @Test
    void testConditionResultNull() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var expression = new Expression().setLanguage("text/fhirpath").setExpression("null");
        action.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        doReturn(null)
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var results = fixture.evaluateConditions(request, actionAdapter);
        assertEquals(1, results.size());
        assertNull(results.get(0).getSecond());
        assertNull(request.getOperationOutcome());
    }

    @Test
    void testConditionResultNotBoolean() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var expression = new Expression().setLanguage("text/fhirpath").setExpression("null");
        action.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        doReturn(List.of(new StringType("Test")))
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var results = fixture.evaluateConditions(request, actionAdapter);
        assertTrue(results.stream().noneMatch(Pair::getSecond));
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
        var requestOrchestration = IAdapterFactory.createAdapterForResource(new RequestGroup());
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var requestAction = fixture.generateRequestAction(actionAdapter, null);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
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
        var requestOrchestration = IAdapterFactory.createAdapterForResource(new RequestGroup());
        var actionAdapter =
                (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(FhirVersionEnum.R4, action);
        var requestAction = fixture.generateRequestAction(actionAdapter, null);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        var metConditions = new ArrayList<String>();
        fixture.processChildActions(request, requestOrchestration, metConditions, actionAdapter, requestAction);
        assertFalse(requestAction.getAction().isEmpty());

        action.setExtension(null);
        action.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType("bad"));
        fixture.processChildActions(request, requestOrchestration, metConditions, actionAdapter, requestAction);
        assertFalse(requestAction.getAction().isEmpty());
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

    private ApplyRequest pdRequest() {
        return RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
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
                        case "nullList" -> null;
                        case "nullElement" -> java.util.Collections.singletonList(null);
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
    void missingExpressionIsNOTAnError() {
        var action = conditionAction("a");
        action.addCondition().setKind(ActionConditionKind.APPLICABILITY);
        var request = pdRequest();
        var results = fixture.evaluateConditions(request, adapt(action));
        assertEquals(1, results.size());
        assertNull(results.get(0).getSecond());
        assertNull(request.getOperationOutcome());
    }

    @Test
    void nullResultsRemainUnknownWithoutErrors() {
        literalResults();
        for (var expression : List.of("nullList", "nullElement")) {
            var request = pdRequest();
            var results = fixture.evaluateConditions(request, adapt(conditionAction("a", expression)));
            assertEquals(1, results.size());
            assertNull(results.get(0).getSecond());
            assertNull(request.getOperationOutcome());
        }
    }

    @Test
    void disabledPauseRetainsLegacyNullFiltering() {
        literalResults();
        var request = pdRequest();
        var results = fixture.evaluateConditions(request, adapt(conditionAction("a", "multipleWithNull")));
        assertTrue(results.stream().allMatch(Pair::getSecond));
        assertNull(request.getOperationOutcome());
    }

    @Test
    void conditionFailureStopsProcessing() {
        literalResults();
        var request = pdRequest();
        var results = fixture.evaluateConditions(request, adapt(conditionAction("a", "throws", "false", "nonBoolean")));
        assertTrue(results.stream().noneMatch(p -> Boolean.TRUE.equals(p.getSecond())));
        var outcome = (org.hl7.fhir.r4.model.OperationOutcome) request.getOperationOutcome();
        assertEquals(1, outcome.getIssue().size());
        assertEquals(
                "Condition expression 'throws' encountered exception: test evaluation failure",
                outcome.getIssue().get(0).getDiagnostics());
        org.mockito.Mockito.verify(libraryEngine, org.mockito.Mockito.times(3))
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
    }

    private List<String> visitGroup(String behavior, String expression) {
        var first = conditionAction("first", expression);
        first.addAction(conditionAction("descendant"));
        var group = conditionAction("group");
        group.addExtension(CQF_APPLICABILITY_BEHAVIOR, new StringType(behavior));
        group.setAction(List.of(first, conditionAction("fallback")));
        var request = pdRequest().setQuestionnaire(new org.hl7.fhir.r4.model.Questionnaire());
        var visited = new ArrayList<String>();
        org.mockito.Mockito.doAnswer(invocation -> {
                    visited.add(((IPlanDefinitionActionAdapter) invocation.getArgument(1)).getId());
                    return null;
                })
                .when(fixture)
                .addQuestionnaireItemForInput(eq(request), any());
        var result = (RequestGroupActionComponent) fixture.processAction(
                request, IAdapterFactory.createAdapterForResource(new RequestGroup()), new ArrayList<>(), adapt(group));
        if (behavior.equals("any") && expression.equals("null")) {
            assertEquals(1, result.getAction().size());
        }
        return visited;
    }

    @Test
    void anyStopsAtUnknownAndKeepsOnlyReachedQuestion() {
        literalResults();
        assertEquals(List.of("group", "first"), visitGroup("any", "null"));
        org.mockito.Mockito.verify(libraryEngine, org.mockito.Mockito.times(1))
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), any(), any(), eq(null));
    }

    @Test
    void falsePrunesDescendantsAndReachesSibling() {
        literalResults();
        assertEquals(List.of("group", "first", "fallback"), visitGroup("any", "false"));
    }

    @Test
    void truePrunesLaterSibling() {
        literalResults();
        assertEquals(List.of("group", "first", "descendant"), visitGroup("any", "true"));
    }

    @Test
    void allKeepsIndependentSibling() {
        literalResults();
        assertEquals(List.of("group", "first", "fallback"), visitGroup("all", "null"));
    }

    @Test
    void nestedAnyDoesNotEscapePausedSelectedBranch() {
        literalResults();
        var inner = conditionAction("selected");
        inner.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType("any"));
        inner.setAction(List.of(conditionAction("unknown", "null"), conditionAction("innerFallback")));
        var outer = conditionAction("outer");
        outer.addExtension(CQF_APPLICABILITY_BEHAVIOR, new CodeType("any"));
        outer.setAction(List.of(inner, conditionAction("outerFallback")));
        var result = fixture.generateRequestAction(adapt(outer), null);
        fixture.processChildActions(
                pdRequest(),
                IAdapterFactory.createAdapterForResource(new RequestGroup()),
                new ArrayList<>(),
                adapt(outer),
                result);
        assertEquals(1, result.getAction().size());
        assertEquals("selected", result.getAction().get(0).getId());
        assertFalse(result.getAction().get(0).getAction().isEmpty());
    }
}
