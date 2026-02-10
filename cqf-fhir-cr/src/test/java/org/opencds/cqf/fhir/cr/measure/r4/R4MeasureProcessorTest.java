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
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

class R4MeasureProcessorTest {
    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MinimalMeasureEvaluation");
    private static final MeasureDef MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP = MeasureDef.fromIdAndUrl(
            new IdType(ResourceType.Measure.name(), "MinimalCohortBooleanBasisSingleGroup"),
            "http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup");
    private static final String SUBJECT_ID = "Patient/female-1914";

    // This test could probably be improved with better data and more assertions, but it's to
    // confirm that a method exposed for downstream works with reasonable sanity.
    @Test
    void evaluateMultiMeasureIdsWithCqlEngine() {
        var repository = GIVEN_REPO.getRepository();
        var r4MeasureProcessor = new R4MeasureProcessor(repository, MeasureEvaluationOptions.defaultOptions());

        var cqlEngine = Engines.forRepository(repository);

        var results = r4MeasureProcessor.evaluateMultiMeasureIdsWithCqlEngine(
                List.of(SUBJECT_ID),
                List.of(new IdType(MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP.id())),
                null,
                null,
                new Parameters(),
                cqlEngine);

        assertNotNull(results);
        var evaluationResults = results.processMeasureForSuccessOrFailure(MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP);

        assertNotNull(evaluationResults);

        var evaluationResult = evaluationResults.get(SUBJECT_ID);
        assertNotNull(evaluationResult);

        var expressionResults = evaluationResult.getExpressionResults();
        assertNotNull(expressionResults);

        var expressionResult = expressionResults.get("Initial Population");
        assertNotNull(expressionResult);

        var evaluatedResources = expressionResult.getEvaluatedResources();
        assertEquals(1, evaluatedResources.size());
    }
}
