package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

class R4MeasureProcessorTest {
    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MinimalMeasureEvaluation");
    private static final IdType MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP =
            new IdType(ResourceType.Measure.name(), "MinimalCohortBooleanBasisSingleGroup");
    private static final String SUBJECT_ID = "Patient/female-1914";

    // This test could probably be improved with better data and more assertions, but it's to
    // confirm that a method exposed for downstream works with reasonable sanity.
    @Test
    void evaluateMultiMeasureIdsWithCqlEngine() {
        var repository = GIVEN_REPO.getRepository();
        var engineInitializationContext = GIVEN_REPO.getEngineInitializationContext();
        var r4MeasureProcessor = new R4MeasureProcessor(
                repository,
                engineInitializationContext,
                MeasureEvaluationOptions.defaultOptions(),
                new MeasureProcessorUtils());

        var cqlEngine = Engines.forContext(engineInitializationContext);

        var results = r4MeasureProcessor.evaluateMultiMeasureIdsWithCqlEngine(
                List.of(SUBJECT_ID),
                List.of(MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP),
                null,
                null,
                new Parameters(),
                cqlEngine);

        assertNotNull(results);
        var measureDef = new MeasureDef("", "", "", List.of(), List.of());
        var evaluationResults =
                results.processMeasureForSuccessOrFailure(MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP, measureDef);

        assertNotNull(evaluationResults);

        var evaluationResult = evaluationResults.get(SUBJECT_ID);
        assertNotNull(evaluationResult);

        var expressionResults = evaluationResult.expressionResults;
        assertNotNull(expressionResults);

        var expressionResult = expressionResults.get("Initial Population");
        assertNotNull(expressionResult);

        var evaluatedResources = expressionResult.evaluatedResources();
        assertEquals(1, evaluatedResources.size());
    }
}
