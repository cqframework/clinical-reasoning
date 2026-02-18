package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_RELEASE;
import static org.opencds.cqf.fhir.utility.Parameters.newCodePart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;

public class ImplementationGuideOperationsProviderIT extends BaseCrR4TestServer {
    @Test
    void testRelease() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/uscore-package-bundle.json");
        loadResourceFromPath("org/opencds/cqf/fhir/cr/hapi/r4/igs/ImplementationGuide-hl7.fhir.us.core-6-1-0.json");
        var id = new IdType("ImplementationGuide", "hl7.fhir.us.core");
        var parameters = newParameters(
                getFhirContext(),
                newStringPart(getFhirContext(), "version", "1.0.0"),
                newCodePart(getFhirContext(), "versionBehavior", "default"));
        var result = ourClient
                .operation()
                .onInstance(id)
                .named(CRMI_OPERATION_RELEASE)
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);
    }
}
