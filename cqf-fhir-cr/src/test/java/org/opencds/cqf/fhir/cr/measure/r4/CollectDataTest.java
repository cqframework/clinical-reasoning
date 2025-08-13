package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.CollectData.Given;

@SuppressWarnings("squid:S2699")
class CollectDataTest {

    @Test
    void collectData_booleanBasisMeasure_subject() {
        Given given = CollectData.given().repositoryFor("CaseRepresentation101");
        given.when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart("2022-01-01")
                .periodEnd("2022-06-29")
                .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
                .collectData()
                .then()
                .hasParameterCount(13)
                .measureReport()
                .hasDataCollectionReportType()
                .hasEvaluatedResourceCount(12)
                .up()
                .report();
    }

    @Test
    void collectData_booleanBasisMeasure_population() {
        // only one patient, no subject specified
        CollectData.Given given = CollectData.given().repositoryFor("CaseRepresentation101");
        given.when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart("2022-01-01")
                .periodEnd("2022-06-29")
                .collectData()
                .then()
                .hasParameterCount(13)
                .hasMeasureReportCount(1)
                .report();
    }

    @Test
    void collectData_resourceBasisMeasure_subject() {
        CollectData.Given given = CollectData.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/numer-EXM104")
                .collectData()
                .then()
                .hasParameterCount(5)
                .measureReport()
                .hasDataCollectionReportType()
                .hasEvaluatedResourceCount(4)
                .up()
                .report();
    }

    @Test
    void collectData_resourceBasisMeasure_population() {
        CollectData.Given given = CollectData.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .collectData()
                .then()
                .hasParameterCount(17)
                .hasMeasureReportCount(4)
                .report();
    }

    @Test
    void collectData_resourceBasisMeasure_practitioner() {
        CollectData.Given given = CollectData.given().repositoryFor("MinimalMeasureEvaluation");
        given.when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .practitioner("Practitioner/tester")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .collectData()
                .then()
                .hasParameterCount(3)
                .hasMeasureReportCount(1)
                .report();
    }

    @Test
    void collectData_resourceBasisMeasure_practitioner_noPatients() {
        CollectData.Given given = CollectData.given().repositoryFor("MinimalMeasureEvaluation");
        given.when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .practitioner("Practitioner/empty")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .collectData()
                .then()
                .hasParameterCount(1)
                .hasMeasureReportCount(1)
                .report();
    }
}
