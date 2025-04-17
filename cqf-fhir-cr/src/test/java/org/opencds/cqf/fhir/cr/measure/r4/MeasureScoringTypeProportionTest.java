package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.Repository;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

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
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(
                    getResourcePath(MeasureScoringTypeProportionTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }

    @Test
    void proportionBooleanPopulation() {

        given.when()
                .measureId("ProportionBooleanAllPopulations")
                .evaluate()
                .then()
                .firstGroup()
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
                .firstGroup()
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
                .firstGroup()
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
                .firstGroup()
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
                .firstGroup()
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
