package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.r4.implementationguide.ImplementationGuideReleaseProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class ImplementationGuideOperationsProviderIT extends BaseCrR4TestServer {
    @Autowired
    ImplementationGuideReleaseProvider implementationGuideReleaseProvider;

    @Test
    void testRelease() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/uscore-package-bundle.json");
        loadResourceFromPath("org/opencds/cqf/fhir/cr/hapi/r4/igs/ImplementationGuide-hl7.fhir.us.core-6-1-0.json");
        var requestDetails = setupRequestDetails();
        var result = implementationGuideReleaseProvider.releaseImplementationGuide(
                "ImplementationGuide/hl7.fhir.us.core",
                "1.0.0",
                new CodeType("default"),
                null,
                null,
                null,
                null,
                requestDetails);
        assertInstanceOf(Bundle.class, result);
    }
}
