package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

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

    private static final Given given = Measure.given().repositoryFor("MeasureTest");

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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(0.14285714285714285) // 150/1050  sum(30,120) /sum(30, 180, 120, 120, 120, 120, 120, 120, 120)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                .hasScore(
                        "0.14285714285714285") // 150/1050  sum(30,120) /sum(30, 180, 120, 120, 120, 120, 120, 120, 120)
                .up()
                // LUKETODO: get rid of this:
                .logReportJson()
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(0.2222222222222222) // 2/9
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(0.6428571428571428) // 75/116.6666 avg(30,120)=75,avg(180,30,120,120,120,120,120,120,120)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                .hasScore("0.6428571428571428") // 75/116.6666 avg(30,120)=75,avg(180,30,120,120,120,120,120,120,120)
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(1.0) // 30/30
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(0.6666666666666666) // 120/180
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                .hasScore("0.6666666666666666") // 120/180
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(0.625) // 75/120 Median(30,120)=75
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .hasStatus(MeasureReportStatus.COMPLETE)
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
                .hasScore("0.625") // 75/120 Median(30,120)=75
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(0.25) // 36,39=75, 36,39,36,39,36,39,36,39=300 ===>.25
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasScore(0.07142857142857142) // 75/1050 avg/sum  avg(30, 120)= 75, sum(30,
                // 180,120,120,120,120,120,120,120)=1050,
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                .hasScore("0.07142857142857142") // 75/1050 avg/sum  avg(30, 120)= 75, sum(30,
                // 180,120,120,120,120,120,120,120)=1050,
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

    /**
     * Test 10:
     * Den=0 scenario
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: Sum
     */
    @Test
    void ratioContinuousVariableNoDenominator() {
        given.when()
                .measureId("RatioContVarResourceSum")
                .subject("Patient/patient-7")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(0) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(0) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(0) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasMeasureScore(false)
                .up()
                .report();
    }
    /**
     * Test 11:
     * Den>0, Num=0 scenario
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: Sum
     */
    @Test
    void ratioContinuousVariableNoNumerator() {
        given.when()
                .measureId("RatioContVarResourceSum2")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(1) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(0) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasMeasureScore(false)
                .up()
                .report();
    }
    /**
     * Test 12:
     * no den defined
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: NA
     */
    @Test
    void ratioContinuousVariableNoDenDef() {
        given.when()
                .measureId("RatioContVarResourceSumError")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcomeMsg(
                        "Ratio Continuous Variable requires 2 Measure Observations defined, you have: 1")
                .firstGroup()
                .hasMeasureScore(false)
                .up()
                .report();
    }

    /**
     * Test 12:
     * bad den defined
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: NA
     */
    @Test
    void ratioContinuousVariableBadDenDef() {
        try {
            given.when()
                    .measureId("RatioContVarResourceSumError2")
                    .subject("Patient/patient-9")
                    .evaluate()
                    .then()
                    .report();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("no matching criteria reference was found for extension"));
        }
    }

    /**
     * Test 13:
     * test RCV Stratifier Scoring
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: Sum
     */
    @Test
    void ratioContinuousVariableStratifierResource() {
        given.when()
                .measureId("RatioContVarResourceSumValueStrat")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
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
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(9) // final Numerator = 9
                .up()
                .hasScore(0.7714285714285715) // 810/1050  sum(30,180,120,120,120,120,120) /sum(30, 180, 120, 120, 120,
                // 120, 120, 120, 120)
                .firstStratifier()
                .hasStratumCount(2)
                .stratumByValue("M")
                .hasScore(0.7894736842105263) // 450/570 Sum(180,30,120,120)/sum(180,30,120,120,120)
                .population("initial-population")
                .hasCount(6)
                .up()
                .population("denominator")
                .hasCount(6) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(5) // final Numerator = 2
                .up()
                .up()
                .stratumByValue("F")
                .hasScore(0.75) // sum(120,120,120)/sum(120,120,120,120)
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("denominator")
                .hasCount(5) // final Denominator = 4 (5-1)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(4) // final Numerator = 3
                .up()
                .up()
                .up()
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
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
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(9) // final Numerator = 9
                .up()
                .populationId("observation-den")
                .hasCount(9) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(7) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore(
                        "0.7714285714285715") // 810/1050  sum(30,180,120,120,120,120,120) /sum(30, 180, 120, 120, 120,
                // 120, 120, 120, 120)
                .firstStratifier()
                .hasStratumCount(2)
                .stratum("M")
                .hasScore("0.7894736842105263") // 450/570 Sum(180,30,120,120)/sum(180,30,120,120,120)
                .population("initial-population")
                .hasCount(6)
                .up()
                .population("denominator")
                .hasCount(6) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(5) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(5) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(4)
                .up()
                .up()
                .stratum("F")
                .hasScore("0.75") // sum(120,120,120)/sum(120,120,120,120)
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("denominator")
                .hasCount(5) // final Denominator = 4 (5-1)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(4) // final Numerator = 3
                .up()
                .populationId("observation-den")
                .hasCount(4) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(3)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Test 13:
     * test RCV Stratifier Scoring with Boolean basis
     * aggregateMethod Numerator: Sum
     * aggregateMethod Denominator: Sum
     */
    @Test
    void ratioContinuousVariableStratifierBoolean() {
        given.when()
                .measureId("RatioContVarBooleanSumValueStrat")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 9
                .up()
                .hasScore(0.25) // 75 / 300  sum(36,39) /sum(39,36,39,36,39,36,39,36)
                .firstStratifier()
                .hasStratumCount(2)
                .stratumByValue("M")
                .hasScore(0.25) //  Sum(36)/sum(36,36,36,36)
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("denominator")
                .hasCount(5) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1) // final Numerator = 2
                .up()
                .up()
                .stratumByValue("F")
                .hasScore(0.25) // sum(39)/sum(39,39,39,39)
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("denominator")
                .hasCount(5) // final Denominator = 4 (5-1)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1) // final Numerator = 3
                .up()
                .up()
                .up()
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2) // final Numerator = 9
                .up()
                .populationId("observation-den")
                .hasCount(8) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("0.25") // 75 / 300  sum(36,39) /sum(39,36,39,36,39,36,39,36)
                .firstStratifier()
                .hasStratumCount(2)
                .stratum("M")
                .hasScore("0.25") //  Sum(36)/sum(36,36,36,36)
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("denominator")
                .hasCount(5) // final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1) // final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(4) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(1)
                .up()
                .up()
                .stratum("F")
                .hasScore("0.25") // sum(39)/sum(39,39,39,39)
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("denominator")
                .hasCount(5) // final Denominator = 4 (5-1)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1) // final Numerator = 3
                .up()
                .populationId("observation-den")
                .hasCount(4) // we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
}
