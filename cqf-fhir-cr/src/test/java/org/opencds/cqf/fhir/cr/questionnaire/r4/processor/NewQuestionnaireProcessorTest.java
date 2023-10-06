package org.opencds.cqf.fhir.cr.questionnaire.r4.processor;

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
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.StringType;
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
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.packager.PackageService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.populate.PopulateService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.PrePopulateService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.resolve.ResolveService;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;

@ExtendWith(MockitoExtension.class)
class NewQuestionnaireProcessorTest {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/questionnaire/r4/processor";

    @Mock
    private PopulateService myPopulateService;

    @Mock
    private ResolveService myResolveService;

    @Mock
    private PackageService myPackageService;

    @Mock
    private PrePopulateService myPrePopulateService;

    @Mock
    private FhirContext myFhirContext = FhirContext.forR4();

    @Mock
    private final Repository myRepository = new IGFileStructureRepository(myFhirContext, CLASS_PATH);

    @Spy
    @InjectMocks
    private NewQuestionnaireProcessor myFixture = NewQuestionnaireProcessor.of(myRepository);

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
        final Questionnaire actual = myFixture.resolveQuestionnaire(idType, canonical, questionnaire);
        // validate
        verify(myResolveService).resolve(idType, canonical, questionnaire);
        assertEquals(expected, actual);
    }

    @Test
    void prePopulateShouldReturnQuestionnaire() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final String patientId = "patientId";
        final IBaseParameters parameters = new Parameters();
        final Bundle bundle = new Bundle();
        final LibraryEngine libraryEngine = new LibraryEngine(myRepository, EvaluationSettings.getDefault());
        final Questionnaire expected = new Questionnaire();
        doReturn(expected).when(myPrePopulateService).prePopulate(any(PrePopulateRequest.class));
        // execute
        final IBaseResource actual = myFixture.prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        // validate
        assertEquals(expected, actual);
        verify(myPrePopulateService)
                .prePopulate(argThat(request -> Objects.equals(questionnaire, request.getQuestionnaire())
                        && Objects.equals(patientId, request.getPatientId())
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
        final LibraryEngine libraryEngine = new LibraryEngine(myRepository, EvaluationSettings.getDefault());
        final Questionnaire prePopulatedQuestionnaire = new Questionnaire();
        final QuestionnaireResponse expected = new QuestionnaireResponse();
        doReturn(prePopulatedQuestionnaire)
                .when(myFixture)
                .prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        doReturn(expected).when(myPopulateService).populate(questionnaire, prePopulatedQuestionnaire, patientId);
        // execute
        final IBaseResource actual = myFixture.populate(questionnaire, patientId, parameters, bundle, libraryEngine);
        // validate
        assertEquals(expected, actual);
        verify(myFixture).prePopulate(questionnaire, patientId, parameters, bundle, libraryEngine);
        verify(myPopulateService).populate(questionnaire, prePopulatedQuestionnaire, patientId);
    }

    @Test
    void generateQuestionnaireShouldReturnQuestionnaire() {
        // setup
        final String id = "questionnaire-id";
        // execute
        final Questionnaire actual = myFixture.generateQuestionnaire(id);
        // validate
        assertEquals("Questionnaire/questionnaire-id", actual.getId());
    }

    @Test
    void packageQuestionnaireShouldReturnBundle() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final Bundle expected = new Bundle();
        doReturn(expected).when(myPackageService).packageQuestionnaire(questionnaire, false);
        // execute
        final Bundle actual = myFixture.packageQuestionnaire(questionnaire, false);
        // validate
        assertEquals(expected, actual);
        verify(myPackageService).packageQuestionnaire(questionnaire, false);
    }
}
