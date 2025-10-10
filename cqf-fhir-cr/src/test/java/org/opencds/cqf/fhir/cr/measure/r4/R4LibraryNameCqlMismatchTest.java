package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class R4LibraryNameCqlMismatchTest {
    private static final Given GIVEN_REPO = Measure.given().repositoryFor("LibraryUrlNameCqlMismatch");

    // The point of this test is to ensure that in the case where the library URL, either in the
    // FHIR Measure or the FHIR Library itself contains a name that doesn't match the
    // last part of the URL, we'll fail with a clear error message.
    //
    // Measure/library URL = "http://example.org/fhir/Library/SampleReporting-2024.2.0|2024.2.0"
    // Library name = "Sample_Reporting"
    // CQL library = "Sample_Reporting"
    @Test
    void ensureFailureWithLibraryUrlNameMismatch() {

        try {
            GIVEN_REPO
                    .when()
                    .measureId("SampleReporting-2024.2.0")
                    .evaluate()
                    .then()
                    .hasContainedOperationOutcome();
            fail("expected Exception");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "Library cannot be resolved because the name: Sample_Reporting does not match the version-less last part of the URL: http://sample.org/fhir/Library/SampleReporting-2024.2.0",
                    exception.getMessage());
        } catch (Exception exception) {
            fail("expected InvalidRequestException");
        }
    }
}
