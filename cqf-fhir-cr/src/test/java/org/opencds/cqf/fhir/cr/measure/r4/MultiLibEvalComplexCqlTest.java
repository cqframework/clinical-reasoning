package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

@SuppressWarnings({"java:S2699"})
class MultiLibEvalComplexCqlTest {
    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MultiLibEvalComplexCql");

    @Test
    void singleLibraryTest_1A() {

        var when = GIVEN_REPO
            .when()
            .measureId("Level1A")
            .reportType("population")
            .evaluate();

        when.then()
            .hasMeasureReportCount(1)
            .getFirstMeasureReport()
            .firstGroup()
            .firstStratifier()
            .stratum("Encounters A");
    }

    @Test
    void singleLibraryTest_1B() {

        var when = GIVEN_REPO
            .when()
            .measureId("Level1A")
            .reportType("population")
            .evaluate();

        when.then()
            .hasMeasureReportCount(1)
            .getFirstMeasureReport();
    }

    @Test
    void multipleLibraryTest_1A_and_1B() {

        var when = GIVEN_REPO
            .when()
            .measureId("Level1A")
            .measureId("Level1B")
            .reportType("population")
            .evaluate();

        when.then()
            .hasMeasureReportCount(2)
            .getFirstMeasureReport();
    }
}
