package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

public class ValueSetOperationsProviderIT extends BaseCrR4TestServer {
    @Test
    void testPackage() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var result = ourClient
                .operation()
                .onInstance(new IdType("ValueSet", "aslp-a1-de2"))
                .named(ProviderConstants.CR_OPERATION_PACKAGE)
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();
        assertInstanceOf(Bundle.class, result);
    }
}
