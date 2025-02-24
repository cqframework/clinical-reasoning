package org.opencds.cqf.fhir.cr.hapi.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.opencds.cqf.fhir.cr.hapi.dstu3.library.LibraryEvaluateProvider;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LibraryOperationsProviderTest extends BaseCrDstu3TestServer {
    @Autowired
    LibraryEvaluateProvider libraryEvaluateProvider;

    @Test
    void testEvaluateLibrary() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-view-bundle.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-data.json");

        var requestDetails = setupRequestDetails();
        var url = "http://fhir.org/guides/cdc/opioid-cds/Library/HelloWorld";
        var patientId = "Patient/helloworld-patient-1";
        var result = libraryEvaluateProvider.evaluate(
                url, patientId, null, null, new BooleanType(false), null, null, null, null, null, requestDetails);

        assertNotNull(result);
        assertEquals(8, result.getParameter().size());
    }
}
