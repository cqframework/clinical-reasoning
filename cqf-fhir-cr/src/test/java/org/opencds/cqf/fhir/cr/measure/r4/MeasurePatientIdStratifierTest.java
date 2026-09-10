package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * Integration-style test for a cohort Measure whose stratifier is driven by a CQL expression that
 * returns the Patient id ({@code define PatientIdStratifier: Patient.id.value}).
 *
 * <p>There are 5 patients in the repository, but the initial population CQL only selects the 3
 * female patients. A "population" (Summary) report still evaluates the stratifier for every subject,
 * so each patient id gets its own stratum: the 3 patients in the initial population have a count of
 * 1, and the 2 excluded patients have a count of 0.
 */
@SuppressWarnings("squid:S2699")
class MeasurePatientIdStratifierTest {

    private static final Given GIVEN_FEASIBILITY = Measure.given().repositoryFor("FeasibilityMeasure");

    @Test
    void cohortPatientIdStratifierPopulationReportOneStratumPerPatient() {
        GIVEN_FEASIBILITY
                .when()
                .measureId("FeasibilityMeasure")
                .reportType("population")
                .evaluate()
                .then()
                .hasReportType("Summary")
                .firstGroup()
                // Only the 3 female patients are in the cohort's initial population.
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .hasStratifierCount(1)
                .firstStratifier()
                .hasCodeText("PatientIdStratifier")
                // Every evaluated patient id gets its own stratum, including the 2 not in the population.
                .hasStratumCount(5)
                // The 3 patients in the initial population each have a count of 1.
                .stratumByComponentValueText("patient-1")
                .hasComponentValueText("patient-1")
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .up()
                .stratumByComponentValueText("patient-3")
                .hasComponentValueText("patient-3")
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .up()
                .stratumByComponentValueText("patient-5")
                .hasComponentValueText("patient-5")
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .up()
                // The 2 male patients are excluded from the population, so their strata have a count of 0.
                .stratumByComponentValueText("patient-2")
                .hasComponentValueText("patient-2")
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(0)
                .up()
                .up()
                .stratumByComponentValueText("patient-4")
                .hasComponentValueText("patient-4")
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(0);
    }
}
