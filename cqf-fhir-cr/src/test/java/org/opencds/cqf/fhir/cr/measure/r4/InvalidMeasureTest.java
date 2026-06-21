package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureValidationException;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

// This class has tests that verify failure behavior for various types of invalid measures.
// The goal is for the DSTU3 and R4 implementations to have the same behavior for these tests.
class InvalidMeasureTest {

    private static final Given GIVEN_INVALID_MEASURE_REPO = Measure.given().repositoryFor("InvalidMeasure");

    @Test
    void evaluateThrowsErrorWithEmptyMeasure() {
        var when = GIVEN_INVALID_MEASURE_REPO.when().measureId("Empty").evaluate();
        var e = assertThrows(InvalidRequestException.class, when::then);
        assertTrue(e.getMessage().contains("does not have a primary library"));
    }

    @Test
    void evaluateThrowsErrorWhenLibraryUnavailable() {
        var when = GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("LibraryUnavailable")
                .evaluate();
        var e = assertThrows(MeasureValidationException.class, when::then);
        assertTrue(e.getMessage().contains("LIBRARY_NOT_FOUND"));
        assertTrue(e.getMessage().contains("was not found in the repository"));
    }

    @Test
    void evaluateThrowsErrorWhenLibraryIsMissingContent() {
        var when = GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("LibraryMissingContent")
                .evaluate();
        var e = assertThrows(CqlIncludeException.class, when::then);
        assertTrue(e.getMessage().contains("Could not load source for library"));
    }

    @Test
    void evaluateThrowsErrorWithDuplicatePopulationIds() {
        var when = GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("DuplicatePopulationIds")
                .evaluate();
        var e = assertThrows(InvalidRequestException.class, when::then);
        assertTrue(e.getMessage().contains("Duplicate population ID"));
        assertTrue(e.getMessage().contains("initial-population"));
        assertTrue(e.getMessage().contains("group-1"));
    }

    /**
     * Cohort encounter-basis measure whose CQL references a ValueSet that is not present in the
     * IG (no vocabulary/valueset entry). Pre-validation in {@link
     * org.opencds.cqf.fhir.cr.measure.common.MeasureLibraryPreValidator} resolves the library,
     * walks its ELM ValueSet refs, and surfaces the engine-level "Unable to locate ValueSet"
     * diagnostic as a contained OperationOutcome with status=ERROR — without ever entering the
     * per-subject CQL evaluation loop.
     */
    @Test
    void cohortEncounterMissingValueSet() {
        GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("CohortEncounterMissingValueSet")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Unable to locate ValueSet");
    }

    /**
     * Same library/CQL as {@link #cohortEncounterMissingValueSet()} with an added stratifier
     * referencing a simple scalar expression ("Patient Gender String"). Confirms the
     * unresolvable-ValueSet diagnostic still surfaces when an otherwise-valid scalar
     * stratifier is present.
     */
    @Test
    void cohortEncounterMissingValueSetWithScalarStratifier() {
        GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("CohortEncounterMissingValueSetStrat")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Unable to locate ValueSet");
    }

    /**
     * Variant of {@link #cohortEncounterMissingValueSetWithScalarStratifier()} where the
     * stratifier is expressed as a value/component stratifier (stratifier.component[].criteria)
     * referencing the same scalar expression instead of a top-level stratifier.criteria.
     */
    @Test
    void cohortEncounterMissingValueSetWithComponentStratifier() {
        GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("CohortEncounterMissingValueSetStratComponent")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Unable to locate ValueSet");
    }

    /**
     * Variant of {@link #cohortEncounterMissingValueSetWithComponentStratifier()} where the
     * component stratifier expression is a CQL function ("Encounter Status Function") taking an
     * Encounter parameter, rather than a non-function scalar expression. Without pre-validation,
     * this case used to mask the underlying CqlException with a higher-level "Expression result:
     * Initial Population is missing" diagnostic; pre-validation now drops the library before
     * function evaluation runs, so the engine-level diagnostic surfaces uniformly across all
     * stratifier shapes.
     */
    @Test
    void cohortEncounterMissingValueSetWithFunctionStratifier() {
        GIVEN_INVALID_MEASURE_REPO
                .when()
                .measureId("CohortEncounterMissingValueSetStratFunction")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Unable to locate ValueSet");
    }
}
