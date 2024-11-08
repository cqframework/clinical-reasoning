package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type Ratio
 */
public class MeasureScoringTypeRatioTest {
    // req'd populations
    // exception works
    // exclusion works
    // has score
    // resource based
    // boolean based
    // group scoring def
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(MeasureStratifierTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    protected Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }

    @Test
    void ratioBooleanPopulation() {

        given.when()
                .measureId("RatioBooleanAllPopulations")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(8)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
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
    void ratioResourcePopulation() {

        given.when()
                .measureId("RatioResourceAllPopulations")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(9)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.2222222222222222")
                .up()
                .report();
    }

    @Test
    void ratioBooleanMissingRequiredPopulation() {
        try {
            given.when()
                    .measureId("RatioBooleanMissingReqdPopulation")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .population("initial-population")
                    .hasCount(10)
                    .up()
                    .population("denominator-exclusion")
                    .hasCount(2)
                    .up()
                    .population("numerator-exclusion")
                    .hasCount(2)
                    .up()
                    .population("numerator")
                    .hasCount(2)
                    .up()
                    .hasScore("0.3333333333333333")
                    .up()
                    .report();
            fail("This should throw error");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("'ratio' measure is missing required population: denominator"));
        }
    }

    @Test
    void ratioResourceIndividual() {

        given.when()
                .measureId("RatioResourceAllPopulations")
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
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("0.5")
                .up()
                .report();
    }

    @Test
    void ratioBooleanExtraInvalidPopulation() {
        try {
            given.when()
                    .measureId("RatioBooleanExtraInvalidPopulation")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .population("initial-population")
                    .hasCount(10)
                    .up()
                    .population("denominator-exclusion")
                    .hasCount(2)
                    .up()
                    .population("numerator-exclusion")
                    .hasCount(2)
                    .up()
                    .population("numerator")
                    .hasCount(2)
                    .up()
                    .hasScore("0.3333333333333333")
                    .up()
                    .report();
            fail("This should throw error");
        } catch (UnsupportedOperationException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "MeasurePopulationType: measure-population, is not a member of allowed 'ratio' populations"));
        }
    }

    @Test
    void ratioBooleanGroupScoringDef() {

        given.when()
                .measureId("RatioBooleanGroupScoringDef")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(8)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.25")
                .up()
                .report();
    }
}
