package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;

/**
 * Phase 1 of the engine-5 retrieve/conversion performance work: determine whether enabling the
 * engine's {@code DisableEvaluatedResourcesCollection} option (a memory lever) is safe for measure
 * evaluation.
 *
 * <p>The handoff proposing the option assumed the engine's evaluated-resources set is "a pure
 * retention root" that measure eval does not read. That is <b>not</b> true in clinical-reasoning:
 * {@code MeasureEvaluator} pulls {@code ExpressionResult.evaluatedResources()} into each
 * {@code PopulationDef}, and {@code R4MeasureReportBuilder} emits them as
 * {@code MeasureReport.evaluatedResource} (plus per-population and per-SDE references).
 *
 * <p>This test pins the answer empirically. <b>Finding (engine 5.7.0):</b> with the collection
 * disabled, {@code MeasureReport.evaluatedResource} drops from 2 to 0 — measure eval's
 * per-expression evaluated-resources are sourced from the same global collection the option
 * disables. Enabling {@code DisableEvaluatedResourcesCollection} is therefore a <b>functional
 * regression</b> for measure evaluation, not the free memory win the handoff assumed. The option
 * must stay off for measure eval until the engine exposes per-expression evaluated-resources
 * independently of the global collection.
 *
 * <p>This is a characterization/guard test: it locks in the current (regressive) behavior. If a
 * future engine change decouples the two, the {@code disabled == 0} assertion below will start
 * failing — that is the signal to revisit enabling the option for measure eval.
 */
@SuppressWarnings("squid:S2699")
class EvaluatedResourcesCollectionToggleTest {

    private static final String REPOSITORY = "MeasureTest";
    private static final String MEASURE_ID = "ProportionResourceAllPopulations";
    private static final String SUBJECT = "Patient/patient-9";

    private static MeasureEvaluationOptions optionsWithCollectionDisabled(boolean disabled) {
        var options = MeasureEvaluationOptions.defaultOptions();
        // Mirror the defaults the Measure.Given harness applies, so the only variable is the toggle.
        options.getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
        options.getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        options.getEvaluationSettings()
                .getCqlOptions()
                .getCqlEngineOptions()
                .setEvaluatedResourcesCollectionDisabled(disabled);
        return options;
    }

    private static MeasureReport evaluate(boolean collectionDisabled) {
        return evaluate(MEASURE_ID, collectionDisabled);
    }

    private static MeasureReport evaluate(String measureId, boolean collectionDisabled) {
        return Measure.given()
                .repositoryFor(REPOSITORY)
                .evaluationOptions(optionsWithCollectionDisabled(collectionDisabled))
                .when()
                .measureId(measureId)
                .subject(SUBJECT)
                .reportType("subject")
                .evaluate()
                .then()
                .measureReport();
    }

    /** SDE supplemental-data observation value codes (e.g. "M"), from the report's contained resources. */
    private static List<String> sdeObservationValueCodes(MeasureReport report) {
        return report.getContained().stream()
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .filter(Observation::hasValueCodeableConcept)
                .map(o -> o.getValueCodeableConcept().getCodingFirstRep().getCode())
                .sorted()
                .collect(Collectors.toList());
    }

    @Test
    void disablingEvaluatedResourcesCollectionEmptiesMeasureReportOutput() {
        var baseline = evaluate(false).getEvaluatedResource().size();
        var disabled = evaluate(true).getEvaluatedResource().size();

        // Baseline sanity: this measure/subject produces evaluated resources (matches
        // MeasureReportTypeIndividualTest#hasEvaluatedResourceCount(2)).
        assertTrue(baseline > 0, "baseline measure eval should produce evaluatedResource references");
        assertEquals(2, baseline, "baseline evaluatedResource count regressed from the known-good value of 2");

        // Finding: disabling the engine's global collection empties CR's evaluatedResource output.
        // When this assertion starts failing (disabled != 0), the engine has decoupled per-expression
        // evaluated resources and enabling the option for measure eval can be reconsidered.
        assertEquals(
                0,
                disabled,
                "expected disabling DisableEvaluatedResourcesCollection to empty MeasureReport.evaluatedResource "
                        + "(engine-5.7.0 behavior); got " + disabled
                        + " — the engine may now source per-expression evaluated resources independently");
    }

    /**
     * Critical for consumers like link-cloud: disabling the collection drops only the <b>provenance</b>
     * (which source resources were evaluated), NOT the computed SDE <b>values</b>. The supplemental-data
     * observations are built from the SDE expression result ({@code SdeDef.getAccumulatedValues()} ←
     * {@code result.nonNullValues()}), which is independent of the evaluated-resources tracking the flag
     * controls ({@code SdeDef.getAllEvaluatedResources()} ← {@code result.evaluatedResources()}).
     *
     * <p>So turning the flag on does NOT "turn off SDE": the SDE observation values survive unchanged;
     * only the per-SDE evaluated-resource references (provenance) are omitted.
     */
    @Test
    void disablingCollectionPreservesSdeValuesAndOnlyDropsProvenance() {
        var sdeMeasure = "CohortBooleanSDESingleValue";

        var baseline = evaluate(sdeMeasure, false);
        var disabled = evaluate(sdeMeasure, true);

        var baselineSdeValues = sdeObservationValueCodes(baseline);
        var disabledSdeValues = sdeObservationValueCodes(disabled);

        // Baseline sanity: this SDE measure emits a supplemental-data observation with value "M".
        assertEquals(List.of("M"), baselineSdeValues, "baseline SDE observation value");

        // The reassuring result: SDE VALUES are fully preserved with the collection disabled...
        assertEquals(
                baselineSdeValues,
                disabledSdeValues,
                "disabling evaluated-resources collection must NOT change SDE observation values");

        // ...even though the evaluatedResource PROVENANCE on the report is dropped.
        assertTrue(baseline.getEvaluatedResource().size() > 0, "baseline should carry evaluatedResource provenance");
        assertEquals(
                0,
                disabled.getEvaluatedResource().size(),
                "disabling collection drops evaluatedResource provenance (expected), while SDE values remain");
    }
}
