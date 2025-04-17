package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.Repository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;

@ExtendWith(MockitoExtension.class)
class ProcessItemWithContextTests {
    @Mock
    private Repository repository;

    @Mock
    private ExpressionProcessor expressionProcessor;

    @Mock
    private LibraryEngine libraryEngine;

    private ProcessItemWithContext processItemWithContext;

    @BeforeEach
    void setup() {
        processItemWithContext = new ProcessItemWithContext(expressionProcessor);
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(expressionProcessor);
        verifyNoMoreInteractions(libraryEngine);
    }

    @Test
    void testMissingProfileLogsException() {
        var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var populateRequest = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var questionnaireItem = new QuestionnaireItemComponent().setLinkId("1").setDefinition("missing");
        var extensions = Arrays.asList(new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT));
        questionnaireItem.setExtension(extensions);
        var expression = new CqfExpression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
        List<IBase> expressionResults = Arrays.asList(new StringType("test"));
        doReturn(expression)
                .when(expressionProcessor)
                .getCqfExpression(populateRequest, extensions, Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        doReturn(expressionResults)
                .when(expressionProcessor)
                .getExpressionResultForItem(populateRequest, expression, "1");
        processItemWithContext.processContextItem(populateRequest, questionnaireItem);
        var operationOutcome = (OperationOutcome) populateRequest.getOperationOutcome();
        assertTrue(operationOutcome.hasIssue());
        assertEquals(2, operationOutcome.getIssue().size());
    }

    @Test
    void testNoContextStillReturnsResponseItem() {
        var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var populateRequest = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var questionnaireItem = new QuestionnaireItemComponent()
                .setLinkId("1")
                .setDefinition("http://hl7.org/fhir/Patient#Patient.name.given");
        var extensions = Arrays.asList(new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT));
        questionnaireItem.setExtension(extensions);
        var expression = new CqfExpression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
        List<IBase> expressionResults = new ArrayList<>();
        doReturn(expression)
                .when(expressionProcessor)
                .getCqfExpression(populateRequest, extensions, Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        doReturn(expressionResults)
                .when(expressionProcessor)
                .getExpressionResultForItem(populateRequest, expression, "1");
        var actual = processItemWithContext.processContextItem(populateRequest, questionnaireItem);
        assertEquals(1, actual.size());
        assertTrue(
                ((QuestionnaireResponseItemComponent) actual.get(0)).getAnswer().isEmpty());
    }
}
