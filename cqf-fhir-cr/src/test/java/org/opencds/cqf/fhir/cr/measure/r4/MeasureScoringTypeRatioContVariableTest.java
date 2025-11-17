package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * Summary of generated Patients and their Encounter durations.
 *
 * All Encounters share the period:
 * - **Start:** 2024-01-01T01:00:00Z
 * - **End:**   2024-01-01T03:00:00Z
 * → **Duration:** 120 minutes
 *
 * ## Encounter Durations (Markdown Table)
 *
 * | Patient ID         | DOB          | Age (yrs) | Encounter ID                   | Status       | Duration (min) |
 * |--------------------|--------------|-----------|--------------------------------|--------------|----------------|
 * | patient-0          | 1985-06-16   | 39        | patient-0-encounter-1          | in-progress  | 120            |
 * | patient-1          | 1988-01-11   | 36        | patient-1-encounter-1          | in-progress  | 120            |
 * | patient-2          | 1985-06-16   | 39        | patient-2-encounter-1          | arrived      | 120            |
 * | patient-3          | 1988-01-11   | 36        | patient-3-encounter-1          | arrived      | 120            |
 * | patient-4          | 1985-06-16   | 39        | patient-4-encounter-1          | triaged      | 120            |
 * | patient-5          | 1988-01-11   | 36        | patient-5-encounter-1          | triaged      | 120            |
 * | patient-6          | 1985-06-16   | 39        | patient-6-encounter-1          | cancelled    | 120            |
 * | patient-7          | 1988-01-11   | 36        | patient-7-encounter-1          | cancelled    | 120            |
 * | patient-8          | 1985-06-16   | 39        | patient-8-encounter-1          | finished     | 120            |
 * | patient-9          | 1988-01-11   | 36        | patient-9-encounter-1          | finished     | 30             |
 * | patient-9          | 1988-01-11   | 36        | patient-9-encounter-2          | in-progress  | 180            |
 *
 * ## Encounter Durations
 *
 * | Metric | Value |
 * |--------|-------|
 * | Count  | 11    |
 * | Sum    | 1290  |
 * | Avg    | 117.27   |
 * | Min    | 30   |
 * | Max    | 180   |
 * | Median | 120   |
 *
 * ## Age Statistics (as of 2024-12-31)
 *
 * | Metric | Value |
 * |--------|-------|
 * | Count  | 10    |
 * | Sum    | 375   |
 * | Avg    | 37.5  |
 * | Min    | 36    |
 * | Max    | 39    |
 * | Median | 37.5  |
 */
@SuppressWarnings("squid:S2699")
class MeasureScoringTypeRatioContVariableTest {

    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final IRepository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Path.of(getResourcePath(MeasureScoringTypeRatioContVariableTest.class) + "/" + CLASS_PATH + "/"
                    + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);

    /**
     * Test 1:
     * ResourceBasis
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: Sum
     */
    @Test
    void ratioContinuousVariableResourceBasisSum() {

        given.when()
                .measureId("RatioContVarResourceSum")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("0.2222222222222222")
                .up()
                .report();
    }

    /**
     * Test 2:
     * ResourceBasis
     * aggregateMethod Numerator: Count
     * aggregateMethod Denominator: Count
     */
    @Test
    void ratioContinuousVariableResourceBasisCount() {

        given.when()
                .measureId("RatioContVarResourceCount")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("0.2222222222222222") // 2/9
                .up()
                .report();
    }

    /**
     * Test 3:
     * ResourceBasis
     * aggregateMethod Numerator: Avg
     * aggregateMethod Denominator: Avg
     */
    @Test
    void ratioContinuousVariableResourceBasisAvg() {

        given.when()
                .measureId("RatioContVarResourceAvg")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("1.0") // 120/120
                .up()
                .report();
    }

    /**
     * Test 4:
     * ResourceBasis
     * aggregateMethod Numerator: Min
     * aggregateMethod Denominator: Min
     */
    @Test
    void ratioContinuousVariableResourceBasisMin() {

        given.when()
                .measureId("RatioContVarResourceMin")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("1.0") // 30/30
                .up()
                .report();
    }
    /**
     * Test 5:
     * ResourceBasis
     * aggregateMethod Numerator: Max
     * aggregateMethod Denominator: Max
     */
    @Test
    void ratioContinuousVariableResourceBasisMax() {

        given.when()
                .measureId("RatioContVarResourceMax")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("1.0") // 180/180
                .up()
                .report();
    }
    /**
     * Test 6:
     * ResourceBasis
     * aggregateMethod Numerator: Median
     * aggregateMethod Denominator: Median
     */
    @Test
    void ratioContinuousVariableResourceBasisMedian() {

        given.when()
                .measureId("RatioContVarResourceMedian")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("1.0") // 120/120
                .up()
                .report();
    }
    /**
     * Test 7:
     * BooleanBasis
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: Sum
     */
    @Test
    void ratioContinuousVariableBooleanBasisSum() {

        given.when()
                .measureId("RatioContVarBooleanSum")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10) // final Denominator = 8 (10-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(8) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("0.25") // 36,39=75, 36,39,36,39,36,39,36,39=300 ===>.25
                .up()
                .report();
    }
    /**
     * Test 8:
     * ResourceBasis
     * Mixed Aggregate Methods
     * aggregateMethod Numerator: avg
     * aggregateMethod Denominator: Sum
     */
    @Test
    void ratioContinuousVariableResourceBasisMix() {

        given.when()
                .measureId("RatioContVarResourceMix")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("0.0892857143") // avg/sum  avg(30, 120)= 75, sum(30, 180,120,120,120,120,120,120,120)=840,
                // 75/840
                .up()
                .report();
    }
    /**
     * Test 9:
     * Mixed Basis on MeasureObservations, This should Error due to mismatch of basis on criteria expression function
     * aggregateMethod Numerator: Sum Age
     * aggregateMethod Denominator: Sum Encounter Durations
     */
    @Test
    void ratioContinuousVariableMixBasisSum() {

        given.when()
                .measureId("RatioContVarMixBasisSum")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcomeMsg(
                        "Measure observation criteria expression: MeasureObservationBoolean is missing a function parameter matching the population-basis")
                .firstGroup()
                .hasMeasureScore(false)
                .up()
                .report();
    }
}
