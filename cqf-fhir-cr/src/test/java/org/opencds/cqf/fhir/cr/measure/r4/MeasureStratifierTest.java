package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.SelectedReport;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Measure Stratifier Testing to validate Measure defined Stratifier elements and the resulting MeasureReport Stratifier elements
 * Mainly using a Cohort Measure as the example scoringType for simplicity of parsing results.
 *
 */
@SuppressWarnings("squid:S2699")
class MeasureStratifierTest {
    private static final Logger logger = LoggerFactory.getLogger(MeasureStratifierTest.class);

    private static final Given GIVEN_MEASURE_STRATIFIER_TEST = Measure.given().repositoryFor("MeasureStratifierTest");
    private static final Given GIVEN_CRITERIA_BASED_STRAT_SIMPLE =
            Measure.given().repositoryFor("CriteriaBasedStratifiersSimple");
    private static final Given GIVEN_CRITERIA_BASED_STRAT_COMPLEX =
            Measure.given().repositoryFor("CriteriaBasedStratifiersComplex");

    /**
     * Boolean Basis Measure with Stratifier defined by component expression that results in CodeableConcept value of 'M' or 'F' for the Measure population. For 'Individual' reportType
     */
    @Test
    void cohortBooleanHasCodeStratIndividualResult() {
        var mCC = new CodeableConcept().setText("M");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratCode")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText("stratifier-sex")
                .hasStratumCount(1)
                .stratum(mCC)
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Stratifier defined by component expression that results in CodeableConcept value of 'M' or 'F' for the Measure population.
     */
    @Test
    void cohortBooleanHasCodeStrat() {
        var mCC = new CodeableConcept().setText("M");
        var fCC = new CodeableConcept().setText("F");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratCode")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText("stratifier-sex")
                .hasStratumCount(2)
                .stratum(mCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .stratum(fCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Stratifier defined by value expression that results in CodeableConcept value of 'true' or 'false' for the Measure population.
     */
    @Test
    void cohortBooleanValueStrat() {
        var isUnfinished = new CodeableConcept().setText("true");
        var notUnfinished = new CodeableConcept().setText("false");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratValue")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText(null)
                .hasStratumCount(2)
                .stratum(isUnfinished)
                .firstPopulation()
                .hasCount(9)
                .up()
                .up()
                .stratum(notUnfinished)
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Multiple Stratifiers defined by value expression & component expression.
     * Each Stratifier should have Stratum for varied results.
     * stratifier 1: 'true' or 'false' for the Measure population.
     * stratifier 2: 'M' or 'F' for the Measure population.
     */
    @Test
    void cohortBooleanMultiStrat() {
        var isUnfinished = new CodeableConcept().setText("true");
        var notUnfinished = new CodeableConcept().setText("false");
        var mCC = new CodeableConcept().setText("M");
        var fCC = new CodeableConcept().setText("F");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratMulti")
                .evaluate()
                .then()
                .firstGroup()
                .stratifierById("stratifier-1")
                .hasCodeText(null)
                .hasStratumCount(2)
                .stratum(isUnfinished)
                .firstPopulation()
                .hasCount(9)
                .up()
                .up()
                .stratum(notUnfinished)
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .stratifierById("stratifier-2")
                .hasStratumCount(2)
                .stratum(mCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .stratum(fCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Stratifier defined without an 'id' populated. Result should throw an error.
     */
    @Test
    void cohortBooleanNoIdStrat() {
        final When evaluate = GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratNoId")
                .evaluate();
        try {
            evaluate.then();
            fail("should throw a missing Id scenario");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("id is required on all Elements of type: Measure.group.stratifier"));
        }
    }

    // Previously, we didn't expect this to fail but with the new validation logic we decided that
    // it now ought to.
    @Test
    void cohortBooleanDifferentTypeStrat() {
        try {
            GIVEN_MEASURE_STRATIFIER_TEST
                    .when()
                    .measureId("CohortBooleanStratDifferentType")
                    .evaluate()
                    .then();
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "stratifier expression criteria results for expression: [resource strat not finished] must fall within accepted types for boolean population basis: [boolean] for Measure: http://example.com/Measure/CohortBooleanStratDifferentType",
                    exception.getMessage());
        }
    }

    /**
     * Boolean Basis Measure with Stratifier defined as a component.
     * MultiComponent stratifiers blend results of multiple criteria (Gender of Patient and Payer, instead of just one or the other)
     * This is allowed within the specification, but is not currently implemented
     */
    @Test
    void cohortBooleanComponentStrat() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratComponent")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .group("group-1")
                .stratifierById("stratifier-1")
                .hasCodeText("Gender and Age")
                .hasStratumCount(2)
                .stratumByComponentCodeText("Age")
                .up()
                .stratumByComponentValueText("38")
                .hasComponentStratifierCount(2)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .stratumByComponentCodeText("Age")
                .up()
                .stratumByComponentValueText("35")
                .hasComponentStratifierCount(2)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Ratio Measure with Resource Basis where Stratifier defined by expression that results in two different ages.
     * Given that Population results are "Encounter" resources, intersection of results is based on subject
     * related Encounters where their age matches the stratifier criteria results
     */
    @Test
    void ratioResourceValueStrat() {

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("RatioResourceStratValue")
                .evaluate()
                .reportType("subject-list")
                .then()
                .subjectResultsValidation()
                .firstGroup()
                .stratifierById("stratifier-2")
                .hasCodeText(null)
                .stratum("35")
                .population("denominator")
                .hasCount(6)
                .hasStratumPopulationSubjectResults()
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .up()
                .stratum("38")
                .population("denominator")
                .hasCount(5)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Ratio Measure with Resource Basis where Stratifier defined by expression that results in Encounter.status per subject.
     * Multiple results for a single subject are allowed
     */
    @Test
    void ratioResourceDifferentTypeStratNotCriteriaBased() {
        // LUKETODO:  implement this
        final SelectedReport selectedReport = GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("RatioResourceStratDifferentType")
                .evaluate()
                .then();

        var jsonParser = FhirContext.forR4Cached().newJsonParser();

        logger.info(jsonParser.setPrettyPrint(true).encodeResourceToString(selectedReport.report()));

        selectedReport
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasMeasureScore(true)
                .hasScore("0.18181818181818182")
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText(null)
                .hasStratumCount(6)
                .stratum("triaged")
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(0)
                .up()
                .hasScore("0.0")
                .up()
                .stratum("arrived")
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(0)
                .up()
                .hasScore("0.0")
                .up()
                .stratum("cancelled")
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(0)
                .up()
                .hasScore("0.0")
                .up()
                .stratum("in-progress")
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(0)
                .up()
                .hasScore("0.0")
                .up()
                .stratum("finished")
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                // LUKETODO:  document this weirdness
                .stratum("in-progress,finished")
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("0.5");
    }

    /**
     * Ratio Measure with Boolean Basis where Stratifier defined by expression that results in gender stratification for the Measure population.
     * intersection of results should be allowed
     */
    @Test
    void ratioBooleanValueStrat() {

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("RatioBooleanStratValue")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .firstStratifier()
                .hasCodeText(null)
                .hasStratumCount(2)
                .stratum("M")
                .hasScore("0.2") // make sure stratum are scored
                .population("initial-population")
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Cannot define a Stratifier with both component criteria and expression criteria
     * You can only define one or the other
     */
    @Test
    void twoStatifierCriteria() {
        try {
            GIVEN_MEASURE_STRATIFIER_TEST
                    .when()
                    .measureId("CohortBooleanStratComponentInvalid")
                    .evaluate()
                    .then()
                    .report();
            fail("should throw an exception");
        } catch (InvalidRequestException exception) {
            assertTrue(
                    exception
                            .getMessage()
                            .contains(
                                    "Measure stratifier: stratifier-1, has both component and stratifier criteria expression defined. Only one should be specified"));
        }
    }

    @Test
    void criteriaBasedStratSinglePatientSingleEncounter() {
        GIVEN_CRITERIA_BASED_STRAT_SIMPLE
                .when()
                .measureId("CriteriaBasedStratifiersSimple")
                .subject("Patient/patient1")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText("in-progress encounters")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(1)
                .population("initial-population")
                .hasCount(1);
    }

    /*
    two patients
    9 total encounters
    1 encounter for patient 1 in-progress
    2 encounter for patient 2 in-progress
     */
    @Test
    void criteriaBasedStratAllPatientsTwoEncounters() {
        GIVEN_CRITERIA_BASED_STRAT_SIMPLE
                .when()
                .measureId("CriteriaBasedStratifiersSimple")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(9)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText("in-progress encounters")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(3);
    }

    @Disabled
    @Test
    void newnewnewnewmultiexpressionstratifiercriteriabasedtest() {
        // LUKETODO:   this test needs to be re-writter from the ground
        final SelectedReport selectedReport = GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("RatioResourceStratSameType")
                .evaluate()
                .then();

        var jsonParser = FhirContext.forR4Cached().newJsonParser();

        logger.info(jsonParser.setPrettyPrint(true).encodeResourceToString(selectedReport.report()));

        // LUKETODO:  add way more assertions
        // LUKETODO: write a similar test where the stratifier returns Encounters

        selectedReport
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasMeasureScore(true)
                .hasScore("0.18181818181818182")
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText("in-progress encounters")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(4)
                .up()
                .population("denominator")
                .hasCount(4)
                .up()
                .population("numerator")
                // LUKETODO:  make this work
                .hasCount(3);
    }
}
