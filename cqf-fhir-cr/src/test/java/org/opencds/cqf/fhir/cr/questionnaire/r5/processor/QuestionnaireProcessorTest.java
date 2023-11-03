package org.opencds.cqf.fhir.cr.questionnaire.r5.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ca.uhn.fhir.context.FhirContext;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.r5.processor.prepopulate.PrePopulateProcessor;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;

@ExtendWith(MockitoExtension.class)
class QuestionnaireProcessorTest {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/questionnaire/r5/processor";

    @Mock
    private PopulateProcessor populateProcessor;

    @Mock
    private ResolveProcessor resolveProcessor;

    @Mock
    private PackageProcessor packageProcessor;

    @Mock
    private PrePopulateProcessor prePopulateProcessor;

    @Mock
    private FhirContext myFhirContext = FhirContext.forR4();

    @Mock
    private EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

    @Mock
    private final Repository repository = new IGFileStructureRepository(myFhirContext, CLASS_PATH);

    @InjectMocks
    @Spy
    private QuestionnaireProcessor fixture = new QuestionnaireProcessor(repository);

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(evaluationSettings);
        verifyNoMoreInteractions(populateProcessor);
        verifyNoMoreInteractions(resolveProcessor);
        verifyNoMoreInteractions(packageProcessor);
        verifyNoMoreInteractions(prePopulateProcessor);
    }

    @Test
    void resolveQuestionnaireShouldReturnQuestionnaire() {
        // setup
        final IIdType idType = new IdType();
        final StringType canonical = new StringType("canonical");
        final Questionnaire questionnaire = new Questionnaire();
        final Questionnaire expected = new Questionnaire();
        doReturn(expected).when(resolveProcessor).resolve(idType, canonical, questionnaire);
        // execute
        final Questionnaire actual = fixture.resolveQuestionnaire(idType, canonical, questionnaire);
        // validate
        verify(resolveProcessor).resolve(idType, canonical, questionnaire);
        assertEquals(expected, actual);
    }

    @Test
    void prePopulateShouldReturnQuestionnaire() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final String patientId = "patientId";
        final IBaseParameters parameters = new Parameters();
        final Bundle bundle = new Bundle();
        final LibraryEngine libraryEngine = new LibraryEngine(repository, EvaluationSettings.getDefault());
        final Questionnaire expected = new Questionnaire();
        doReturn(expected)
                .when(prePopulateProcessor)
                .prePopulate(any(Questionnaire.class), any(PrePopulateRequest.class));
        // execute
        final Questionnaire actual = fixture.prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        // validate
        assertEquals(expected, actual);
        verify(prePopulateProcessor)
                .prePopulate(
                        argThat(q -> Objects.equals(questionnaire, q)),
                        argThat(request -> Objects.equals(patientId, request.getPatientId())
                                && Objects.equals(parameters, request.getParameters())
                                && Objects.equals(bundle, request.getBundle())
                                && Objects.equals(libraryEngine, request.getLibraryEngine())));
    }

    @Test
    void populateShouldReturnQuestionnaire() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final String patientId = "patientId";
        final IBaseParameters parameters = new Parameters();
        final Bundle bundle = new Bundle();
        final LibraryEngine libraryEngine = new LibraryEngine(repository, EvaluationSettings.getDefault());
        final Questionnaire prePopulatedQuestionnaire = new Questionnaire();
        final QuestionnaireResponse expected = new QuestionnaireResponse();
        doReturn(prePopulatedQuestionnaire)
                .when(fixture)
                .prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        doReturn(expected).when(populateProcessor).populate(questionnaire, prePopulatedQuestionnaire, patientId);
        // execute
        final IBaseResource actual = fixture.populate(questionnaire, patientId, parameters, bundle, libraryEngine);
        // validate
        assertEquals(expected, actual);
        verify(fixture).prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        verify(populateProcessor).populate(questionnaire, prePopulatedQuestionnaire, patientId);
    }

    @Test
    void generateQuestionnaireShouldReturnQuestionnaire() {
        // setup
        final String id = "questionnaire-id";
        // execute
        final Questionnaire actual = fixture.generateQuestionnaire(id);
        // validate
        assertEquals("Questionnaire/questionnaire-id", actual.getId());
        assertEquals("Questionnaire/questionnaire-id", actual.getId());
    }

    @Test
    void packageQuestionnaireShouldReturnBundle() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final Bundle expected = new Bundle();
        doReturn(expected).when(packageProcessor).packageQuestionnaire(questionnaire, false);
        // execute
        final Bundle actual = fixture.packageQuestionnaire(questionnaire, false);
        // validate
        assertEquals(expected, actual);
        verify(packageProcessor).packageQuestionnaire(questionnaire, false);
    }
}
