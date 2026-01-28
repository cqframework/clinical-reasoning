package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.When;

/**
 * Measure Stratifier Testing to validate Measure defined Stratifier elements and the resulting MeasureReport Stratifier elements
 * Mainly using a Cohort Measure as the example scoringType for simplicity of parsing results.
 *
 */
@SuppressWarnings("squid:S2699")
class MeasureStratifierTest {

    private static final Given GIVEN_MEASURE_STRATIFIER_TEST = Measure.given().repositoryFor("MeasureStratifierTest");
    private static final Given GIVEN_CRITERIA_BASED_STRAT_SIMPLE =
            Measure.given().repositoryFor("CriteriaBasedStratifiersSimple");
    private static final Given GIVEN_CRITERIA_BASED_STRAT_COMPLEX =
            Measure.given().repositoryFor("CriteriaBasedStratifiersComplex");
    private static final Given GIVEN_SIMPLE = Measure.given().repositoryFor("MeasureTest");
    /**
     * Boolean Basis Measure with Stratifier defined by component expression that results in CodeableConcept value of 'M' or 'F' for the Measure population. For 'Individual' reportType
     */
    @Test
    void cohortBooleanValueStratHasCodeIndResult() {
        var mCC = new CodeableConcept().setText("M");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratCode")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                // This is a value stratifier, which does not pull in the measure populations and
                // does not use the CQL expression for the code text
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
    void cohortBooleanValueStratHasCodeStratGender() {
        var mCC = new CodeableConcept().setText("M");
        var fCC = new CodeableConcept().setText("F");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratCode")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                // This is a value stratifier, which does not pull in the measure populations and
                // does not use the CQL expression for the code text
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
    void cohortBooleanValueStratHasBooleanNotFinished() {
        var isUnfinished = new CodeableConcept().setText("true");
        var notUnfinished = new CodeableConcept().setText("false");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratValue")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText("boolean strat not finished")
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
    void cohortBooleanValueStratMultiStratGenderAndBooleanNotFinished() {
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
                // This is a value stratifier, which does not pull in the measure populations and
                // does not use the CQL expression for the code text
                .hasCodeText("boolean strat not finished")
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
    void cohortBooleanValueStratNoIdStratInvalid() {
        final When evaluate = GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratNoId")
                .evaluate();
        try {
            evaluate.then();
            fail("should throw a missing Id scenario");
        } catch (InvalidRequestException e) {
            assertTrue(e.getMessage().contains("id is required on all Elements of type: Measure.group.stratifier"));
        }
    }

    // Previously, we didn't expect this to fail but with the new validation logic we decided that
    // it now ought to.
    @Test
    void cohortBooleanValueStratDifferentStratTypeFromBasisInvalid() {
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
    void cohortBooleanValueStratComponentStrat() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratComponent")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .group("group-1")
                .stratifierById("stratifier-1")
                // This is a value stratifier, which does not pull in the measure populations and
                // does not use the CQL expression for the code text
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
     * Ratio Measure with Resource (Encounter) Basis where Stratifier uses stratifier.component[].criteria
     * with a non-function expression "Age". This is classified as NON_SUBJECT_VALUE stratifier because:
     * - hasCriteria=false (no stratifier.criteria)
     * - hasAnyComponentCriteria=true (has stratifier.component[].criteria)
     * - isBooleanBasis=false (basis is Encounter)
     *
     * NON_SUBJECT_VALUE stratifiers MUST use CQL function definitions. The "Age" expression is a scalar,
     * not a function, so this should produce a validation error.
     */
    @Test
    void ratioResourceValueStratAge() {

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("RatioResourceStratValue")
                .evaluate()
                .then()
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "Non-subject value stratifier expression 'Age' must be a CQL function definition")
                .hasStatus(MeasureReportStatus.ERROR);
    }

    /**
     * Ratio Measure with Resource (Encounter) Basis where Stratifier is defined using stratifier.criteria
     * (making it a CRITERIA stratifier), but the expression "Encounter Status" returns status values
     * (strings) instead of Encounter resources.
     *
     * This is an INVALID measure configuration because:
     * - CRITERIA stratifiers must return resources matching the population basis
     * - Population basis is Encounter, but "Encounter Status" returns E.status (string values)
     *
     * The evaluation should capture the error in a contained OperationOutcome.
     */
    @Test
    void ratioResourceCriteriaStratifierWithInvalidReturnType() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("RatioResourceStratDifferentType")
                .evaluate()
                .then()
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "criteria-based stratifier is invalid for expression: [Encounter Status] due to mismatch between population basis: [Encounter]");
    }

    /**
     * Ratio Measure with Boolean Basis where Stratifier defined by expression that results in gender stratification for the Measure population.
     * intersection of results should be allowed
     */
    @Test
    void ratioBooleanValueStratGender() {

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
                // This is a value stratifier, because the expression it uses is based on age
                // and not on a resource, and does not set the code text.
                .hasCodeText("Gender Stratification")
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

    @Test
    void invalidStratifierTopLevelCriteriaEmptyComponent() {

        try {
            GIVEN_MEASURE_STRATIFIER_TEST
                    .when()
                    .measureId("InvalidStratifierTopLevelCriteriaEmptyComponent")
                    .evaluate()
                    .then();
        } catch (InvalidRequestException exception) {
            var exceptionMessage = exception.getMessage();

            assertEquals(
                    "Measure: http://example.com/Measure/InvalidStratifierTopLevelCriteriaEmptyComponent with stratifier: stratifier-ethnicity, has both components and stratifier criteria expressions defined. Only one should be specified",
                    exceptionMessage);
        }
    }

    /**
     * Cannot define a Stratifier with both component criteria and expression criteria
     * You can only define one or the other
     */
    @Test
    void cohortBooleanValueStratTwoStratifierCriteriaInvalid() {
        var when = GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratComponentInvalid")
                .evaluate();
        try {
            when.then();
            fail("should throw an exception");
        } catch (InvalidRequestException exception) {
            var exceptionMessage = exception.getMessage();
            assertEquals(
                    "Measure: http://example.com/Measure/CohortBooleanStratComponentInvalid with stratifier: stratifier-1, has both components and stratifier criteria expressions defined. Only one should be specified",
                    exceptionMessage);
        }
    }

    @Test
    void cohortBooleanCriteriaStratSinglePatientSingleEncounter() {
        GIVEN_CRITERIA_BASED_STRAT_SIMPLE
                .when()
                .measureId("CriteriaBasedStratifiersEncounterBasisSimple")
                .subject("Patient/patient1")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(4)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText("in-progress encounters resource")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(1)
                .population("initial-population")
                .hasCount(1);
    }

    /*
    This test proves that if measure evaluation returns empty stratum results that we'll no longer
    get error:  criteria-based stratifier is invalid for expression: ...
     */
    @Test
    void cohortEncounterCriteriaStratSinglePatientSingleEncounterZeroResultsOutsideOfPeriodNoError() {
        GIVEN_CRITERIA_BASED_STRAT_COMPLEX
                .when()
                .measureId("CriteriaBasedStratifiersComplex")
                .periodStart("2024-02-11")
                .periodEnd("2024-12-31")
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
                .hasCodeText("Encounters in Period")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(3)
                .population("initial-population")
                .hasCount(0)
                .up()
                .population("denominator")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(0);
    }

    @Test
    void cohortResourceCriteriaStratSingleBadExpressionForValueInvalid() {
        GIVEN_CRITERIA_BASED_STRAT_SIMPLE
                .when()
                .measureId("CriteriaBasedStratifiersEncounterBasisSimpleBad")
                .subject("Patient/patient1")
                .evaluate()
                .then()
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "criteria-based stratifier is invalid for expression: [bad criteria stratifier] due to mismatch between population basis: [Encounter] and result types: [Boolean] for measure URL: http://example.com/Measure/CriteriaBasedStratifiersEncounterBasisSimpleBad");
    }

    /*
    two patients
    9 total encounters
    1 encounter for patient 1 in-progress
    2 encounter for patient 2 in-progress
     */
    @Test
    void cohortResourceCriteriaStratAllPatientsTwoEncounters() {
        GIVEN_CRITERIA_BASED_STRAT_SIMPLE
                .when()
                .measureId("CriteriaBasedStratifiersEncounterBasisSimple")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(9)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText("in-progress encounters resource")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(3);
    }

    /*
    two patients
    9 total encounters
    1 encounter for patient 1 in-progress
    2 encounter for patient 2 in-progress
     */
    @Test
    void cohortCriteriaStratBooleanBasisAllPatientsTwoEncounters() {
        GIVEN_CRITERIA_BASED_STRAT_SIMPLE
                .when()
                .measureId("CriteriaBasedStratifiersBooleanBasisSimple")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText("bad criteria stratifier")
                .hasStratumCount(1)
                .firstStratum()
                .hasPopulationCount(1)
                .firstPopulation()
                .hasCount(0);
    }

    @Test
    void ratioResourceCriteriaStratComplexSetsDifferentForInitialDenominatorAndNumerator() {
        GIVEN_CRITERIA_BASED_STRAT_COMPLEX
                .when()
                .measureId("CriteriaBasedStratifiersComplex")
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
                .hasCodeText("Encounters in Period")
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
    void invalidStratifierCriteriaAndComponentCriteria() {

        try {
            GIVEN_CRITERIA_BASED_STRAT_SIMPLE
                    .when()
                    .measureId("InvalidStratifierCriteriaAndComponentCriteria")
                    .evaluate()
                    .then();
        } catch (InvalidRequestException exception) {
            var exceptionMessage = exception.getMessage();
            assertEquals(
                    "Measure: http://example.com/Measure/InvalidStratifierCriteriaAndComponentCriteria with stratifier: stratifier-1, has both components and stratifier criteria expressions defined. Only one should be specified",
                    exceptionMessage);
        }
    }

    @Test
    void cohortBooleanValueStratExpressionNonBoolean() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratValueNonBoolean")
                .evaluate()
                .then()
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "Exception for subjectId: Patient/patient-8, Message: stratifier expression criteria results for expression: [Encounters in Period] must fall within accepted types for population-basis: [boolean] for Measure: [http://example.com/Measure/CohortBooleanStratValueNonBoolean] due to mismatch between total eval result classes: [Encounter] and matching result classes: []");
    }

    @Test
    void measureWithCriteriaExtensionRatioResourceCriteriaStratComplexSetsDifferentForInitialDenominatorAndNumerator() {

        GIVEN_CRITERIA_BASED_STRAT_COMPLEX
                .when()
                .measureId("CriteriaBasedStratifiersComplexWithExtension")
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
                .hasCodeText("Encounters in Period")
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
    void measureWithCriteriaExtensionDifferentThanCohortBooleanValueStratHasCodeIndResult() {
        var mCC = new CodeableConcept().setText("M");

        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanStratCodeWithExtension")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                // This is a value stratifier, which does not pull in the measure populations and
                // does not use the CQL expression for the code text
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
     * Resource (Encounter) Basis Measure with multi-component value stratifier.
     * Components: Age Range Stratifier (function) + Encounter Status Stratifier (function)
     *
     * Patient-9 has:
     * - Encounter 1: status='finished', period 2024-01-01 (age ~36 -> P21Y--P41Y)
     * - Encounter 2: status='in-progress', period 2024-01-01 (age ~36 -> P21Y--P41Y)
     *
     * Expected strata (grouped by component value combinations):
     * - Stratum 1: P21Y--P41Y + finished -> count=1 (encounter-1)
     * - Stratum 2: P21Y--P41Y + in-progress -> count=1 (encounter-2)
     */
    @Test
    void cohortResourceValueStrat() {

        GIVEN_SIMPLE
                .when()
                .measureId("CohortResourceAllPopulationsValueStrat")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText("Encounter Age and Status")
                // 2 strata: one for each unique (AgeRange, Status) combination
                .hasStratumCount(2)
                // Stratum for "P21Y--P41Y + finished" - identified by unique "finished" status
                .stratumByComponentValueText("finished")
                .hasComponentStratifierCount(2) // two components: age range + status
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                // Stratum for "P21Y--P41Y + in-progress" - identified by unique "in-progress" status
                .stratumByComponentValueText("in-progress")
                .hasComponentStratifierCount(2) // two components: age range + status
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    @Test
    void cohortResourceValueStratNull() {
        // Tests that stratifier functions returning empty list {} and null are handled gracefully.
        // For NON_SUBJECT_VALUE stratifiers, each encounter is processed independently.
        // Patient-9 has 2 encounters, and both Empty Function and Null Function are evaluated per encounter.
        // The resulting strata group encounters by their component value combinations (empty, null).
        // Since all encounters produce the same values, we expect 1 stratum with both encounters.
        var report = GIVEN_SIMPLE
                .when()
                .measureId("CohortResourceAllPopulationsValueStratNull")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .report();

        // Debug: print stratum count and values
        var stratifier = report.getGroupFirstRep().getStratifierFirstRep();
        int stratumCount = stratifier.getStratum().size();
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("Stratum count: ").append(stratumCount).append("\n");
        for (var stratum : stratifier.getStratum()) {
            debugInfo.append("Stratum components:\n");
            for (var comp : stratum.getComponent()) {
                debugInfo
                        .append("  Code: ")
                        .append(comp.getCode().getText())
                        .append(", Value: ")
                        .append(comp.getValue().getText())
                        .append("\n");
            }
            debugInfo
                    .append("  Population count: ")
                    .append(stratum.getPopulationFirstRep().getCount())
                    .append("\n");
        }

        // Assert expected values - there should be 1 stratum with component values "empty" and "null"
        assertEquals(1, stratumCount, "Expected 1 stratum but got: " + stratumCount + "\n" + debugInfo);

        var singleStratum = stratifier.getStratum().get(0);
        assertEquals(2, singleStratum.getComponent().size(), "Expected 2 components");

        // Verify component values
        var compValues = singleStratum.getComponent().stream()
                .map(c -> c.getValue().getText())
                .sorted()
                .toList();
        assertEquals(
                java.util.List.of("empty", "null"),
                compValues,
                "Expected component values 'empty' and 'null' but got: " + compValues);

        // Population count should be 2 (one for each encounter)
        assertEquals(
                2,
                singleStratum.getPopulationFirstRep().getCount(),
                "Expected population count of 2 (for 2 encounters)");
    }

    /**
     * VALUE stratifier that returns List<EncounterStatus> should expand
     * into multiple strata. Patient-9 has 2 encounters (finished, in-progress),
     * so the list expansion should create 2 strata with count=1 each.
     * <p/>
     * This tests the multi-value expansion feature in nonComponentStratumPlural().
     */
    @Test
    void cohortBooleanValueStratMultiValueList() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanMultiValueStrat")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText("encounter-statuses")
                .hasStratumCount(2) // One stratum per unique status
                .stratumByText("finished")
                .firstPopulation()
                .hasCount(1) // Patient-9 appears once in "finished"
                .up()
                .up()
                .stratumByText("in-progress")
                .firstPopulation()
                .hasCount(1) // Patient-9 appears once in "in-progress"
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Tests that multiple patients can contribute to the same stratum
     * when their list values overlap.
     * <p/>
     * Setup (based on test data):
     * - patient-0: [in-progress, finished] (2 encounters)
     * - patient-1: [in-progress, finished] (2 encounters)
     * - patient-2: [arrived] (1 encounter)
     * - patient-3: [arrived] (1 encounter)
     * - patient-4: [triaged] (1 encounter)
     * - patient-5: [triaged] (1 encounter)
     * - patient-6: [cancelled] (1 encounter)
     * - patient-7: [cancelled] (1 encounter)
     * - patient-8: [finished] (1 encounter)
     * - patient-9: [finished, in-progress] (2 encounters)
     * <p/>
     * Expected strata:
     * - "finished": count=4 (patient-0, patient-1, patient-8, patient-9)
     * - "in-progress": count=3 (patient-0, patient-1, patient-9)
     * - "arrived": count=2 (patient-2, patient-3)
     * - "triaged": count=2 (patient-4, patient-5)
     * - "cancelled": count=2 (patient-6, patient-7)
     */
    @Test
    void cohortBooleanValueStratMultiValueListOverlapping() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanMultiValueStrat")
                .evaluate() // All patients
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText("encounter-statuses")
                // Should have strata for each unique encounter status across all patients
                .hasStratumCount(5) // finished, in-progress, arrived, cancelled, triaged
                .stratumByText("finished")
                .firstPopulation()
                .hasCount(4) // patient-0, patient-1, patient-8, patient-9 all have finished encounters
                .up()
                .up()
                .stratumByText("in-progress")
                .firstPopulation()
                .hasCount(3) // patient-0, patient-1, patient-9 have in-progress
                .up()
                .up()
                .stratumByText("arrived")
                .firstPopulation()
                .hasCount(2) // patient-2, patient-3
                .up()
                .up()
                .stratumByText("arrived")
                .firstPopulation()
                .hasCount(2) // patient-2, patient-3
                .up()
                .up()
                .stratumByText("triaged")
                .firstPopulation()
                .hasCount(2) // patient-4, patient-5
                .up()
                .up()
                .stratumByText("cancelled")
                .firstPopulation()
                .hasCount(2); // patient-7, patient-7
    }

    /**
     * Tests that single-element list behaves identically to scalar value.
     * Patient-2 has only one encounter (status: arrived), so should produce one stratum.
     */
    @Test
    void cohortBooleanValueStratSingleElementList() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanMultiValueStrat")
                .subject("Patient/patient-2")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasCodeText("encounter-statuses")
                .hasStratumCount(1)
                .stratumByText("arrived")
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Tests that empty list results in no stratum for that subject.
     * Patient with no encounters should not contribute to any stratum.
     */
    @Test
    void cohortBooleanValueStratEmptyList() {
        GIVEN_MEASURE_STRATIFIER_TEST
                .when()
                .measureId("CohortBooleanMultiValueStrat")
                .subject("Patient/patient-no-encounters")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .hasStratumCount(0); // No strata when list is empty
    }
}
