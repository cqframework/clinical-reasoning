package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.When;

/**
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type that is not implemented or valid
 */
class MeasureScoringTypeCompositeTest {
    // req'd populations
    // exception works
    // exclusion works
    // has score
    // resource based
    // boolean based
    // group scoring def
    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    @Test
    void compositeBoolean() {
        final When evaluate =
                given.when().measureId("CompositeBooleanAllPopulations").evaluate();
        try {
            evaluate.then();
            fail("This is not a covered scoring Type and should fail");
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "Measure Scoring code: composite, is not a valid Measure Scoring Type for measure: http://example.com/Measure/CompositeBooleanAllPopulations."));
        }
    }
}
