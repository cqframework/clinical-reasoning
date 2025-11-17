package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type Cohort
 */
@SuppressWarnings("squid:S2699")
class MeasureScoringTypeCohortTest {
    // req'd populations
    // exception works
    // exclusion works
    // has score
    // resource based
    // boolean based
    // group scoring def
    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    @Test
    void cohortBooleanPopulation() {

        given.when()
                .measureId("CohortBooleanAllPopulations")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .up()
                .report();
    }

    @Test
    void cohortBooleanIndividual() {

        given.when()
                .measureId("CohortBooleanAllPopulations")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .up()
                .report();
    }

    @Test
    void cohortResourcePopulation() {

        given.when()
                .measureId("CohortResourceAllPopulations")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .up()
                .report();
    }

    @Test
    void cohortBooleanMissingRequiredPopulation() {
        given.when()
                .measureId("CohortBooleanMissingReqdPopulation")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "MeasurePopulationType: numerator, is not a member of allowed 'cohort' populations")
                .report();
    }

    @Test
    void cohortResourceIndividual() {

        given.when()
                .measureId("CohortResourceAllPopulations")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .up()
                .report();
    }

    @Test
    void cohortBooleanExtraInvalidPopulation() {
        given.when()
                .measureId("CohortBooleanExtraInvalidPopulation")
                .evaluate()
                .then()
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "MeasurePopulationType: denominator, is not a member of allowed 'cohort' populations")
                .report();
    }

    @Test
    void cohortBooleanGroupScoringDef() {

        given.when()
                .measureId("CohortBooleanGroupScoringDef")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .up()
                .report();
    }
}
