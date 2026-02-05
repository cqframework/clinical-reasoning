package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .hasMeasureScoring(MeasureScoring.PROPORTION)
                .firstGroup()
                .hasNoGroupLevelScoring()
                .hasEffectiveScoring(MeasureScoring.PROPORTION)
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore(0.3333333333333333)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .hasNoGroupScoringExt()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
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
                .hasMeasureScoring(MeasureScoring.PROPORTION)
                .firstGroup()
                .hasNoGroupLevelScoring()
                .hasEffectiveScoring(MeasureScoring.PROPORTION)
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore(1.0)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .hasNoGroupScoringExt()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(0) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
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
                .hasMeasureScoring(MeasureScoring.PROPORTION)
                .firstGroup()
                .hasNoGroupLevelScoring()
                .hasEffectiveScoring(MeasureScoring.PROPORTION)
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore(0.3333333333333333)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .hasNoGroupScoringExt()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
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
                .hasMeasureScoring(MeasureScoring.PROPORTION)
                .firstGroup()
                .hasNoGroupLevelScoring()
                .hasEffectiveScoring(MeasureScoring.PROPORTION)
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore(1.0)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .hasNoGroupScoringExt()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
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
                .hasNoMeasureScoring()
                .firstGroup()
                .hasGroupLevelScoring(MeasureScoring.PROPORTION)
                .hasEffectiveScoring(MeasureScoring.PROPORTION)
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore(0.3333333333333333)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .hasGroupScoringExt(MeasureScoring.PROPORTION)
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }
}
