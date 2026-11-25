package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * Verifies the public {@link R4MultiMeasureService#evaluateWithDef} API returns the full evaluation
 * result — a populated {@code MeasureDef} and the per-subject CQL results — not just the
 * MeasureReport. This is the supported replacement for the split-package reflection workaround Link
 * uses to reach the previously package-private capture-def method.
 */
@SuppressWarnings("squid:S2699")
class R4MultiMeasureServiceEvaluateWithDefTest {

    private static final String REPOSITORY = "MeasureTest";
    private static final String MEASURE_ID = "ProportionResourceAllPopulations";
    private static final String SUBJECT = "Patient/patient-9";

    private static R4MultiMeasureService service() {
        var repository = new IgRepository(
                FhirContext.forR4Cached(),
                Path.of(getResourcePath(R4MultiMeasureServiceEvaluateWithDefTest.class) + "/" + Measure.CLASS_PATH + "/"
                        + REPOSITORY));
        var options = MeasureEvaluationOptions.defaultOptions();
        options.getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
        options.getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        return new R4MultiMeasureService(repository, options, "http://localhost", new MeasurePeriodValidator());
    }

    @Test
    void evaluateWithDefReturnsMeasureDefMeasureReportAndCqlResults() {
        var result = service()
                .evaluateWithDef(
                        new MeasureReference.ById(new IdType("Measure", MEASURE_ID)),
                        null,
                        null,
                        "subject",
                        SUBJECT,
                        null,
                        new Parameters(),
                        null,
                        null);

        // MeasureReport present — parity with the public evaluate() return.
        assertNotNull(result.measureReport(), "MeasureReport should be returned");

        // MeasureDef populated — the pre-scoring model Link reads (groups, url, errors).
        assertNotNull(result.measureDef(), "MeasureDef should be captured");
        assertFalse(result.measureDef().groups().isEmpty(), "MeasureDef should have populated groups");

        // Per-subject CQL results populated — what Link formats for debug output.
        assertFalse(result.evaluationResults().isEmpty(), "per-subject CQL evaluation results should be populated");
        assertEquals(1, result.evaluationResults().size(), "single-subject evaluation should yield one CQL result");
        assertNotNull(
                result.evaluationResults().values().iterator().next(),
                "the per-subject CqlEvaluationResult should be non-null");
    }
}
