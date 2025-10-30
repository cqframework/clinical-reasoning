package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
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
    void cohortDateComponentCriteriaStratWithIntersectionScenario1() {

        final MeasureReport report = GIVEN.when()
                .measureId("ComponentCriteriaStratifierDateBasisWithIntersectionScenario1")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .stratifierById("stratifier-feb1-jan2-feb3-jan2")
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    /*
       population=1/1/2024, 1/2/2024
           Components
               criteria stratifier 1
                   raw result: 2/1/2024, 3/2/2024
               criteria stratifier 2
                   raw result: 2/3/2024, 3/2/2024, 1/2/2024
               stratum population: NONE:  lack of intersection between components and population
    */
    @Test
    void cohortDateComponentCriteriaStratNoIntersectionScenario2() {

        final MeasureReport report = GIVEN.when()
                .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario2")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .stratifierById("stratifier-feb1-mar2-feb1-mar2-jan2")
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(0)
                .up()
                .up()
                .up()
                .up()
                .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortDateComponentCriteriaStratNoIntersectionScenario3() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario3")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-feb1-mar2-feb1-mar2-jan2")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(0)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortDateComponentCriteriaStratNoIntersectionScenario4() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario4")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-feb1-mar2-feb1-mar2-jan2")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(0)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortDateComponentCriteriaStratNoIntersectionScenario5() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario5")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-feb1-mar2-feb1-mar2-jan2")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(0)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortBooleanComponentCriteriaStratWithIntersectionScenario1() {

        final MeasureReport report = GIVEN.when()
                .measureId("ComponentCriteriaStratifierBooleanBasisWithIntersectionScenario1")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(2)
                .up()
                .hasStratifierCount(1)
                .stratifierById("stratifier-encounters-arrived-triaged-arrived-in-progress-boolean")
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortBooleanComponentCriteriaStratNoIntersectionScenario2() {

        final MeasureReport report = GIVEN.when()
                .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario2")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(2)
                .up()
                .hasStratifierCount(1)
                .stratifierById("stratifier-encounter-finished-in-progress-boolean")
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(0)
                .up()
                .up()
                .up()
                .up()
                .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortBooleanComponentCriteriaStratNoIntersectionScenario3() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario3")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(2)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-encounter-finished-in-progress-boolean")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(0)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortBooleanComponentCriteriaStratNoIntersectionScenario4() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario4")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(2)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-encounter-finished-in-progress-boolean")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(0)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortBooleanComponentCriteriaStratNoIntersectionScenario5() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario5")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(2)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-encounter-finished-in-progress-boolean")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(0)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortEncounterComponentCriteriaStratWithIntersectionScenario1() {

        final MeasureReport report = GIVEN.when()
                .measureId("ComponentCriteriaStratifierEncounterBasisWithIntersectionScenario1")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .stratifierById("stratifier-encounters-arrived-triaged-arrived-in-progress-resource")
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortEncounterComponentCriteriaStratNoIntersectionScenario2() {

        final MeasureReport report = GIVEN.when()
                .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario2")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .stratifierById("stratifier-encounter-finished-in-progress-encounter")
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasName("initial-population")
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortEncounterComponentCriteriaStratNoIntersectionScenario3() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario3")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-encounter-finished-in-progress-encounter")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(1)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }


    @Test
    void cohortEncounterComponentCriteriaStratNoIntersectionScenario4() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario4")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-encounter-finished-in-progress-encounter")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(1)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }


    @Test
    void cohortEncounterComponentCriteriaStratNoIntersectionScenario5() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario5")
            .evaluate()
            .then()
            .hasGroupCount(1)
            .firstGroup()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasCount(4)
            .up()
            .hasStratifierCount(1)
            .stratifierById("stratifier-encounter-finished-in-progress-encounter")
            .firstStratum()
            .hasPopulationCount(1)
            .firstPopulation()
            .hasName("initial-population")
            .hasCount(1)
            .up()
            .up()
            .up()
            .up()
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    // LUKETODO:  test that explicitly handles mismatches and asserts error handling:

//    9. 1 of n Component stratifier criteria expression has non-compliant population basis (population = Resource, Stratifier expression result is "String" or something). Throws error

    @Test
    void cohortBooleanComponentCriteriaStratPopulationStratExpressionMismatchEncounter() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierBooleanBasisMismatchEncounter")
            .evaluate()
            .then()
            .hasContainedOperationOutcome()
            .hasContainedOperationOutcomeMsg("Mismatch between population basis and stratifier criteria expression")
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortEncounterComponentCriteriaStratPopulationStratExpressionMismatchDate() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierEncounterBasisMismatchDate")
            .evaluate()
            .then()
            .hasContainedOperationOutcome()
            .hasContainedOperationOutcomeMsg("Mismatch between population basis and stratifier criteria expression")
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }

    @Test
    void cohortDateComponentCriteriaStratPopulationStratExpressionMismatchBoolean() {

        final MeasureReport report = GIVEN.when()
            .measureId("ComponentCriteriaStratifierDateBasisMismatchBoolean")
            .evaluate()
            .then()
            .hasContainedOperationOutcome()
            .hasContainedOperationOutcomeMsg("Mismatch between population basis and stratifier criteria expression")
            .report();

        System.out.println(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }
}
