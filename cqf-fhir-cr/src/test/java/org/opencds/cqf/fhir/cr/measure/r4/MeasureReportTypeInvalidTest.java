package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.When;

/**
 * This test is to verify and confirm that unsupported reportType value is appropriately handled
 *     invalid reportType value
 */
@SuppressWarnings("squid:S2699")
class MeasureReportTypeInvalidTest {
    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    @Test
    void invalidReportTypeValue() {
        final When evaluate = given.when()
                .measureId("ProportionResourceAllPopulations")
                .subject("Group/group-patients-1")
                .reportType("summary")
                .evaluate();

        try {
            evaluate.then();
            fail("'summary' is not a valid value and should fail");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("ReportType: summary, is not an accepted R4 EvalType value."));
        }
    }

    @Test
    void unsupportedReportTypeValue() {
        final When evaluate = given.when()
                .measureId("ProportionResourceAllPopulations")
                .subject("Group/group-patients-1")
                .reportType("patient-list")
                .evaluate();

        try {
            evaluate.then();
            fail("'patient-list' is not a valid value for R4 and should fail");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("ReportType: patient-list, is not an accepted R4 EvalType value."));
        }
    }
}
