package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.OperationOutcomes.addExceptionToOperationOutcome;
import static org.opencds.cqf.fhir.utility.OperationOutcomes.newOperationOutcome;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;

class OperationOutcomesTests {

    @Test
    void unsupportedVersionShouldReturnNull() {
        assertNull(newOperationOutcome(FhirVersionEnum.DSTU2));
    }

    @Test
    void dstu3() {
        var oc = newOperationOutcome(FhirVersionEnum.DSTU3);
        addExceptionToOperationOutcome(oc, "Test exception");
        var expected = (org.hl7.fhir.dstu3.model.OperationOutcome) oc;
        assertNotNull(expected);
        assertTrue(expected.hasIssue());
    }

    @Test
    void r4() {
        var oc = newOperationOutcome(FhirVersionEnum.R4);
        addExceptionToOperationOutcome(oc, "Test exception");
        var expected = (org.hl7.fhir.r4.model.OperationOutcome) oc;
        assertNotNull(expected);
        assertTrue(expected.hasIssue());
    }

    @Test
    void r5() {
        var oc = newOperationOutcome(FhirVersionEnum.R5);
        addExceptionToOperationOutcome(oc, "Test exception");
        var expected = (org.hl7.fhir.r5.model.OperationOutcome) oc;
        assertNotNull(expected);
        assertTrue(expected.hasIssue());
    }
}
