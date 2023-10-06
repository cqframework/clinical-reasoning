package org.opencds.cqf.fhir.cr.questionnaire.r4.processor;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.packager.PackageService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.populate.PopulateService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.PrePopulateService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.resolve.ResolveService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NewQuestionnaireProcessorTest {
    @Mock
    private PopulateService myPopulateService;
    @Mock
    private ResolveService myResolveService;
    @Mock
    private PackageService myPackageService;
    @Mock
    private PrePopulateService myPrePopulateService;
    @Mock
    private FhirContext myFhirContext;
    @Mock
    private Repository myRepository;
    @Mock
    private EvaluationSettings myEvaluationSettings;

    @InjectMocks
    private NewQuestionnaireProcessor myNewQuestionnaireProcessor;

    @BeforeEach
    void setup() {
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(myPopulateService);
        verifyNoMoreInteractions(myResolveService);
        verifyNoMoreInteractions(myPackageService);
        verifyNoMoreInteractions(myPrePopulateService);
    }

    @Test
    void resolveQuestionnaireShouldReturnQuestionnaire() {
        // setup
        final IIdType idType = new IdType();
        final StringType canonical = new StringType("canonical");
        final Questionnaire questionnaire = new Questionnaire();
        final Questionnaire expected = new Questionnaire();
        doReturn(expected).when(myResolveService).resolve(idType, canonical, questionnaire);
        // execute
        final Questionnaire actual = myNewQuestionnaireProcessor.resolveQuestionnaire(idType, canonical, questionnaire);
        // validate
        verify(myResolveService).resolve(idType, canonical, questionnaire);
        assertEquals(expected, actual);
    }

    @Test
    void prePopulateShouldReturnQuestionnaire() {
        // setup
        // execute
        // validate
    }

    @Test
    void populateShouldReturnQuestionnaire() {
        // setup
        // execute
        // validate
    }

    @Test
    void generateQuestionnaireShouldReturnQuestionnaire() {
        // setup
        // execute
        // validate
    }

    @Test
    void packageQuestionnaireShouldReturnBundle() {
        // setup
        // execute
        // validate
    }
}
