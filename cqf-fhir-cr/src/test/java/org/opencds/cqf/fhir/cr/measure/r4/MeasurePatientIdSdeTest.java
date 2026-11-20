package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * Companion to {@link MeasurePatientIdStratifierTest} that demonstrates the supplemental-data-element
 * (SDE) approach to emitting the initial-population patient ids, rather than the stratifier approach.
 *
 * <p>The SDE expression re-uses the InitialPopulation logic and returns {@code Patient.id.value} only
 * for the members of the initial population ({@code if "InitialPopulation" then Patient.id.value else
 * null}). Patients outside the population evaluate to null and are dropped during SDE accumulation, so
 * they never appear in the report.
 *
 * <p>In a "population" (Summary) report each distinct SDE value becomes its own contained
 * {@code Observation}, whose code carries the patient id and whose value is the count (1 per id). Only
 * the 3 female patients are emitted; the 2 male patients are absent.
 */
@SuppressWarnings("squid:S2699")
class MeasurePatientIdSdeTest {

    private static final Given GIVEN_FEASIBILITY_SDE = Measure.given().repositoryFor("FeasibilityMeasureSde");

    @Test
    void cohortPatientIdSdePopulationReportListsInitialPopulationMembers() {
        GIVEN_FEASIBILITY_SDE
                .when()
                .measureId("FeasibilityMeasureSde")
                .reportType("population")
                .evaluate()
                .then()
                .hasReportType("Summary")
                .firstGroup()
                // Only the 3 female patients are in the cohort's initial population.
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                // One contained Observation per initial-population member (3 ids), and no others.
                .hasContainedResourceCount(3)
                .containedObservationsHaveMatchingExtension()
                // Each id appears as the Observation code, with a count of 1.
                .containedByCoding("patient-1")
                .observationCount(1)
                .up()
                .containedByCoding("patient-3")
                .observationCount(1)
                .up()
                .containedByCoding("patient-5")
                .observationCount(1)
                .up()
                // The report links each contained Observation via a supplementalData extension.
                .hasExtension(EXT_SDE_REFERENCE_URL, 3);
    }
}
