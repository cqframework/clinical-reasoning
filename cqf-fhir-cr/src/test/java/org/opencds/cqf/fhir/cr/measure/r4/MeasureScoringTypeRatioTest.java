package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type Ratio
 */
@SuppressWarnings("squid:S2699")
class MeasureScoringTypeRatioTest {
    // req'd populations
    // exception works
    // exclusion works
    // has score
    // resource based
    // boolean based
    // group scoring def
    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    @Test
    void ratioBooleanPopulation() {

        given.when()
                .measureId("RatioBooleanAllPopulations")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore(0.25)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore("0.25")
                .up()
                .report();
    }

    @Test
    void ratioBooleanIndividual() {

        given.when()
                .measureId("RatioBooleanAllPopulations")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore(1.0)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .report();
    }

    @Test
    void ratioResourcePopulation() {

        given.when()
                .measureId("RatioResourceAllPopulations")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(11)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(11)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore(0.2222222222222222)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(11)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(11)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore("0.2222222222222222")
                .up()
                .report();
    }

    @Test
    void ratioBooleanMissingRequiredPopulation() {
        given.when()
                .measureId("RatioBooleanMissingReqdPopulation")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("'ratio' measure is missing required population: denominator")
                .report();
    }

    @Test
    void ratioResourceIndividual() {

        given.when()
                .measureId("RatioResourceAllPopulations")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore(0.5)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("0.5")
                .up()
                .report();
    }

    @Test
    void ratioBooleanExtraInvalidPopulation() {
        given.when()
                .measureId("RatioBooleanExtraInvalidPopulation")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "MeasurePopulationType: measure-population, is not a member of allowed 'ratio' populations")
                .report();
    }

    @Test
    void ratioBooleanGroupScoringDef() {

        given.when()
                .measureId("RatioBooleanGroupScoringDef")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore(0.25)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore("0.25")
                .up()
                .report();
    }
}
