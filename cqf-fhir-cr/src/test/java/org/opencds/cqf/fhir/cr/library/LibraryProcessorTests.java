package org.opencds.cqf.fhir.cr.library;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.cr.library.TestLibrary.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.library.TestLibrary.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.library.evaluate.EvaluateProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class LibraryProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Test
    void defaultSettings() {
        var repository =
                new IgRepository(fhirContextR4, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
        var processor = new LibraryProcessor(repository);
        assertNotNull(processor.evaluationSettings());
    }

    @Test
    void processor() {
        var repository =
                new IgRepository(fhirContextR5, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r5"));
        var evaluationSettings = EvaluationSettings.getDefault();
        var packageProcessor = new PackageProcessor(repository);
        var evaluationService = new EvaluateProcessor(repository, evaluationSettings);
        var processor = new LibraryProcessor(repository, evaluationSettings, packageProcessor, evaluationService);
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
    void evaluateDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .libraryId("HelloWorld")
                .subject("helloworld-patient-1")
                .useServerData(false)
                .thenEvaluate()
                .hasErrors(false)
                .hasExpression("Get Title");
    }

    @Test
    void evaluateR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .libraryId("HelloWorld")
                .subject("helloworld-patient-1")
                .useServerData(false)
                .thenEvaluate()
                .hasErrors(false)
                .hasExpression("Get Title");
    }

    @Test
    void evaluateR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .libraryId("HelloWorld")
                .subject("helloworld-patient-1")
                .useServerData(false)
                .thenEvaluate()
                .hasErrors(false)
                .hasExpression("Get Title");
    }
}
