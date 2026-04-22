package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type Proportion
 */
@SuppressWarnings("squid:S2699")
class MeasureScoringTypeProportionTest {
    // missing req'd populations
    // denominator-exception works
    // denominator-exclusion works
    // group has score
    // resource based measures work
    // boolean based measures work
    // group scoring def
    // measure scoring def
    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    @Test
    void proportionBooleanPopulation() {

        given.when()
                .measureId("ProportionBooleanAllPopulations")
                .evaluate()
                .then()
                .hasSupplementalDataSearchParameter()
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore(0.3333333333333333)
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionBooleanIndividual() {

        given.when()
                .measureId("ProportionBooleanAllPopulations")
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(0) // because subject was also in Numerator
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
    void proportionResourcePopulation() {

        given.when()
                .measureId("ProportionResourceAllPopulations")
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore(0.3333333333333333)
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionBooleanMissingRequiredPopulation() {
        given.when()
                .measureId("ProportionBooleanMissingReqdPopulation")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("'proportion' measure is missing required population: denominator")
                .report();
    }

    @Test
    void proportionResourceIndividual() {

        given.when()
                .measureId("ProportionResourceAllPopulations")
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
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
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
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
    void proportionBooleanExtraInvalidPopulation() {
        given.when()
                .measureId("ProportionBooleanExtraInvalidPopulation")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "MeasurePopulationType: measure-population, is not a member of allowed 'proportion' populations")
                .report();
    }

    @Test
    void proportionBooleanGroupScoringDef() {

        given.when()
                .measureId("ProportionBooleanGroupScoringDef")
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore(0.3333333333333333)
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
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }
}
