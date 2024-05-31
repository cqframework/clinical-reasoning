package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

class DataRequirementsTest {
    @Test
    void dataRequirements_resourceBasisMeasure_withMeasurementPeriod() {
        DataRequirements.Given given = DataRequirements.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .DataRequirements()
                .then()
                .hasDataRequirementCount(28)
                .hasParameterDefCount(10)
                .hasRelatedArtifactCount(24)
                .report();
    }

    @Test
    void dataRequirements_resourceBasisMeasure_partialMeasurementPeriod() {
        DataRequirements.Given given = DataRequirements.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .periodEnd("2020-01-01")
                .DataRequirements()
                .then()
                .hasDataRequirementCount(29)
                .hasParameterDefCount(16)
                .hasRelatedArtifactCount(35)
                .report();
    }

    @Test
    void dataRequirements_resourceBasisMeasure_noPeriod() {
        DataRequirements.Given given = DataRequirements.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .DataRequirements()
                .then()
                .hasDataRequirementCount(29)
                .hasParameterDefCount(16)
                .hasRelatedArtifactCount(35)
                .report();
    }

    @Test
    void dataRequirements_booleanBasisMeasure_noPeriod() {
        DataRequirements.Given given = DataRequirements.given().repositoryFor("MinimalMeasureEvaluation");
        given.when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .DataRequirements()
                .then()
                .hasDataRequirementCount(1)
                .hasParameterDefCount(10)
                .hasRelatedArtifactCount(1)
                .report();
    }

    @Test
    void dataRequirements_booleanBasisMeasure_Period() {
        DataRequirements.Given given = DataRequirements.given().repositoryFor("MinimalMeasureEvaluation");
        given.when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .DataRequirements()
                .then()
                .hasDataRequirementCount(1)
                .hasParameterDefCount(7)
                .hasRelatedArtifactCount(0)
                .report();
    }

    @Test
    void dataRequirements_booleanBasisMeasure_badLibraryUrl() {
        DataRequirements.Given given = DataRequirements.given().repositoryFor("MinimalMeasureEvaluation");
        var when = given.when()
                .measureId("MinimalCohortBooleanBasisSingleGroup_badLibraryUrl")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .DataRequirements();

        assertThrows(ResourceNotFoundException.class, () -> when.then().report());
    }

    @Test
    void dataRequirements_booleanBasisMeasure_Canonical() {
        DataRequirements.Given given = DataRequirements.given().repositoryFor("MinimalMeasureEvaluation");
        given.when()
                .measureId("MinimalCohortBooleanBasisSingleGroupCanonical")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .DataRequirements()
                .then()
                .hasDataRequirementCount(1)
                .hasParameterDefCount(1)
                .hasRelatedArtifactCount(0)
                .report();
    }
}
