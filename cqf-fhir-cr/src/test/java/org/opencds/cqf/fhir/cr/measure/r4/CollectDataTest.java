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
                .periodStart("2022-01-01")
                .periodEnd("2022-06-29")
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

    // LUKETODO: fix this:
    /*
    java.lang.UnsupportedOperationException
	at java.base/java.util.AbstractList.add(AbstractList.java:155)
	at java.base/java.util.AbstractList.add(AbstractList.java:113)
	at org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor.lambda$resolveParameterMap$3(R4MeasureProcessor.java:428)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor.resolveParameterMap(R4MeasureProcessor.java:411)
	at org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor.setArgParameters(R4MeasureProcessor.java:379)
	at org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor.getLibraryEngine(R4MeasureProcessor.java:358)
	at org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor.buildLibraryIdEngineDetails(R4MeasureProcessor.java:274)
     */
    @Test
    void collectData_resourceBasisMeasure_population() {
        CollectData.Given given = CollectData.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
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
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
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
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .collectData()
                .then()
                .hasParameterCount(1)
                .hasMeasureReportCount(1)
                .report();
    }
}
