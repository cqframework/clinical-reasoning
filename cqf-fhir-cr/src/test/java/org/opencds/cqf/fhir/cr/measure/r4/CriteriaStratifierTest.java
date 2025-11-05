package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.SelectedReport;

public class CriteriaStratifierTest {

    private static final Given GIVEN_COMPONENT_CRITERIA_STRATIFIER_BOOLEAN_BASIS =
            Measure.given().repositoryFor("ComponentCriteriaStratifierBooleanBasis");
    private static final Given GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS =
            Measure.given().repositoryFor("ComponentCriteriaStratifierEncounterBasis");
    private static final Given GIVEN_VALUE_CRITERIA_STRATIFIER_BOOLEAN_BASIS =
            Measure.given().repositoryFor("CriteriaBasedStratifiersBooleanBasisComplex");
    private static final Given GIVEN_VALUE_CRITERIA_STRATIFIER_ENCOUNTER_BASIS =
            Measure.given().repositoryFor("CriteriaBasedStratifiersEncounterBasisComplex");

    @Nested
    class HappyPath {

        @Test
        void ratioResourceValueCriteriaStratComplexBooleanBasisSetsDifferentForInitialDenominatorAndNumerator() {
            final SelectedReport then = GIVEN_VALUE_CRITERIA_STRATIFIER_BOOLEAN_BASIS
                    .when()
                    .measureId("CriteriaBasedStratifiersBooleanBasisComplex")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(3)
                    .population("initial-population")
                    .hasCount(11)
                    .up()
                    .population("denominator")
                    .hasCount(8)
                    .up()
                    .population("numerator")
                    // due to apply scoring, we keep only those numerator encounters that are also in the denominator
                    .hasCount(5)
                    .up()
                    .hasMeasureScore(true)
                    .hasScore("0.625")
                    .hasStratifierCount(1)
                    .firstStratifier()
                    .hasCodeText("Encounters in Period Boolean")
                    .hasStratumCount(1)
                    .firstStratum()
                    .hasPopulationCount(3)
                    .population("initial-population")
                    .hasCount(3)
                    .up()
                    .population("denominator")
                    .hasCount(2)
                    .up()
                    .population("numerator")
                    .hasCount(1);
        }

