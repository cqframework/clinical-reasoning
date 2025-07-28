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
            .firstStratum()
            .hasValue("arrived, planned");
    }

    @Test
    void singleLibraryTest_1B() {

        var when = GIVEN_REPO
            .when()
            .measureId("Level1B")
            .reportType("population")
            .evaluate();

        when.then()
            .hasMeasureReportCount(1)
            .getFirstMeasureReport()
            .firstGroup()
            .firstStratifier()
            .firstStratum()
            // LUKETODO:  why is this:  enc_arrived_patient3, enc_planned_patient3
            .hasValue("arrived, planned");
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
