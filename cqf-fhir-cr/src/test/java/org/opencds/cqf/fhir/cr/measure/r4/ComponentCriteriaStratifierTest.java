package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class ComponentCriteriaStratifierTest {

    private static final Given GIVEN = Measure.given().repositoryFor("ComponentCriteriaStratifier");

    /*
        population=1/1/2024, 1/2/2024
            Components
                criteria stratifier 1
                    raw result: 2/1/2024, 1/2/2024
                criteria stratifier 2
                    raw result: 2/3/2024, 1/2/2024
                stratum population: 1/2/2024
     */
    @Test
    void cohortDateComponentCriteriaStratWithIntersection() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierDateBasisWithIntersection")
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

    /*
        population=1/1/2024, 1/2/2024
            Components
                criteria stratifier 1
                    raw result: 2/1/2024, 1/2/2024
                criteria stratifier 2
                    raw result: 2/3/2024, 1/2/2024
                stratum population: 1/2/2024
     */
    @Test
    void cohortDateComponentCriteriaStratNoIntersection() {

        // LUKETODO:  Justin simple example 2
        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierDateBasisNoIntersection")
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

        System.out.println("report = " + report);
    }

    // LUKETODO:  boolean basis

    @Test
    void cohortBooleanComponentCriteriaStratWithIntersection() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierBooleanBasisWithIntersection")
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
    void cohortBooleanComponentCriteriaStratNoIntersection() {

        // LUKETODO:  Justin simple example 2
        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersection")
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

        System.out.println("report = " + report);
    }

    // LUKETODO:  encounter basis
}
