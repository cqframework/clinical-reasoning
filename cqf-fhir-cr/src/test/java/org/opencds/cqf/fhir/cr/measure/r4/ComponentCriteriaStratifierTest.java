package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.SelectedReport;

public class ComponentCriteriaStratifierTest {


    /*
    The logic would essentially intersect population results against the intersect of underlying stratifier expressions
        population=1/1/2024, 1/2/2024
            Components
                criteria stratifier 1
                    raw result: 2/1/2024, 1/2/2024
                criteria stratifier 2
                    raw result: 2/3/2024, 1/2/2024
                stratum population: 1/2/2024

        population=1/1/2024, 1/2/2024
            Components
                criteria stratifier 1
                    raw result: 2/1/2024, 3/2/2024
                criteria stratifier 2
                    raw result: 2/3/2024, 3/2/2024, 1/2/2024
                stratum population: empty (no overlap between components, that also intersected population)


     */


    /*
        enc1, enc2, enc3
            *
     */
    private static final Given GIVEN = Measure.given().repositoryFor("ComponentCriteriaStratifier");

    // LUKETODO:  use Justin's test naming convention for all tests

    @Test
    void withIntersection() {
        // LUKETODO:  Justin simple example 1

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierWithIntersection")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-feb1-jan2-feb3-jan2")
            .firstStratum()
            .hasPopulationCount(1)
            .up()
            .up()
            .up()
            .report();

        System.out.println("report = " + report);
    }

    @Test
    void noIntersection() {

        // LUKETODO:  Justin simple example 2
        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierNoIntersection")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-feb1-mar2-feb1-mar2-jan2")
            .firstStratum()
            .hasPopulationCount(0)
            .up()
            .up()
            .up()
            .report();

    }

    // LUKETODO:  boolean basis
    // LUKETODO:  encounter basis



}
