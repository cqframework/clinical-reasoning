package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class ContinuousVariableResourceMeasureObservationTest {

    private static final Given GIVEN = Measure.given().repositoryFor("ContinuousVariableObservation");

    // LUKETODO:  add stratifies to the mix?

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisAvg() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisAvg() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    // LUKETODO:  this should be the easiest one:
    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisCount() {

        GIVEN.when()
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
                .hasScore("11.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisCount() {

        GIVEN.when()
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

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMedian() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisMin() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMin() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisMax() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMax() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisSum() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisSum() {

        GIVEN.when()
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
                .hasScore("1320.0")
                .up()
                .report();
    }
}
