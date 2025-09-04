package org.opencds.cqf.fhir.cr.graphdefinition;

import static org.opencds.cqf.fhir.cr.graphdefinition.TestGraphDefinition.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.graphdefinition.TestGraphDefinition.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings("UnstableApiUsage")
class GraphDefinitionProcessorTest {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Test
    void testApply_returnsBundleResource2() {

        IgRepository repository = new IgRepository(
                fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4/eras"));

        var patientID = "Patient/time-zero";
        var graphDefinitionID = "eras-postop";

        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .graphDefinitionId(graphDefinitionID)
                .subjectId(patientID)
                .dataRepository(repository)
                .thenApply()
                .responseIsBundle();
    }
}
