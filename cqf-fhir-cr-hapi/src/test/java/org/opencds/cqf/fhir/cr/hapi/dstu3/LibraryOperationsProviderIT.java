package org.opencds.cqf.fhir.cr.hapi.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newUriPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;

class LibraryOperationsProviderIT extends BaseCrDstu3TestServer {
    @Test
    void testEvaluateLibrary() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-view-bundle.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-data.json");

        var url = "http://fhir.org/guides/cdc/opioid-cds/Library/HelloWorld";
        var patientId = "Patient/helloworld-patient-1";
        var parameters = newParameters(
                getFhirContext(),
                newUriPart(getFhirContext(), "url", url),
                newPart(getFhirContext(), Reference.class, "subject", patientId));
        var result = ourClient
                .operation()
                .onType("Library")
                .named(ProviderConstants.CR_OPERATION_EVALUATE)
                .withParameters(parameters)
                .returnResourceType(Parameters.class)
                .execute();

        assertNotNull(result);
        assertEquals(8, result.getParameter().size());
    }
}
