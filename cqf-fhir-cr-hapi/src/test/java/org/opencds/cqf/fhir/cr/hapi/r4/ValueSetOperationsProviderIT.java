package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.r4.valueset.ValueSetPackageProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class ValueSetOperationsProviderIT extends BaseCrR4TestServer {
    @Autowired
    ValueSetPackageProvider valueSetPackageProvider;

    @Test
    void testPackage() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireContent.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-GenerateQuestionnaireStructures.json");
        var requestDetails = setupRequestDetails();
        var result = valueSetPackageProvider.packageValueSet(
                "ValueSet/aslp-a1-de2", null, null, null, null, null, requestDetails);
        assertInstanceOf(Bundle.class, result);
    }
}
