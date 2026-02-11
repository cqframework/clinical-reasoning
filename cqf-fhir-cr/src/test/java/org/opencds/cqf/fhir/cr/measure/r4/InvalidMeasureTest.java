package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

// This class has tests that verify failure behavior for various types of invalid measures.
// The goal is for the DSTU3 and R4 implementations to have the same behavior for these tests.
class InvalidMeasureTest {

    private static final Given GIVEN_INVALID_MEASURE_REPO = Measure.given().repositoryFor("InvalidMeasure");

    @Test
    void evaluateThrowsErrorWithEmptyMeasure() {
        var when = GIVEN_INVALID_MEASURE_REPO.when().measureId("Empty").evaluate();
        var e = assertThrows(InvalidRequestException.class, when::then);
        assertTrue(e.getMessage().contains("does not have a primary library"));
    }

    @Test
    void evaluateThrowsErrorWhenLibraryUnavailable() {
        var when = GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("LibraryUnavailable")
                .evaluate();
        assertThrows(ResourceNotFoundException.class, when::then);
    }

    @Test
    void evaluateThrowsErrorWhenLibraryIsMissingContent() {
        var when = GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("LibraryMissingContent")
                .evaluate();
        var e = assertThrows(IllegalStateException.class, when::then);
        assertTrue(e.getMessage().contains("Unable to load CQL/ELM for library"));
    }

    @Test
    void evaluateThrowsErrorWithDuplicatePopulationIds() {
        var when = GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("DuplicatePopulationIds")
                .evaluate();
        var e = assertThrows(InvalidRequestException.class, when::then);
        assertTrue(e.getMessage().contains("Duplicate population ID"));
        assertTrue(e.getMessage().contains("initial-population"));
        assertTrue(e.getMessage().contains("group-1"));
    }
}
