package org.opencds.cqf.fhir.cr.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.cr.library.TestLibrary.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.library.TestLibrary.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@ExtendWith(MockitoExtension.class)
class LibraryProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Mock
    LibraryEngine libraryEngine;

    @Test
    void defaultSettings() {
        var repository =
                new IgRepository(fhirContextR4, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
        var processor = new LibraryProcessor(repository);
        assertNotNull(processor.evaluationSettings());
    }

    @Test
    void testRequest() {
        var library = new Library();
        var request = RequestHelpers.newEvaluateRequestForVersion(FhirVersionEnum.R4, libraryEngine, library);
        assertEquals("evaluate", request.getOperationName());
        assertEquals("patientId", request.getSubjectId().getIdPart());
        assertNull(request.getContext());
    }

    @Test
    void processor() {
        var repository =
                new IgRepository(fhirContextR5, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r5"));
        var packageProcessor = new PackageProcessor(repository);
        var dataRequirementsProcessor = new DataRequirementsProcessor(repository);
        var evaluateProcessor = new EvaluateProcessor(repository, EvaluationSettings.getDefault());
        var processor = new LibraryProcessor(
                repository,
                EvaluationSettings.getDefault(),
                packageProcessor,
                dataRequirementsProcessor,
                evaluateProcessor);
        assertNotNull(processor.evaluationSettings());
        var result = processor.resolveLibrary(Eithers.forMiddle3(
                Ids.newId(repository.fhirContext(), "Library", "OutpatientPriorAuthorizationPrepopulation")));
        assertNotNull(result);
    }

    @Test
    void packageDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .thenPackage()
                .hasEntry(2);
    }

    @Test
    void packageR4() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .libraryId("ASLPDataElements")
                .thenPackage()
                .hasEntry(10);

        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .isPut(Boolean.TRUE)
                .thenPackage()
                .hasEntry(2);
    }

    @Test
    void packageR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .isPut(Boolean.TRUE)
                .thenPackage()
                .hasEntry(2);
    }

    @Test
    void dataRequirementsDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .thenDataRequirements()
                .hasDataRequirements(29);
    }

    @Test
    void dataRequirementsR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .thenDataRequirements()
                .hasDataRequirements(30);
    }

    @Test
    void dataRequirementsR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .thenDataRequirements()
                .hasDataRequirements(30);
    }

    @Test
    void evaluateException() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .libraryId("BadLibrary")
                .subjectId("OPA-Patient1")
                .thenEvaluate()
                .hasOperationOutcome();
    }

    @Test
    void evaluateDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .subjectId("OPA-Patient1")
                .thenEvaluate()
                .hasResults(48);
    }

    @Test
    void evaluateR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .subjectId("OPA-Patient1")
                .thenEvaluate()
                .hasResults(51);
    }

    @Test
    void evaluateR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .libraryId("OutpatientPriorAuthorizationPrepopulation")
                .subjectId("OPA-Patient1")
                .thenEvaluate()
                .hasResults(49);
    }

    @Test
    void testPrefetchData() {
        var patientID = "patient-CdsHooksMultipleActions";
        var data = "r4/cds-hooks-multiple-actions/cds_hooks_multiple_actions_patient_data.json";
        var content = "r4/cds-hooks-multiple-actions/cds_hooks_multiple_actions_plan_definition.json";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .libraryUrl("http://example.com/Library/CdsHooksMultipleActions")
                .subjectId(patientID)
                .prefetchData("patient", data)
                .content(content)
                .terminology(content)
                .thenEvaluate()
                .hasResults(6);
    }
}
