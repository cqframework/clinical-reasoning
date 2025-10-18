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
                FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver);
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
                FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver);
        doReturn(null)
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), eq(null), any(), eq(null));
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
                FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver);
        doReturn(List.of(new StringType("Test")))
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), eq(null), any(), eq(null));
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
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), any(), eq(null), any(), eq(null));
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
}
