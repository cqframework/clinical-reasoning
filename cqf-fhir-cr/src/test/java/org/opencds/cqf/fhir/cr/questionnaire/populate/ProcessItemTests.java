package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.CqfExpression;

@ExtendWith(MockitoExtension.class)
class ProcessItemTests {
    @Mock
    private Repository repository;

    @Mock
    private ExpressionProcessor expressionProcessorService;

    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    @InjectMocks
    private ProcessItem fixture;

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(expressionProcessorService);
        verifyNoMoreInteractions(libraryEngine);
    }

    @Test
    void processItemShouldReturnQuestionnaireResponseItemComponentDstu3() {
        // setup
        final org.hl7.fhir.dstu3.model.Questionnaire questionnaire = new org.hl7.fhir.dstu3.model.Questionnaire();
        doReturn(FhirContext.forDstu3Cached()).when(repository).fhirContext();
        final PopulateRequest populateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine, questionnaire);
        final IBaseBackboneElement originalQuestionnaireItemComponent =
                new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent();
        final List<IBase> expressionResults = withExpressionResults(FhirVersionEnum.DSTU3);
        doReturn(expressionResults).when(fixture).getInitialValue(populateRequest, originalQuestionnaireItemComponent);
        // execute
        final IBaseBackboneElement actual = fixture.processItem(populateRequest, originalQuestionnaireItemComponent);
        // validate
        verify(fixture).getInitialValue(populateRequest, originalQuestionnaireItemComponent);
        // final var extensions = populateRequest.getExtensionsByUrl(actual, Constants.QUESTIONNAIRE_RESPONSE_AUTHOR);
        // assertEquals(1, extensions.size());
        final var answers = populateRequest.resolvePathList(actual, "answer", IBaseBackboneElement.class);
        assertEquals(1, answers.size());
        for (int i = 0; i < answers.size(); i++) {
            assertEquals(expressionResults.get(i), populateRequest.resolvePath(answers.get(i), "value"));
        }
    }

    @Test
    void processItemShouldReturnQuestionnaireResponseItemComponentR4() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final PopulateRequest populateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final IBaseBackboneElement originalQuestionnaireItemComponent = new QuestionnaireItemComponent();
        final List<IBase> expressionResults = withExpressionResults(FhirVersionEnum.R4);
        doReturn(expressionResults).when(fixture).getInitialValue(populateRequest, originalQuestionnaireItemComponent);
        // execute
        final IBaseBackboneElement actual = fixture.processItem(populateRequest, originalQuestionnaireItemComponent);
        // validate
        verify(fixture).getInitialValue(populateRequest, originalQuestionnaireItemComponent);
        // final var extensions = populateRequest.getExtensionsByUrl(actual, Constants.QUESTIONNAIRE_RESPONSE_AUTHOR);
        // assertEquals(1, extensions.size());
        final var answers = populateRequest.resolvePathList(actual, "answer", IBaseBackboneElement.class);
        assertEquals(3, answers.size());
        for (int i = 0; i < answers.size(); i++) {
            assertEquals(expressionResults.get(i), populateRequest.resolvePath(answers.get(i), "value"));
        }
    }

    @Test
    void processItemShouldReturnQuestionnaireResponseItemComponentR5() {
        // setup
        final org.hl7.fhir.r5.model.Questionnaire questionnaire = new org.hl7.fhir.r5.model.Questionnaire();
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        final PopulateRequest populateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.R5, libraryEngine, questionnaire);
        final IBaseBackboneElement originalQuestionnaireItemComponent =
                new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent();
        final List<IBase> expressionResults = withExpressionResults(FhirVersionEnum.R5);
        doReturn(expressionResults).when(fixture).getInitialValue(populateRequest, originalQuestionnaireItemComponent);
        // execute
        final IBaseBackboneElement actual = fixture.processItem(populateRequest, originalQuestionnaireItemComponent);
        // validate
        verify(fixture).getInitialValue(populateRequest, originalQuestionnaireItemComponent);
        // final var extensions = populateRequest.getExtensionsByUrl(actual, Constants.QUESTIONNAIRE_RESPONSE_AUTHOR);
        // assertEquals(1, extensions.size());
        final var answers = populateRequest.resolvePathList(actual, "answer", IBaseBackboneElement.class);
        assertEquals(3, answers.size());
        for (int i = 0; i < answers.size(); i++) {
            assertEquals(expressionResults.get(i), populateRequest.resolvePath(answers.get(i), "value"));
        }
    }

    private List<IBase> withExpressionResults(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return List.of(new org.hl7.fhir.dstu3.model.StringType("string type value"));
            case R4:
                return List.of(
                        new org.hl7.fhir.r4.model.StringType("string type value"),
                        new org.hl7.fhir.r4.model.BooleanType(true),
                        new org.hl7.fhir.r4.model.IntegerType(3));
            case R5:
                return List.of(
                        new org.hl7.fhir.r5.model.StringType("string type value"),
                        new org.hl7.fhir.r5.model.BooleanType(true),
                        new org.hl7.fhir.r5.model.IntegerType(3));

            default:
                return null;
        }
    }

    @Test
    void getExpressionResultsShouldReturnEmptyListIfInitialExpressionIsNull() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final PopulateRequest prePopulateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        doReturn(null)
                .when(expressionProcessorService)
                .getItemInitialExpression(prePopulateRequest, questionnaireItemComponent);
        // execute
        final List<IBase> actual = fixture.getInitialValue(prePopulateRequest, questionnaireItemComponent);
        // validate
        assertTrue(actual.isEmpty());
        verify(expressionProcessorService).getItemInitialExpression(prePopulateRequest, questionnaireItemComponent);
        verify(expressionProcessorService, never()).getExpressionResultForItem(prePopulateRequest, null, "linkId");
    }

    @Test
    void getExpressionResultsShouldReturnListOfResourcesIfInitialExpressionIsNotNull() {
        // setup
        final List<IBase> expected = withExpressionResults(FhirVersionEnum.R4);
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final PopulateRequest prePopulateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        questionnaireItemComponent.setLinkId("linkId");
        final CqfExpression expression = withExpression();
        doReturn(expression)
                .when(expressionProcessorService)
                .getItemInitialExpression(prePopulateRequest, questionnaireItemComponent);
        doReturn(expected)
                .when(expressionProcessorService)
                .getExpressionResultForItem(prePopulateRequest, expression, "linkId");
        // execute
        final List<IBase> actual = fixture.getInitialValue(prePopulateRequest, questionnaireItemComponent);
        // validate
        assertEquals(expected, actual);
        verify(expressionProcessorService).getItemInitialExpression(prePopulateRequest, questionnaireItemComponent);
        verify(expressionProcessorService).getExpressionResultForItem(prePopulateRequest, expression, "linkId");
    }

    private CqfExpression withExpression() {
        return new CqfExpression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
    }
}
