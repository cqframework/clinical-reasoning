package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class ContinuousVariableResourceMeasureObservationTest {

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
                .stratifierById("stratifier-gender")
                .stratumCount(4)
                .firstStratum()
                .hasValue("male")
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue("female")
                .hasScore("480.0")
                .up()
                .stratumByPosition(3)
                .hasValue("other")
                .hasScore("75.2")
                .up()
                .stratumByPosition(4)
                .hasValue("unknown")
                .hasScore("75.2")
                .up()
                .up()
                .up()
                .report();
    }

    // LUKETODO:  refactor for code reuse
    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisAvg() {

        /*
        # total sum: 2536.0

        # first stratum:  84:

        patient-0-encounter-1: 00:00 to 02:00 (120 minutes)
        patient-1-encounter-1: 00:00 to 02:00 (120 minutes)

        sum: 240.0

        # second stratum:  74:

        patient-2-encounter-1: 03:00 to 12:00 (540 minutes)
        patient-3-encounter-1: 07:00 to 14:00 (420 minutes)
        patient-4-encounter-1: 05:00 to 19:00 (840 minutes)
        patient-5-encounter-1: 02:00 to 04:00 (120 minutes)

        sum: 1920.0

        # third stratum:  64:

        patient-6-encounter-1: 03:00 to 03:30 (30 minutes)
        patient-7-encounter-1: 01:00 to 02:30 (90 minutes)
        patient-8-encounter-1: 00:00 to 00:15 (15 minutes)
        patient-9-encounter-1: 01:01 to 03:02 (121 minutes)
        patient-9-encounter-2: 01:00 to 03:00 (120 minutes)

        sum: 376.0
         */

        final LocalDate measurementPeriodStart = LocalDate.of(2024, 1, 1);

        final int expectedAgeStratum1 = computeAge(measurementPeriodStart, LocalDate.of(1940, Month.JANUARY, 1));
        final int expectedAgeStratum2 = computeAge(measurementPeriodStart, LocalDate.of(1950, Month.JANUARY, 1));
        final int expectedAgeStratum3 = computeAge(measurementPeriodStart, LocalDate.of(1960, Month.JANUARY, 1));

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
                .stratifierById("stratifier-age")
                .stratumCount(3)
                .firstStratum()
                .hasValue(Integer.toString(expectedAgeStratum1))
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue(Integer.toString(expectedAgeStratum2))
                .hasScore("480.0")
                .up()
                .stratumByPosition(3)
                .hasValue(Integer.toString(expectedAgeStratum3))
                .hasScore("75.2")
                .up()
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisCount() {
        /*
        female:

        patient-1940-1
        patient-1950
        patient-1965
        patient-1970

        male:

        patient-1940-2
        patient-1945-2

        other:

        patient-1940-3
        patient-1945-1
        patient-1955

        unknown:
        patient-1960
         */

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
                .stratifierById("stratifier-gender")
                .stratumCount(4)
                .firstStratum()
                .hasValue("male")
                .hasScore("2.0")
                .up()
                .stratumByPosition(2)
                .hasValue("unknown")
                .hasScore("1.0")
                .up()
                .stratumByPosition(3)
                .hasValue("other")
                .hasScore("3.0")
                .up()
                .stratumByPosition(4)
                .hasValue("female")
                .hasScore("4.0")
                .up()
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisCount() {

        /*
        # total sum: 2536.0

        # first stratum:  84:

        patient-0-encounter-1: 00:00 to 02:00 (120 minutes)
        patient-1-encounter-1: 00:00 to 02:00 (120 minutes)

        sum: 240.0

        # second stratum:  74:

        patient-2-encounter-1: 03:00 to 12:00 (540 minutes)
        patient-3-encounter-1: 07:00 to 14:00 (420 minutes)
        patient-4-encounter-1: 05:00 to 19:00 (840 minutes)
        patient-5-encounter-1: 02:00 to 04:00 (120 minutes)

        sum: 1920.0

        # third stratum:  64:

        patient-6-encounter-1: 03:00 to 03:30 (30 minutes)
        patient-7-encounter-1: 01:00 to 02:30 (90 minutes)
        patient-8-encounter-1: 00:00 to 00:15 (15 minutes)
        patient-9-encounter-1: 01:01 to 03:02 (121 minutes)
        patient-9-encounter-2: 01:00 to 03:00 (120 minutes)

        sum: 376.0
         */

        final LocalDate measurementPeriodStart = LocalDate.of(2024, 1, 1);

        final int expectedAgeStratum1 = computeAge(measurementPeriodStart, LocalDate.of(1940, Month.JANUARY, 1));
        final int expectedAgeStratum2 = computeAge(measurementPeriodStart, LocalDate.of(1950, Month.JANUARY, 1));
        final int expectedAgeStratum3 = computeAge(measurementPeriodStart, LocalDate.of(1960, Month.JANUARY, 1));

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
                .stratifierById("stratifier-age")
                .stratumCount(3)
                .firstStratum()
                .hasValue(Integer.toString(expectedAgeStratum1))
                .hasScore("2.0")
                .up()
                .stratumByPosition(2)
                .hasValue(Integer.toString(expectedAgeStratum2))
                .hasScore("4.0")
                .up()
                .stratumByPosition(3)
                .hasValue(Integer.toString(expectedAgeStratum3))
                .hasScore("5.0")
                .up()
                .up()
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
                .stratifierById("stratifier-gender")
                .stratumCount(4)
                .firstStratum()
                .hasValue("male")
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue("female")
                .hasScore("480.0")
                .up()
                .stratumByPosition(3)
                .hasValue("other")
                .hasScore("75.2")
                .up()
                .stratumByPosition(4)
                .hasValue("unknown")
                .hasScore("75.2")
                .up()
                .up()
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
        /*
        # total sum: 2536.0

        # first stratum:  84:

        patient-0-encounter-1: 00:00 to 02:00 (120 minutes)
        patient-1-encounter-1: 00:00 to 02:00 (120 minutes)

        sum: 240.0

        # second stratum:  74:

        patient-2-encounter-1: 03:00 to 12:00 (540 minutes)
        patient-3-encounter-1: 07:00 to 14:00 (420 minutes)
        patient-4-encounter-1: 05:00 to 19:00 (840 minutes)
        patient-5-encounter-1: 02:00 to 04:00 (120 minutes)

        sum: 1920.0

        # third stratum:  64:

        patient-6-encounter-1: 03:00 to 03:30 (30 minutes)
        patient-7-encounter-1: 01:00 to 02:30 (90 minutes)
        patient-8-encounter-1: 00:00 to 00:15 (15 minutes)
        patient-9-encounter-1: 01:01 to 03:02 (121 minutes)
        patient-9-encounter-2: 01:00 to 03:00 (120 minutes)

        sum: 376.0
         */

        final LocalDate measurementPeriodStart = LocalDate.of(2024, 1, 1);

        final int expectedAgeStratum1 = computeAge(measurementPeriodStart, LocalDate.of(1940, Month.JANUARY, 1));
        final int expectedAgeStratum2 = computeAge(measurementPeriodStart, LocalDate.of(1950, Month.JANUARY, 1));
        final int expectedAgeStratum3 = computeAge(measurementPeriodStart, LocalDate.of(1960, Month.JANUARY, 1));

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
                .stratifierById("stratifier-age")
                .stratumCount(3)
                .firstStratum()
                .hasValue(Integer.toString(expectedAgeStratum1))
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue(Integer.toString(expectedAgeStratum2))
                .hasScore("480.0")
                .up()
                .stratumByPosition(3)
                .hasValue(Integer.toString(expectedAgeStratum3))
                .hasScore("90.0")
                .up()
                .up()
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
                .stratifierById("stratifier-gender")
                .stratumCount(4)
                .firstStratum()
                .hasValue("male")
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue("female")
                .hasScore("480.0")
                .up()
                .stratumByPosition(3)
                .hasValue("other")
                .hasScore("75.2")
                .up()
                .stratumByPosition(4)
                .hasValue("unknown")
                .hasScore("75.2")
                .up()
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMin() {

        /*
        # total sum: 2536.0

        # first stratum:  84:

        patient-0-encounter-1: 00:00 to 02:00 (120 minutes)
        patient-1-encounter-1: 00:00 to 02:00 (120 minutes)

        sum: 240.0

        # second stratum:  74:

        patient-2-encounter-1: 03:00 to 12:00 (540 minutes)
        patient-3-encounter-1: 07:00 to 14:00 (420 minutes)
        patient-4-encounter-1: 05:00 to 19:00 (840 minutes)
        patient-5-encounter-1: 02:00 to 04:00 (120 minutes)

        sum: 1920.0

        # third stratum:  64:

        patient-6-encounter-1: 03:00 to 03:30 (30 minutes)
        patient-7-encounter-1: 01:00 to 02:30 (90 minutes)
        patient-8-encounter-1: 00:00 to 00:15 (15 minutes)
        patient-9-encounter-1: 01:01 to 03:02 (121 minutes)
        patient-9-encounter-2: 01:00 to 03:00 (120 minutes)

        sum: 376.0
         */

        final LocalDate measurementPeriodStart = LocalDate.of(2024, 1, 1);

        final int expectedAgeStratum1 = computeAge(measurementPeriodStart, LocalDate.of(1940, Month.JANUARY, 1));
        final int expectedAgeStratum2 = computeAge(measurementPeriodStart, LocalDate.of(1950, Month.JANUARY, 1));
        final int expectedAgeStratum3 = computeAge(measurementPeriodStart, LocalDate.of(1960, Month.JANUARY, 1));

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
                .stratifierById("stratifier-age")
                .stratumCount(3)
                .firstStratum()
                .hasValue(Integer.toString(expectedAgeStratum1))
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue(Integer.toString(expectedAgeStratum2))
                .hasScore("120.0")
                .up()
                .stratumByPosition(3)
                .hasValue(Integer.toString(expectedAgeStratum3))
                .hasScore("15.0")
                .up()
                .up()
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
                .stratifierById("stratifier-gender")
                .stratumCount(4)
                .firstStratum()
                .hasValue("male")
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue("female")
                .hasScore("480.0")
                .up()
                .stratumByPosition(3)
                .hasValue("other")
                .hasScore("75.2")
                .up()
                .stratumByPosition(4)
                .hasValue("unknown")
                .hasScore("75.2")
                .up()
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisMax() {

        /*
        # total sum: 2536.0

        # first stratum:  84:

        patient-0-encounter-1: 00:00 to 02:00 (120 minutes)
        patient-1-encounter-1: 00:00 to 02:00 (120 minutes)

        sum: 240.0

        # second stratum:  74:

        patient-2-encounter-1: 03:00 to 12:00 (540 minutes)
        patient-3-encounter-1: 07:00 to 14:00 (420 minutes)
        patient-4-encounter-1: 05:00 to 19:00 (840 minutes)
        patient-5-encounter-1: 02:00 to 04:00 (120 minutes)

        sum: 1920.0

        # third stratum:  64:

        patient-6-encounter-1: 03:00 to 03:30 (30 minutes)
        patient-7-encounter-1: 01:00 to 02:30 (90 minutes)
        patient-8-encounter-1: 00:00 to 00:15 (15 minutes)
        patient-9-encounter-1: 01:01 to 03:02 (121 minutes)
        patient-9-encounter-2: 01:00 to 03:00 (120 minutes)

        sum: 376.0
         */

        final LocalDate measurementPeriodStart = LocalDate.of(2024, 1, 1);

        final int expectedAgeStratum1 = computeAge(measurementPeriodStart, LocalDate.of(1940, Month.JANUARY, 1));
        final int expectedAgeStratum2 = computeAge(measurementPeriodStart, LocalDate.of(1950, Month.JANUARY, 1));
        final int expectedAgeStratum3 = computeAge(measurementPeriodStart, LocalDate.of(1960, Month.JANUARY, 1));

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
                .stratifierById("stratifier-age")
                .stratumCount(3)
                .firstStratum()
                .hasValue(Integer.toString(expectedAgeStratum1))
                .hasScore("120.0")
                .up()
                .stratumByPosition(2)
                .hasValue(Integer.toString(expectedAgeStratum2))
                .hasScore("840.0")
                .up()
                .stratumByPosition(3)
                .hasValue(Integer.toString(expectedAgeStratum3))
                .hasScore("121.0")
                .up()
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationBooleanBasisSum() {
        /*
        patient-1940-female-encounter-1: 00:00 to 02:00 (120 minutes)
        patient-1950-female-encounter-1: 03:00 to 03:30 (30 minutes)
        patient-1965-female-encounter-1: 01:01 to 03:02 (121 minutes)
        patient-1970-female-encounter-1: 01:00 to 03:00 (120 minutes)

        patient-1940-male-encounter-1:   00:00 to 02:00 (120 minutes)
        patient-1945-male-encounter-1:   05:00 to 19:00 (840 minutes)

        patient-1940-other-encounter-1:  03:00 to 12:00 (540 minutes)
        patient-1945-other-encounter-1:  05:00 to 19:00 (840 minutes)
        patient-1955-other-encounter-1:  01:00 to 02:30 (90 minutes)

        patient-1960-unknown-encounter-1: 00:00 to 00:15 (15 minutes)
        */

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
                .stratifierById("stratifier-gender")
                .stratumCount(4)
                .firstStratum()
                .hasValue("male")
                .hasScore("960.0")
                .up()
                .stratumByPosition(2)
                .hasValue("female")
                .hasScore("391.0")
                .up()
                .stratumByPosition(3)
                .hasValue("other")
                .hasScore("1470.0")
                .up()
                .stratumByPosition(4)
                .hasValue("unknown")
                .hasScore("64.0")
                .up()
                .up()
                .up()
                .report();
    }

    @Test
    void continuousVariableResourceMeasureObservationEncounterBasisSum() {
        final LocalDate measurementPeriodStart = LocalDate.of(2024, 1, 1);

        final int expectedAgeStratum1 = computeAge(measurementPeriodStart, LocalDate.of(1940, Month.JANUARY, 1));
        final int expectedAgeStratum2 = computeAge(measurementPeriodStart, LocalDate.of(1950, Month.JANUARY, 1));
        final int expectedAgeStratum3 = computeAge(measurementPeriodStart, LocalDate.of(1960, Month.JANUARY, 1));

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
                .stratifierById("stratifier-age")
                .stratumCount(3)
                .firstStratum()
                .hasValue(Integer.toString(expectedAgeStratum1))
                .hasScore("240.0")
                .up()
                .stratumByPosition(2)
                .hasValue(Integer.toString(expectedAgeStratum2))
                .hasScore("1920.0")
                .up()
                .stratumByPosition(3)
                .hasValue(Integer.toString(expectedAgeStratum3))
                .hasScore("376.0")
                .up()
                .up()
                .up()
                .report();
    }

    int computeAge(LocalDate measurementPeriod, LocalDate birthDate) {
        return Period.between(birthDate, measurementPeriod).getYears();
    }
}
