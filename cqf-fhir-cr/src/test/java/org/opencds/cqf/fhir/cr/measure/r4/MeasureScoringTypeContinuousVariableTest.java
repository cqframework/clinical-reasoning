package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type Continuous-Variable
 */
@SuppressWarnings("squid:S2699")
class MeasureScoringTypeContinuousVariableTest {
    // req'd populations
    // exception works
    // exclusion works
    // has score
    // resource based
    // boolean based
    // group scoring def
    private static final Given GIVEN = Measure.given().repositoryFor("MeasureScoringTypeContinuousVariable");

    @Test
    void continuousVariableBooleanPopulation() {

        GIVEN.when()
                .measureId("ContinuousVariableBooleanAllPopulations")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableBooleanIndividual() {

        GIVEN.when()
                .measureId("ContinuousVariableBooleanAllPopulations")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("measure-population")
                .hasCount(1)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableResourcePopulation() {

        GIVEN.when()
                .measureId("ContinuousVariableResourceAllPopulations")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("measure-population")
                .hasCount(11)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableBooleanMissingRequiredPopulation() {
        GIVEN.when()
                .measureId("ContinuousVariableBooleanMissingReqdPopulation")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "'continuous-variable' measure is missing required population: initial-population")
                .report();
    }

    @Test
    void continuousVariableBooleanProhibitedPopulations() {
        GIVEN.when()
                .measureId("ContinuousVariableBooleanProhibitedPopulations")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "MeasurePopulationType: denominator, is not a member of allowed 'continuous-variable' populations.")
                .report();
    }

    @Test
    void continuousVariableResourceIndividual() {

        GIVEN.when()
                .measureId("ContinuousVariableResourceAllPopulations")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("measure-population")
                .hasCount(2)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableBooleanExtraInvalidPopulation() {
        GIVEN.when()
                .measureId("ContinuousVariableBooleanExtraInvalidPopulation")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "MeasurePopulationType: denominator, is not a member of allowed 'continuous-variable' populations")
                .report();
    }

    @Test
    void continuousVariableBooleanGroupScoringDef() {

        GIVEN.when()
                .measureId("ContinuousVariableBooleanGroupScoringDef")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .up()
                .report();
    }
}
