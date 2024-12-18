package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;

public class MeasureAdditionalDataAndCustomOptionsTests {

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    void measure_eval_with_additional_data() {
        var additionalData = getAdditionalData();

        var report = Measure.given()
                .repositoryFor("AdditionalDataAndCustomOptions")
                .when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart("2022-01-01")
                .periodEnd("2022-01-31")
                .subject("Patient/AdditionalDataPatient")
                .additionalData(additionalData)
                .evaluate()
                .then()
                .hasMeasureVersion("0.000.01")
                .report();

        assertEquals("2022-01-01", FORMATTER.format(report.getPeriod().getStart()));
        assertEquals("2022-01-31", FORMATTER.format(report.getPeriod().getEnd()));
    }

    @Test
    void with_custom_options() {
        var evaluationOptions = MeasureEvaluationOptions.defaultOptions();
        evaluationOptions.getEvaluationSettings().setLibraryCache(new HashMap<>());
        evaluationOptions
                .getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

        evaluationOptions
                .getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

        var when = Measure.given()
                .repositoryFor("AdditionalDataAndCustomOptions")
                .evaluationOptions(evaluationOptions)
                .when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
                .periodStart("2022-01-01")
                .periodEnd("2022-06-29")
                .reportType("subject")
                .evaluate();

        assertNotNull(when.then().report());

        // Run again to find any issues with caching
        when.then().report();
    }

    @Test
    void additional_data_with_custom_options() {
        var evaluationOptions = MeasureEvaluationOptions.defaultOptions();
        evaluationOptions.getEvaluationSettings().setLibraryCache(new HashMap<>());
        evaluationOptions
                .getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

        evaluationOptions
                .getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

        Bundle additionalData = getAdditionalData();

        var when = Measure.given()
                .repositoryFor("AdditionalDataAndCustomOptions")
                .evaluationOptions(evaluationOptions)
                .when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart("2022-01-01")
                .periodEnd("2022-01-31")
                .subject("Patient/AdditionalDataPatient")
                .additionalData(additionalData)
                .evaluate();

        assertNotNull(when.then().report());

        // Run again to find any issues with caching
        when.then().report();
    }

    private Bundle getAdditionalData() {
        return (Bundle) FhirContext.forR4Cached()
                .newJsonParser()
                .parseResource(MeasureAdditionalDataAndCustomOptionsTests.class.getResourceAsStream(
                        "AdditionalDataAndCustomOptions/additionalData.json"));
    }
}