        @Test
        void ratioResourceValueCriteriaStratComplexEncounterBasisSetsDifferentForInitialDenominatorAndNumerator() {
            GIVEN_VALUE_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("CriteriaBasedStratifiersEncounterBasisComplex")
                    .evaluate()
                    .then()
                    .hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(3)
                    .population("initial-population")
                    .hasCount(11)
                    .up()
                    .population("denominator")
                    .hasCount(8)
                    .up()
                    .population("numerator")
                    // due to apply scoring, we keep only those numerator encounters that are also in the denominator
                    .hasCount(5)
                    .up()
                    .hasMeasureScore(true)
                    .hasScore("0.625")
                    .hasStratifierCount(1)
                    .firstStratifier()
                    .hasCodeText("Encounters in Period Resource")
                    .hasStratumCount(1)
                    .firstStratum()
                    .hasPopulationCount(3)
                    .population("initial-population")
                    .hasCount(3)
                    .up()
                    .population("denominator")
                    .hasCount(2)
                    .up()
                    .population("numerator")
                    .hasCount(1);
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
        void cohortDateComponentCriteriaStratWithIntersectionScenario1() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierDateBasisWithIntersectionScenario1")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    // LUKETODO:  I have doubts about this:  if we have two subjects, and the initial-populations of two
                    // dates, is this 2 or 4?
                    .hasCount(2)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-feb1-jan2-feb3-jan2")
                    .hasStratumCount(1)
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

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario2")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    .hasCount(2)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-feb1-mar2-feb1-mar2-jan2")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortDateComponentCriteriaStratNoIntersectionScenario3() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario3")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    .hasCount(2)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-mar1-apr1-jan1-jan2")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortDateComponentCriteriaStratNoIntersectionScenario4() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario4")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    .hasCount(2)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-may1-jun1-may1-jun1")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortDateComponentCriteriaStratNoIntersectionScenario5() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierDateBasisNoIntersectionScenario5")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    .hasCount(2)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-jul1-aug1-sep1-oct1")
                    .hasStratumCount(1)
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
        }

        // LUKETODO:  while this test is technically correct, it doesn't capture any sort of exclusion
        // logic like the Encounter basis test.   We need a tighter pairing of encounters and
        // patients so the counts will differ between initial population and the stratum initial
        // population
        @Test
        void cohortBooleanComponentCriteriaStratWithIntersectionScenario1() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_BOOLEAN_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierBooleanBasisWithIntersectionScenario1")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    // 4 patients each linked to an encounter that's arrived or planned
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-arrived-triaged-arrived-in-progress-boolean")
                    .hasStratumCount(1)
                    .firstStratum()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    // The intersection is two patients, each linked to an arrived encounter
                    .hasCount(2)
                    .up()
                    .up()
                    .up()
                    .up()
                    .report();
        }

        @Test
        void cohortBooleanComponentCriteriaStratNoIntersectionScenario2() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_BOOLEAN_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario2")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounter-planned-triaged-arrived-cancelled-boolean")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortBooleanComponentCriteriaStratNoIntersectionScenario3() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_BOOLEAN_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario3")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-cancelled-finished-arrived-planned-boolean")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortBooleanComponentCriteriaStratNoIntersectionScenario4() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_BOOLEAN_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario4")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-cancelled-triaged-cancelled-triaged-boolean")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortBooleanComponentCriteriaStratNoIntersectionScenario5() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_BOOLEAN_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierBooleanBasisNoIntersectionScenario5")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-cancelled-in-progress-finished-triaged-boolean")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortEncounterComponentCriteriaStratWithIntersectionScenario1() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierEncounterBasisWithIntersectionScenario1")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-arrived-triaged-arrived-in-progress-resource")
                    .hasStratumCount(1)
                    .firstStratum()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasName("initial-population")
                    .hasCount(2)
                    .up()
                    .up()
                    .up()
                    .up()
                    .report();
        }

        @Test
        void cohortEncounterComponentCriteriaStratNoIntersectionScenario2() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario2")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-planned-triaged-arrived-cancelled")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortEncounterComponentCriteriaStratNoIntersectionScenario3() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario3")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-cancelled-finished-arrived-planned")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortEncounterComponentCriteriaStratNoIntersectionScenario4() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario4")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-cancelled-triaged-cancelled-triaged")
                    .hasStratumCount(1)
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
        }

        @Test
        void cohortEncounterComponentCriteriaStratNoIntersectionScenario5() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierEncounterBasisNoIntersectionScenario5")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasGroupCount(1)
                    .firstGroup()
                    .hasPopulationCount(1)
                    .firstPopulation()
                    .hasCount(4)
                    .up()
                    .hasStratifierCount(1)
                    .stratifierById("stratifier-encounters-cancelled-in-progress-finished-triaged")
                    .hasStratumCount(1)
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
        }
    }

    @Nested
    class FailurePath {

        @Test
        void cohortBooleanComponentCriteriaStratPopulationStratExpressionMismatchEncounter() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierBooleanBasisMismatchEncounter")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "Mismatch between population basis and stratifier criteria expression")
                    .report();
        }

        @Test
        void cohortEncounterComponentCriteriaStratPopulationStratExpressionMismatchDate() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierEncounterBasisMismatchDate")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "Mismatch between population basis and stratifier criteria expression")
                    .report();
        }

        @Test
        void cohortDateComponentCriteriaStratPopulationStratExpressionMismatchBoolean() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierDateBasisMismatchBoolean")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "Mismatch between population basis and stratifier criteria expression")
                    .report();
        }

        @Test
        void cohortBooleanComponentCriteriaStratPopulationInvalidExpressionName() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierBooleanBasisInvalidExpressionName")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "Mismatch between population basis and stratifier criteria expression")
                    .report();
        }

        @Test
        void cohortEncounterComponentCriteriaStratPopulationInvalidExpressionName() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierEncounterBasisInvalidExpressionName")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "Mismatch between population basis and stratifier criteria expression")
                    .report();
        }

        @Test
        void cohortDateComponentCriteriaStratPopulationInvalidExpressionName() {

            final SelectedReport then = GIVEN_COMPONENT_CRITERIA_STRATIFIER_ENCOUNTER_BASIS
                    .when()
                    .measureId("ComponentCriteriaStratifierDateBasisInvalidExpressionName")
                    .evaluate()
                    .then();

            System.out.println(FhirContext.forR4Cached()
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(then.report()));

            then.hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "Mismatch between population basis and stratifier criteria expression")
                    .report();
        }
    }
}
