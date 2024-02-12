package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;

public class ProcessRequestTests {
    ProcessRequest fixture = new ProcessRequest();

    @Test
    void unsupportedVersionShouldReturnNull() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4B);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertNull(requestOrchestration);
        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertNull(carePlan);
    }

    @Test
    void testDstu3Request() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertTrue(requestOrchestration instanceof org.hl7.fhir.dstu3.model.RequestGroup);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertTrue(carePlan instanceof org.hl7.fhir.dstu3.model.CarePlan);
    }

    @Test
    void testR4Request() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertTrue(requestOrchestration instanceof org.hl7.fhir.r4.model.RequestGroup);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertTrue(carePlan instanceof org.hl7.fhir.r4.model.CarePlan);
    }

    @Test
    void testR5Request() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertTrue(requestOrchestration instanceof org.hl7.fhir.r5.model.RequestOrchestration);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertTrue(carePlan instanceof org.hl7.fhir.r5.model.CarePlan);
    }
}
