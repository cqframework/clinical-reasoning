package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.PATIENT_ID;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class PopulateProcessorTests {
    @Mock
    private IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    private final PopulateProcessor fixture = new PopulateProcessor();

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFieldsR4() {
        // setup
        final var fhirVersion = FhirVersionEnum.R4;
        final var questionnaireUrl = "original-questionnaire-url";
        final var prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final var originalQuestionnaire = new Questionnaire();
        originalQuestionnaire.setId(prePopulatedQuestionnaireId);
        originalQuestionnaire.setUrl(questionnaireUrl);
        final var item = new QuestionnaireItemComponent().setLinkId("1").setType(QuestionnaireItemType.DECIMAL);
        originalQuestionnaire.addItem(item);
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var request = newPopulateRequestForVersion(fhirVersion, libraryEngine, originalQuestionnaire);
        final var expectedResponses = getExpectedResponses(request);
        doReturn(expectedResponses).when(fixture).populateItem(eq(request), any());
        // execute
        final var actual = (QuestionnaireResponse) fixture.populate(request);
        // validate
        assertEquals(
                prePopulatedQuestionnaireId + "-" + PATIENT_ID,
                actual.getIdElement().getIdPart());
        assertContainedOperationOutcome(request, actual, null);
        assertEquals(questionnaireUrl, actual.getQuestionnaire());
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, actual.getStatus());
        assertEquals("Patient/" + PATIENT_ID, actual.getSubject().getReference());
        assertEquals(expectedResponses.stream().map(IAdapter::get).toList(), actual.getItem());
        verify(fixture).populateItem(eq(request), any());
    }

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFieldsR5() {
        // setup
        final var fhirVersion = FhirVersionEnum.R5;
        final var questionnaireUrl = "original-questionnaire-url";
        final var prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final var originalQuestionnaire = new org.hl7.fhir.r5.model.Questionnaire();
        originalQuestionnaire.setId(prePopulatedQuestionnaireId);
        originalQuestionnaire.setUrl(questionnaireUrl);
        final var item = new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent()
                .setLinkId("1")
                .setType(org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.BOOLEAN);
        originalQuestionnaire.addItem(item);
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        final var request = newPopulateRequestForVersion(fhirVersion, libraryEngine, originalQuestionnaire);
        final var expectedResponses = getExpectedResponses(request);
        doReturn(expectedResponses).when(fixture).populateItem(eq(request), any());
        // execute
        final var actual = (org.hl7.fhir.r5.model.QuestionnaireResponse) fixture.populate(request);
        // validate
        assertEquals(
                prePopulatedQuestionnaireId + "-" + PATIENT_ID,
                actual.getIdElement().getIdPart());
        assertContainedOperationOutcome(request, actual, null);
        assertEquals(questionnaireUrl, request.resolvePathString(actual, "questionnaire"));
        assertEquals(
                org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, actual.getStatus());
        assertEquals("Patient/" + PATIENT_ID, actual.getSubject().getReference());
        assertEquals(expectedResponses.stream().map(IAdapter::get).toList(), actual.getItem());
        verify(fixture).populateItem(eq(request), any());
    }

    private List<IQuestionnaireResponseItemComponentAdapter> getExpectedResponses(PopulateRequest request) {
        return switch (request.getFhirVersion()) {
            case R4 ->
                Stream.of(
                                new QuestionnaireResponseItemComponent(),
                                new QuestionnaireResponseItemComponent(),
                                new QuestionnaireResponseItemComponent())
                        .map(i -> request.getAdapterFactory().createQuestionnaireResponseItem(i))
                        .toList();
            case R5 ->
                Stream.of(
                                new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                                new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                                new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent())
                        .map(i -> request.getAdapterFactory().createQuestionnaireResponseItem(i))
                        .toList();
            default -> List.of();
        };
    }

    private void assertContainedOperationOutcome(
            PopulateRequest request, IBaseResource actual, IBaseOperationOutcome expectedOperationOutcome) {
        final var operationOutcome = getContainedByResourceType(request, actual, "OperationOutcome");
        assertEquals(expectedOperationOutcome, operationOutcome);
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
        final var operationOutcome = withOperationOutcomeWithIssue();
        final var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        request.resolveOperationOutcome(questionnaire);
        // validate
        assertContainedOperationOutcome(request, questionnaire, operationOutcome);
    }

    @Test
    void resolveOperationOutcomeShouldNotAddOperationOutcomeIfHasNoIssues() {
        // setup
        final var operationOutcome = new OperationOutcome();
        final var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        request.resolveOperationOutcome(questionnaire);
        // validate
        assertContainedOperationOutcome(request, questionnaire, null);
    }

    private OperationOutcome withOperationOutcomeWithIssue() {
        final var operationOutcome = new OperationOutcome();
        operationOutcome
                .addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION);
        return operationOutcome;
    }

    @Test
    void testGetVariablesReturnsObject() {
        var questionnaire = new Questionnaire();
        var expression = new Expression()
                .setLanguage("text/cql-expression")
                .setExpression("test")
                .setName("testName");
        questionnaire.addExtension(Constants.VARIABLE_EXTENSION, expression);
        var expectedResponse = new StringType("test");
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        doReturn(List.of(expectedResponse))
                .when(libraryEngine)
                .resolveExpression(eq(PATIENT_ID), any(), any(), any(), any(), any(), any());
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var actual = fixture.getVariables(request, questionnaire);
        assertNotNull(actual);
        assertEquals(expectedResponse, actual.get("testName"));
    }

    @Test
    void testGetVariablesReturnsList() {
        var questionnaire = new Questionnaire();
        var expression = new Expression()
                .setLanguage("text/cql-expression")
                .setExpression("test")
                .setName("testName");
        questionnaire.addExtension(Constants.VARIABLE_EXTENSION, expression);
        var expectedResponse = List.of(new StringType("test1"), new StringType("test2"));
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        doReturn(expectedResponse)
                .when(libraryEngine)
                .resolveExpression(eq(PATIENT_ID), any(), any(), any(), any(), any(), any());
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var actual = fixture.getVariables(request, questionnaire);
        assertNotNull(actual);
        assertEquals(expectedResponse, actual.get("testName"));
    }
}
