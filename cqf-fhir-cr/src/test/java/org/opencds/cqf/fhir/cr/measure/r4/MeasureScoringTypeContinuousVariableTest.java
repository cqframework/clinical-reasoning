package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepositoryForTests;

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
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final IRepository repository = new IgRepositoryForTests(
            FhirContext.forR4Cached(),
            Path.of(getResourcePath(MeasureScoringTypeContinuousVariableTest.class) + "/" + CLASS_PATH + "/"
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
        given.when()
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
        given.when()
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
