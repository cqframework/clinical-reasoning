package org.opencds.cqf.fhir.cr.graphdefinition;

import static org.opencds.cqf.fhir.cr.graphdefinition.TestGraphDefinition.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.graphdefinition.TestGraphDefinition.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings("UnstableApiUsage")
class GraphDefinitionProcessorTest {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final IRepository r4Repository =
            new IgRepository(fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4/eras"));

    @Test
    void testApply_returnsBundleResource2() {
        var patientID = "Patient/time-zero";
        var practitionerID = "Practitioner/ordering-md-1";
        var graphDefinitionID = "eras-postop";

        given().repository(r4Repository)
                .when()
                .graphDefinitionId(graphDefinitionID)
                .subjectId(patientID)
                .practitionerId(practitionerID)
                .thenApply()
                .responseIsBundle();
    }

    @Test
    void testApply_Returns_Referenced_Resources() {
        var patientID = "Patient/time-zero";
        var practitionerID = "Practitioner/ordering-md-1";
        var graphDefinitionID = "hgb-pathway-event";

        given().repository(r4Repository)
                .when()
                .graphDefinitionId(graphDefinitionID)
                .subjectId(patientID)
                .practitionerId(practitionerID)
                .thenApply()
                .responseIsBundle()
                .responseBundleResourceCount(5);
    }
}
