package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class ContinuousVariableResourceMeasureObservationTest {

    // LUKETODO:  DSTU3 tests fail

    private static final Given GIVEN_BOOLEAN_BASIS =
            Measure.given().repositoryFor("ContinuousVariableObservationBooleanBasis");
    private static final Given GIVEN_ENCOUNTER_BASIS =
            Measure.given().repositoryFor("ContinuousVariableObservationEncounterBasis");

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisAvg() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisAvg")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                // 10 encounters in all
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                // There are 10 patients in all
                .hasCount(10)
                .up()
                .hasScore("74.0")
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

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisCount() {

        GIVEN_BOOLEAN_BASIS
                .when()
                .measureId("ContinuousVariableResourceMeasureObservationBooleanBasisCount")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                // 10 encounters in all
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                // There are 10 patients in all
                .hasCount(10)
                .up()
                .hasScore("10.0")
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
                // 10 encounters in all
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                // There are 10 patients in all
                .hasCount(10)
                .up()
                .hasScore("77.5")
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
                // 10 encounters in all
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                // There are 10 patients in all
                .hasCount(10)
                .up()
                .hasScore("55.0")
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
                // 10 encounters in all
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                // There are 10 patients in all
                .hasCount(10)
                .up()
                .hasScore("85.0")
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
                // 10 encounters in all
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                // There are 10 patients in all
                .hasCount(10)
                .up()
                .hasScore("740.0")
                .up()
                .report();
    }

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
