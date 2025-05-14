package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
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
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final IRepository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(MeasureScoringTypeCohortTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
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
