package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.Repository;
import java.util.Arrays;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.PlanDefinition.ActionConditionKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;

@ExtendWith(MockitoExtension.class)
class ProcessActionTests {
    @Mock
    Repository repository;

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
    void unsupportedVersionShouldReturnNull() {
        var action = new org.hl7.fhir.r4b.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.R4B, action);
        assertNull(requestAction);
    }

    @Test
    void dstu3Request() {
        var action = new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.DSTU3, action);
        assertTrue(requestAction instanceof org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void r4Request() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.R4, action);
        assertTrue(requestAction instanceof org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void r5Request() {
        var action = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.R5, action);
        assertTrue(
                requestAction
                        instanceof org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent);
    }

    @Test
    void testExceptionDuringItemGeneration() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        action.addInput(new org.hl7.fhir.r4.model.DataRequirement()
                .addProfile("http://fhir.org/test/StructureDefinition/test"));
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        fixture.addQuestionnaireItemForInput(request, action);
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
                .setExpression(new Expression().setLanguage("text/cql").setExpression(expression));
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        doThrow(new IllegalArgumentException())
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), eq(null), any(), eq(null));
        fixture.meetsConditions(request, action);
        var oc = (org.hl7.fhir.r4.model.OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
        assertTrue(oc.getIssueFirstRep()
                .getDiagnosticsElement()
                .getValue()
                .contains(String.format("Condition expression %s encountered exception:", expression)));
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
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), eq(null), any(), eq(null));
        var result = fixture.meetsConditions(request, action);
        assertFalse(result);
        assertNull(request.getOperationOutcome());
    }

    @Test
    void testConditionResultNotBoolean() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var expression = new Expression().setLanguage("text/fhirpath").setExpression("null");
        action.addCondition().setKind(ActionConditionKind.APPLICABILITY).setExpression(expression);
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        doReturn(Arrays.asList(new org.hl7.fhir.r4.model.StringType("Test")))
                .when(libraryEngine)
                .resolveExpression(eq(RequestHelpers.PATIENT_ID), any(), eq(null), eq(null), any(), eq(null));
        var result = fixture.meetsConditions(request, action);
        assertFalse(result);
        assertNull(request.getOperationOutcome());
    }
}
