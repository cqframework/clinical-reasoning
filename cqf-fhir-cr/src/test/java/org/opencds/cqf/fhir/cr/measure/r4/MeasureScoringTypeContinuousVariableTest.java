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
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type Continuous-Variable
 */
public class MeasureScoringTypeContinuousVariableTest {
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
            Paths.get(getResourcePath(MeasureScoringTypeContinuousVariableTest.class) + "/" + CLASS_PATH + "/"
                    + "MeasureTest"));
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
    void continuousVariableBooleanPopulation() {

        given.when()
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

        given.when()
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

        given.when()
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
        try {
            given.when()
                    .measureId("ContinuousVariableBooleanMissingReqdPopulation")
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
                    .hasCount(10)
                    .up()
                    .up()
                    .report();
            fail("This should throw error");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage()
                    .contains("'continuous-variable' measure is missing required population: initial-population"));
        }
    }

    @Test
    void continuousVariableResourceIndividual() {

        given.when()
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
        try {
            given.when()
                    .measureId("ContinuousVariableBooleanExtraInvalidPopulation")
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
                    .hasCount(10)
                    .up()
                    .up()
                    .report();
            fail("This should throw error");
        } catch (UnsupportedOperationException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "MeasurePopulationType: denominator, is not a member of allowed 'continuous-variable' populations"));
        }
    }

    @Test
    void continuousVariableBooleanGroupScoringDef() {

        given.when()
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
