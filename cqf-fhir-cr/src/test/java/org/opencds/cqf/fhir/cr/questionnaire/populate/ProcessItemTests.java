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
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;

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
    void processItemShouldReturnQuestionnaireItemComponentDstu3() throws ResolveExpressionException {
        // setup
        final org.hl7.fhir.dstu3.model.Questionnaire questionnaire = new org.hl7.fhir.dstu3.model.Questionnaire();
        doReturn(FhirContext.forDstu3Cached()).when(repository).fhirContext();
        final PopulateRequest prePopulateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine, questionnaire);
        final IBaseBackboneElement originalQuestionnaireItemComponent =
                new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent();
        final IBaseBackboneElement populatedQuestionnaireItemComponent =
                new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent();
        final List<IBase> expressionResults = withExpressionResults(FhirVersionEnum.DSTU3);
        doReturn(populatedQuestionnaireItemComponent).when(fixture).copyItem(originalQuestionnaireItemComponent);
        doReturn(expressionResults)
                .when(fixture)
                .getExpressionResults(prePopulateRequest, originalQuestionnaireItemComponent);
        // execute
        final IBaseBackboneElement actual = fixture.processItem(prePopulateRequest, originalQuestionnaireItemComponent);
        // validate
        verify(fixture).getExpressionResults(prePopulateRequest, originalQuestionnaireItemComponent);
        verify(fixture).copyItem(originalQuestionnaireItemComponent);
        final var extensions = prePopulateRequest.getExtensionsByUrl(actual, Constants.QUESTIONNAIRE_RESPONSE_AUTHOR);
        assertEquals(1, extensions.size());
        final var initial = prePopulateRequest.resolvePath(actual, "initial");
        assertEquals(expressionResults.get(0), initial);
    }

    @Test
    void processItemShouldReturnQuestionnaireItemComponentR4() throws ResolveExpressionException {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final PopulateRequest prePopulateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final IBaseBackboneElement originalQuestionnaireItemComponent = new QuestionnaireItemComponent();
        final IBaseBackboneElement populatedQuestionnaireItemComponent = new QuestionnaireItemComponent();
        final List<IBase> expressionResults = withExpressionResults(FhirVersionEnum.R4);
        doReturn(populatedQuestionnaireItemComponent).when(fixture).copyItem(originalQuestionnaireItemComponent);
        doReturn(expressionResults)
                .when(fixture)
                .getExpressionResults(prePopulateRequest, originalQuestionnaireItemComponent);
        // execute
        final IBaseBackboneElement actual = fixture.processItem(prePopulateRequest, originalQuestionnaireItemComponent);
        // validate
        verify(fixture).getExpressionResults(prePopulateRequest, originalQuestionnaireItemComponent);
        verify(fixture).copyItem(originalQuestionnaireItemComponent);
        final var extensions = prePopulateRequest.getExtensionsByUrl(actual, Constants.QUESTIONNAIRE_RESPONSE_AUTHOR);
        assertEquals(1, extensions.size());
        final var initials = prePopulateRequest.resolvePathList(actual, "initial", IBaseBackboneElement.class);
        assertEquals(3, initials.size());
        for (int i = 0; i < initials.size(); i++) {
            assertEquals(expressionResults.get(i), prePopulateRequest.resolvePath(initials.get(i), "value"));
        }
    }

    @Test
    void processItemShouldReturnQuestionnaireItemComponentR5() throws ResolveExpressionException {
        // setup
        final org.hl7.fhir.r5.model.Questionnaire questionnaire = new org.hl7.fhir.r5.model.Questionnaire();
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        final PopulateRequest prePopulateRequest =
                newPopulateRequestForVersion(FhirVersionEnum.R5, libraryEngine, questionnaire);
        final IBaseBackboneElement originalQuestionnaireItemComponent =
                new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent();
        final IBaseBackboneElement populatedQuestionnaireItemComponent =
                new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent();
        final List<IBase> expressionResults = withExpressionResults(FhirVersionEnum.R5);
        doReturn(populatedQuestionnaireItemComponent).when(fixture).copyItem(originalQuestionnaireItemComponent);
        doReturn(expressionResults)
                .when(fixture)
                .getExpressionResults(prePopulateRequest, originalQuestionnaireItemComponent);
        // execute
        final IBaseBackboneElement actual = fixture.processItem(prePopulateRequest, originalQuestionnaireItemComponent);
        // validate
        verify(fixture).getExpressionResults(prePopulateRequest, originalQuestionnaireItemComponent);
        verify(fixture).copyItem(originalQuestionnaireItemComponent);
        final var extensions = prePopulateRequest.getExtensionsByUrl(actual, Constants.QUESTIONNAIRE_RESPONSE_AUTHOR);
        assertEquals(1, extensions.size());
        final var initials = prePopulateRequest.resolvePathList(actual, "initial", IBaseBackboneElement.class);
        assertEquals(3, initials.size());
        for (int i = 0; i < initials.size(); i++) {
            assertEquals(expressionResults.get(i), prePopulateRequest.resolvePath(initials.get(i), "value"));
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
    void getExpressionResultsShouldReturnEmptyListIfInitialExpressionIsNull() throws ResolveExpressionException {
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
        final List<IBase> actual = fixture.getExpressionResults(prePopulateRequest, questionnaireItemComponent);
        // validate
        assertTrue(actual.isEmpty());
        verify(expressionProcessorService).getItemInitialExpression(prePopulateRequest, questionnaireItemComponent);
        verify(expressionProcessorService, never()).getExpressionResultForItem(prePopulateRequest, null, "linkId");
    }

    @Test
    void getExpressionResultsShouldReturnListOfResourcesIfInitialExpressionIsNotNull()
            throws ResolveExpressionException {
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
        final List<IBase> actual = fixture.getExpressionResults(prePopulateRequest, questionnaireItemComponent);
        // validate
        assertEquals(expected, actual);
        verify(expressionProcessorService).getItemInitialExpression(prePopulateRequest, questionnaireItemComponent);
        verify(expressionProcessorService).getExpressionResultForItem(prePopulateRequest, expression, "linkId");
    }

    private CqfExpression withExpression() {
        return new CqfExpression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
    }
}
