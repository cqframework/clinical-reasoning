package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.dstu3.Measure.Given;

// This class has tests that verify success behavior for minimally defined measures.
// It incrementally builds up the measure definition.
class MinimalMeasureTest {

    private static final Given GIVEN_MINIMAL_MEASURE_REPO = Measure.given().repositoryFor("MinimalMeasure");

    @Test
    void evaluateSucceedsWithMinimalMeasure() {
        var when = GIVEN_MINIMAL_MEASURE_REPO.when().measureId("Minimal").evaluate();

        var report = when.then().report();
        assertNotNull(report);
        assertEquals(0, report.getGroup().size());
    }

    @Test
    void evaluateSucceedsWithMinimalWithGroupMeasure() {
        var when =
                GIVEN_MINIMAL_MEASURE_REPO.when().measureId("MinimalWithGroup").evaluate();

        var report = when.then().report();
        assertNotNull(report);
        assertEquals(0, report.getGroup().size());
        assertEquals(0, report.getGroupFirstRep().getPopulation().size());
    }

    @Test
    void evaluateSucceedsWithMinimalWithPopulationMeasure() {
        var when = GIVEN_MINIMAL_MEASURE_REPO
                .when()
                .measureId("MinimalWithPopulation")
                .evaluate();

        var report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());
        assertEquals(1, report.getGroupFirstRep().getPopulation().size());
        assertEquals(0, report.getGroupFirstRep().getPopulationFirstRep().getCount());
    }

    @Test
    void evaluateSucceedsWithMinimalWithParameterMeasure() {
        var when = GIVEN_MINIMAL_MEASURE_REPO
                .when()
                .measureId("MinimalWithParameter")
                .evaluate();

        var report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());
        assertEquals(1, report.getGroupFirstRep().getPopulation().size());
        assertEquals(0, report.getGroupFirstRep().getPopulationFirstRep().getCount());
    }
}
