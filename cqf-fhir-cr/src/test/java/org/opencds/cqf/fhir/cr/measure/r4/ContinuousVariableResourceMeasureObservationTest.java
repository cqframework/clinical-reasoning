package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class ContinuousVariableResourceMeasureObservationTest {

    private static final Given GIVEN_BOOLEAN_BASIS =
            Measure.given().repositoryFor("ContinuousVariableObservationBooleanBasis");
    private static final Given GIVEN_ENCOUNTER_BASIS =
            Measure.given().repositoryFor("ContinuousVariableObservationEncounterBasis");

    // LUKETODO:   add patients with various birth dates and base measure observation on age
    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisAvg() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisAvg")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                // LUKETODO:  figure out what the MeasureObservation function will do before determining a score
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisAvg() {

        GIVEN_ENCOUNTER_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationEncounterBasisAvg")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                .hasScore("230.54545454545453")
                .up()
                .report();
    }

    // LUKETODO:  this should be the easiest one:
    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisCount() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisCount")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                // LUKETODO:  figure out what the MeasureObservation function will do before determining a score
                .hasScore("11.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisCount() {

        GIVEN_ENCOUNTER_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationEncounterBasisCount")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                .hasScore("11.0") // I assume this is the straight-up count of encounters?
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisMedian() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisMedian")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                // LUKETODO:  figure out what the MeasureObservation function will do before determining a score
                .hasScore("1320.0")
                .up()
                .report();
    }

    // LUKETODO:  stratifiers
    /*
    I just want a stratifier use case to make sure the scoring shows up for stratum
    can be any stratifier type, just need to validate stratifiers score as cont-variable scoring type
     */

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMedian() {

        GIVEN_ENCOUNTER_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationEncounterBasisMedian")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                .hasScore("120.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisMin() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisMin")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                // LUKETODO:  figure out what the MeasureObservation function will do before determining a score
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMin() {

        GIVEN_ENCOUNTER_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationEncounterBasisMin")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                .hasScore("15.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisMax() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisMax")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                // LUKETODO:  figure out what the MeasureObservation function will do before determining a score
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMax() {

        GIVEN_ENCOUNTER_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationEncounterBasisMax")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                .hasScore("840.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisSum() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisSum")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                // LUKETODO:  why do we have 10 if this is a boolean basis?
                .hasCount(11)
                .up()
                .population("measure-population")
                .hasCount(11)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                .hasCount(11)
                .up()
                // LUKETODO:  figure out what the MeasureObservation function will do before determining a score
                .hasScore("1320.0")
                .up()
                .report();
    }

    // LUKETODO:  for encounters, leave two encounter periods the same so we can test set logic doesn't eliminate dupes
    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisSum() {

        GIVEN_ENCOUNTER_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationEncounterBasisSum")
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
                .population("measure-observation")
                .hasCount(11)
                .up()
                .hasScore("2536.0")
                .up()
                .report();
    }
}
