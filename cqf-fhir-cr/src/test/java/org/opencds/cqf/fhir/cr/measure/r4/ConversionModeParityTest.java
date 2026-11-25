package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.CONVERSION_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;

/**
 * Parity gate for opt-in lazy FHIR→CQL conversion (engine {@code lazyComposites}, wired through
 * {@link CONVERSION_MODE}). Proves that evaluating the same measure/subject with
 * {@code CONVERSION_MODE.LAZY} produces the same {@link MeasureReport} as {@code CONVERSION_MODE.EAGER}
 * — identical population counts, measure scores, supplemental-data (SDE) values and
 * {@code evaluatedResource} provenance. Lazy conversion must be a pure performance optimization with
 * no observable effect on results; this test compares the two conversion paths head-to-head
 * (explicit EAGER vs LAZY, not relying on the AUTO repository gate).
 */
@SuppressWarnings("squid:S2699")
class ConversionModeParityTest {

    private static final String REPOSITORY = "MeasureTest";
    private static final String SUBJECT = "Patient/patient-9";

    private static MeasureEvaluationOptions optionsFor(CONVERSION_MODE mode) {
        var options = MeasureEvaluationOptions.defaultOptions();
        options.getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY)
                .setConversionMode(mode);
        options.getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        return options;
    }

    private static MeasureReport evaluate(String measureId, CONVERSION_MODE mode) {
        return Measure.given()
                .repositoryFor(REPOSITORY)
                .evaluationOptions(optionsFor(mode))
                .when()
                .measureId(measureId)
                .subject(SUBJECT)
                .reportType("subject")
                .evaluate()
                .then()
                .measureReport();
    }

    private void assertLazyMatchesEager(String measureId) {
        var eager = evaluate(measureId, CONVERSION_MODE.EAGER);
        var lazy = evaluate(measureId, CONVERSION_MODE.LAZY);

        assertEquals(
                populationCounts(eager),
                populationCounts(lazy),
                "population counts must match between EAGER and LAZY for " + measureId);
        assertEquals(
                measureScores(eager),
                measureScores(lazy),
                "measure scores must match between EAGER and LAZY for " + measureId);
        assertEquals(
                sdeValueCodes(eager),
                sdeValueCodes(lazy),
                "SDE observation values must match between EAGER and LAZY for " + measureId);
        assertEquals(
                evaluatedResourceRefs(eager),
                evaluatedResourceRefs(lazy),
                "evaluatedResource provenance must match between EAGER and LAZY for " + measureId);
    }

    /** Proportion measure with all populations, a score, and evaluatedResource provenance. */
    @Test
    void proportionMeasureParity() {
        var eager = evaluate("ProportionResourceAllPopulations", CONVERSION_MODE.EAGER);
        // Baseline sanity: this measure/subject actually exercises the things we compare, so the
        // parity assertions below aren't vacuously comparing two empty reports.
        assertFalse(populationCounts(eager).isEmpty(), "expected populations to compare");
        assertFalse(evaluatedResourceRefs(eager).isEmpty(), "expected evaluatedResource provenance to compare");

        assertLazyMatchesEager("ProportionResourceAllPopulations");
    }

    /** SDE measure: verifies supplemental-data values are identical under lazy conversion. */
    @Test
    void sdeMeasureParity() {
        var eager = evaluate("CohortBooleanSDESingleValue", CONVERSION_MODE.EAGER);
        assertFalse(sdeValueCodes(eager).isEmpty(), "expected SDE values to compare");

        assertLazyMatchesEager("CohortBooleanSDESingleValue");
    }

    private static List<String> populationCounts(MeasureReport r) {
        return r.getGroup().stream()
                .flatMap(g -> g.getPopulation().stream())
                .map(p -> p.getCode().getCodingFirstRep().getCode() + "=" + p.getCount())
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> measureScores(MeasureReport r) {
        return r.getGroup().stream()
                .map(g -> g.hasMeasureScore() && g.getMeasureScore().hasValue()
                        ? g.getMeasureScore().getValue().toPlainString()
                        : "none")
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> sdeValueCodes(MeasureReport r) {
        return r.getContained().stream()
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .filter(Observation::hasValueCodeableConcept)
                .map(o -> o.getValueCodeableConcept().getCodingFirstRep().getCode())
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> evaluatedResourceRefs(MeasureReport r) {
        return r.getEvaluatedResource().stream()
                .map(ref -> ref.getReference())
                .sorted()
                .collect(Collectors.toList());
    }
}
