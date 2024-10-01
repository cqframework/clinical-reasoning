package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.CollectData.Given;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

class CollectDataTest {

    @Test
    void collectData_booleanBasisMeasure_subject() {
        Given given = CollectData.given().repositoryFor("CaseRepresentation101");
        given.when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart(LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2022, Month.JUNE, 29).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
                .collectData()
                .then()
                .hasParameterCount(16) // 3 more?
                .measureReport()
                .hasDataCollectionReportType()
                .hasEvaluatedResourceCount(15) // 3 more?
                .up()
                .report();
    }

    @Test
    void collectData_booleanBasisMeasure_population() {
        // only one patient, no subject specified
        CollectData.Given given = CollectData.given().repositoryFor("CaseRepresentation101");
        given.when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart(LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2022, Month.JUNE, 29).atStartOfDay(ZoneId.systemDefault()))
                .collectData()
                .then()
                .hasParameterCount(16)
                .hasMeasureReportCount(1)
                .report();
    }

    @Test
    void collectData_resourceBasisMeasure_subject() {
        CollectData.Given given = CollectData.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
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
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .collectData()
                .then()
                .hasParameterCount(18)
                .hasMeasureReportCount(4)
                .report();
    }

    @Test
    void collectData_resourceBasisMeasure_practitioner() {
        CollectData.Given given = CollectData.given().repositoryFor("MinimalMeasureEvaluation");
        given.when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .practitioner("Practitioner/tester")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .collectData()
                .then()
                .hasParameterCount(2)
                .hasMeasureReportCount(1)
                .report();
    }

    @Test
    void collectData_resourceBasisMeasure_practitioner_noPatients() {
        CollectData.Given given = CollectData.given().repositoryFor("MinimalMeasureEvaluation");
        given.when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .practitioner("Practitioner/empty")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .collectData()
                .then()
                .hasParameterCount(1)
                .hasMeasureReportCount(1)
                .report();
    }
}
