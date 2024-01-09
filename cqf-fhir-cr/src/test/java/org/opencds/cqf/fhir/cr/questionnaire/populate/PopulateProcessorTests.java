package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.cr.questionnaire.helpers.PopulateRequestHelpers.PATIENT_ID;
import static org.opencds.cqf.fhir.cr.questionnaire.helpers.PopulateRequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;

@ExtendWith(MockitoExtension.class)
class PopulateProcessorTests {
    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    private final PopulateProcessor fixture = new PopulateProcessor();

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFieldsDstu3() {
        // setup
        final String questionnaireUrl = "original-questionnaire-url";
        final String prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final var originalQuestionnaire = new org.hl7.fhir.dstu3.model.Questionnaire();
        originalQuestionnaire.setId(prePopulatedQuestionnaireId);
        originalQuestionnaire.setUrl(questionnaireUrl);
        final PopulateRequest request =
                newPopulateRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine, originalQuestionnaire);
        final var expectedResponses = getExpectedResponses(request);
        final var expectedItems = getExpectedItems(request);
        doReturn(expectedItems).when(fixture).processItems(request, Collections.EMPTY_LIST);
        doReturn(expectedResponses).when(fixture).processResponseItems(request, expectedItems);
        // execute
        final IBaseResource actual = fixture.populate(request);
        // validate
        assertEquals(
                prePopulatedQuestionnaireId + "-" + PATIENT_ID,
                actual.getIdElement().getIdPart());
        assertContainedResources(request, actual, null, originalQuestionnaire);
        assertEquals(
                questionnaireUrl,
                request.resolvePath(actual, "questionnaire", IBaseReference.class)
                        .getReferenceElement()
                        .getValue());
        // assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, actual.getStatus());
        assertEquals(
                "Patient/" + PATIENT_ID,
                request.resolvePath(actual, "subject", IBaseReference.class)
                        .getReferenceElement()
                        .getValue());
        assertEquals(expectedResponses, request.getItems(actual));
        verify(fixture).processItems(request, Collections.EMPTY_LIST);
        verify(fixture).processResponseItems(request, expectedItems);
    }

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFieldsR4() {
        // setup
        final String questionnaireUrl = "original-questionnaire-url";
        final String prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final var originalQuestionnaire = new Questionnaire();
        originalQuestionnaire.setId(prePopulatedQuestionnaireId);
        originalQuestionnaire.setUrl(questionnaireUrl);
        final PopulateRequest request =
                newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, originalQuestionnaire);
        final var expectedResponses = getExpectedResponses(request);
        final var expectedItems = getExpectedItems(request);
        doReturn(expectedItems).when(fixture).processItems(request, Collections.EMPTY_LIST);
        doReturn(expectedResponses).when(fixture).processResponseItems(request, expectedItems);
        // execute
        final IBaseResource actual = fixture.populate(request);
        // validate
        assertEquals(
                prePopulatedQuestionnaireId + "-" + PATIENT_ID,
                actual.getIdElement().getIdPart());
        assertContainedResources(request, actual, null, originalQuestionnaire);
        assertEquals(questionnaireUrl, request.resolvePathString(actual, "questionnaire"));
        // assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, actual.getStatus());
        assertEquals(
                "Patient/" + PATIENT_ID,
                request.resolvePath(actual, "subject", IBaseReference.class)
                        .getReferenceElement()
                        .getValue());
        assertEquals(expectedResponses, request.getItems(actual));
        verify(fixture).processItems(request, Collections.EMPTY_LIST);
        verify(fixture).processResponseItems(request, expectedItems);
    }

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFieldsR5() {
        // setup
        final String questionnaireUrl = "original-questionnaire-url";
        final String prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final var originalQuestionnaire = new org.hl7.fhir.r5.model.Questionnaire();
        originalQuestionnaire.setId(prePopulatedQuestionnaireId);
        originalQuestionnaire.setUrl(questionnaireUrl);
        final PopulateRequest request =
                newPopulateRequestForVersion(FhirVersionEnum.R5, libraryEngine, originalQuestionnaire);
        final var expectedResponses = getExpectedResponses(request);
        final var expectedItems = getExpectedItems(request);
        doReturn(expectedItems).when(fixture).processItems(request, Collections.EMPTY_LIST);
        doReturn(expectedResponses).when(fixture).processResponseItems(request, expectedItems);
        // execute
        final IBaseResource actual = fixture.populate(request);
        // validate
        assertEquals(
                prePopulatedQuestionnaireId + "-" + PATIENT_ID,
                actual.getIdElement().getIdPart());
        assertContainedResources(request, actual, null, originalQuestionnaire);
        assertEquals(questionnaireUrl, request.resolvePathString(actual, "questionnaire"));
        // assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, actual.getStatus());
        assertEquals(
                "Patient/" + PATIENT_ID,
                request.resolvePath(actual, "subject", IBaseReference.class)
                        .getReferenceElement()
                        .getValue());
        assertEquals(expectedResponses, request.getItems(actual));
        verify(fixture).processItems(request, Collections.EMPTY_LIST);
        verify(fixture).processResponseItems(request, expectedItems);
    }

    private List<IBaseBackboneElement> getExpectedResponses(PopulateRequest request) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return List.of(
                        new org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                        new org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                        new org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent());
            case R4:
                return List.of(
                        new QuestionnaireResponseItemComponent(),
                        new QuestionnaireResponseItemComponent(),
                        new QuestionnaireResponseItemComponent());
            case R5:
                return List.of(
                        new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                        new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                        new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent());

            default:
                return null;
        }
    }

    private List<IBaseBackboneElement> getExpectedItems(PopulateRequest request) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return List.of(
                        new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent(),
                        new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent(),
                        new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent());
            case R4:
                return List.of(
                        new QuestionnaireItemComponent(),
                        new QuestionnaireItemComponent(),
                        new QuestionnaireItemComponent());
            case R5:
                return List.of(
                        new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent(),
                        new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent(),
                        new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent());

            default:
                return null;
        }
    }

    private void assertContainedResources(
            PopulateRequest request,
            IBaseResource actual,
            IBaseOperationOutcome expectedOperationOutcome,
            IBaseResource expectedQuestionnaire) {
        final var operationOutcome = getContainedByResourceType(request, actual, "OperationOutcome");
        assertEquals(expectedOperationOutcome, operationOutcome);
        final var questionnaire = getContainedByResourceType(request, actual, "Questionnaire");
        assertEquals(expectedQuestionnaire, questionnaire);
    }

    private IBaseResource getContainedByResourceType(
            PopulateRequest request, IBaseResource actual, String resourceType) {
        return request.getContained(actual).stream()
                .filter(c -> c.fhirType().equals(resourceType))
                .findFirst()
                .orElse(null);
    }

    @Test
    void resolveOperationOutcomeShouldAddOperationOutcomeIfHasIssues() {
        // setup
        final OperationOutcome operationOutcome = withOperationOutcomeWithIssue();
        final Questionnaire questionnaire = new Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        fixture.resolveOperationOutcome(request, questionnaire);
        // validate
        assertContainedResources(request, questionnaire, operationOutcome, null);
    }

    @Test
    void resolveOperationOutcomeShouldNotAddOperationOutcomeIfHasNoIssues() {
        // setup
        final OperationOutcome operationOutcome = new OperationOutcome();
        final Questionnaire questionnaire = new Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        fixture.resolveOperationOutcome(request, questionnaire);
        // validate
        assertContainedResources(request, questionnaire, null, null);
    }

    private OperationOutcome withOperationOutcomeWithIssue() {
        final OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome
                .addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION);
        return operationOutcome;
    }
}
