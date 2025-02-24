package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryEvaluateProvider;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LibraryOperationsProviderTest extends BaseCrR4TestServer {
    @Autowired
    LibraryEvaluateProvider libraryEvaluateProvider;

    @Autowired
    LibraryDataRequirementsProvider libraryDataRequirementsProvider;

    @Test
    void testEvaluateLibrary() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-PatientData.json");

        var requestDetails = setupRequestDetails();
        var url = "http://example.org/sdh/dtr/aslp/Library/ASLPDataElements";
        var patientId = "positive";
        var parameters = new Parameters()
                .addParameter("Service Request Id", "SleepStudy")
                .addParameter("Service Request Id", "SleepStudy2");
        var result = libraryEvaluateProvider.evaluate(
                url, patientId, null, parameters, new BooleanType(true), null, null, null, null, null, requestDetails);

        assertNotNull(result);
        assertEquals(15, result.getParameter().size());
    }

    @Test
    void testDataRequirements() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var requestDetails = setupRequestDetails();
        var result = libraryDataRequirementsProvider.getDataRequirements(
                "Library/ASLPDataElements", null, null, null, requestDetails);
        assertInstanceOf(Library.class, result);
        assertEquals(
                "module-definition",
                ((Library) result).getType().getCodingFirstRep().getCode());
    }
}
