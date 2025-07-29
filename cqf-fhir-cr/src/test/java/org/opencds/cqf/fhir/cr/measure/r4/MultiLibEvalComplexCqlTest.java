package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"java:S2699"})
class MultiLibEvalComplexCqlTest {
    private static final Logger logger = LoggerFactory.getLogger(MultiLibEvalComplexCqlTest.class);

    private static final MeasureEvaluationOptions EVALUATION_OPTIONS = MeasureEvaluationOptions.defaultOptions()
            // We're not doing this not necessarily for HEDIS but just so we can assert different counts for numerator
            // and denominator
            .setApplyScoringSetMembership(false);

    private static final Given GIVEN_REPO =
            MultiMeasure.given().repositoryFor("MultiLibEvalComplexCql").evaluationOptions(EVALUATION_OPTIONS);

    @Test
    void singleLibraryTest_1A() {

        GIVEN_REPO
                .when()
                .measureId("Level1A")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureReportCount(1)
                .getFirstMeasureReport()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(7)
                .up()
                .population("numerator")
                .hasCount(3);
    }

    @Test
    void singleLibraryTest_1B() {

        GIVEN_REPO
                .when()
                .measureId("Level1B")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureReportCount(1)
                .getFirstMeasureReport()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(1);
    }

    @Test
    void multipleLibraryTest_1A_and_1B() {

        GIVEN_REPO
                .when()
                .measureId("Level1A")
                .measureId("Level1B")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureReportCount(2)
                .getFirstMeasureReport()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(7)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .up()
                .up()
                .getSecondMeasureReport()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(1);
    }
}
