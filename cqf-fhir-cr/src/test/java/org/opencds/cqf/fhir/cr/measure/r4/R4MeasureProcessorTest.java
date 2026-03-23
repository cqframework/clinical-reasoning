package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;

class R4MeasureProcessorTest {
    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MinimalMeasureEvaluation");
    private static final MeasureDef MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP = MeasureDef.fromIdAndUrl(
            new IdType(ResourceType.Measure.name(), "MinimalCohortBooleanBasisSingleGroup"),
            "http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup");
    private static final String SUBJECT_ID = "Patient/female-1914";
    private static final String MEASURE_ID = "MinimalCohortBooleanBasisSingleGroup";
    private static final String MEASURE_URL = "http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup";

    record CaptureDefParams(
            String description,
            Function<IRepository, Either3<CanonicalType, IdType, Measure>> measureFactory,
            String reportType,
            MeasureEvalType evalType,
            List<String> subjectIds) {

        @Override
        public String toString() {
            return description;
        }
    }

    static Stream<CaptureDefParams> captureDefParams() {
        return Stream.of(
                new CaptureDefParams(
                        "idType_subject",
                        repo -> Eithers.forMiddle3(new IdType("Measure", MEASURE_ID)),
                        "subject",
                        MeasureEvalType.SUBJECT,
                        List.of(SUBJECT_ID)),
                new CaptureDefParams(
                        "measureResource_subject",
                        repo -> Eithers.forRight3(repo.read(Measure.class, new IdType("Measure", MEASURE_ID))),
                        "subject",
                        MeasureEvalType.SUBJECT,
                        List.of(SUBJECT_ID)),
                new CaptureDefParams(
                        "idType_nullEvalType_derivedFromReportType",
                        repo -> Eithers.forMiddle3(new IdType("Measure", MEASURE_ID)),
                        "subject",
                        null,
                        List.of(SUBJECT_ID)),
                new CaptureDefParams(
                        "idType_population",
                        repo -> Eithers.forMiddle3(new IdType("Measure", MEASURE_ID)),
                        "population",
                        MeasureEvalType.POPULATION,
                        List.of(SUBJECT_ID)));
    }

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
        var outcome = results.processMeasureForSuccessOrFailure(MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP);

        assertNotNull(outcome);

        var evaluationResult = outcome.results().get(SUBJECT_ID);
        assertNotNull(evaluationResult);

        var expressionResults = evaluationResult.getExpressionResults();
        assertNotNull(expressionResults);

        var expressionResult = expressionResults.get("Initial Population");
        assertNotNull(expressionResult);

        var evaluatedResources = expressionResult.getEvaluatedResources();
        assertNotNull(evaluatedResources);
        assertEquals(1, evaluatedResources.size());
    }

    @Test
    void evaluateMeasureIdWithCqlEngine() {
        var repository = GIVEN_REPO.getRepository();
        var r4MeasureProcessor = new R4MeasureProcessor(repository, MeasureEvaluationOptions.defaultOptions());
        var cqlEngine = Engines.forRepository(repository);

        var results = r4MeasureProcessor.evaluateMeasureIdWithCqlEngine(
                List.of(SUBJECT_ID), new IdType("Measure", MEASURE_ID), null, null, new Parameters(), cqlEngine);

        assertNotNull(results);
        var outcome = results.processMeasureForSuccessOrFailure(MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP);
        assertNotNull(outcome);

        var evaluationResult = outcome.results().get(SUBJECT_ID);
        assertNotNull(evaluationResult);

        var expressionResult = evaluationResult.getExpressionResults().get("Initial Population");
        assertNotNull(expressionResult);
        assertNotNull(expressionResult.getEvaluatedResources());
        assertEquals(1, expressionResult.getEvaluatedResources().size());
    }

    @Test
    void evaluateMeasureWithCqlEngine_withEither3() {
        var repository = GIVEN_REPO.getRepository();
        var r4MeasureProcessor = new R4MeasureProcessor(repository, MeasureEvaluationOptions.defaultOptions());
        var cqlEngine = Engines.forRepository(repository);

        Either3<CanonicalType, IdType, Measure> measureEither = Eithers.forMiddle3(new IdType("Measure", MEASURE_ID));

        var results = r4MeasureProcessor.evaluateMeasureWithCqlEngine(
                List.of(SUBJECT_ID), measureEither, null, null, new Parameters(), cqlEngine);

        assertNotNull(results);
        var outcome = results.processMeasureForSuccessOrFailure(MINIMAL_COHORT_BOOLEAN_BASIS_SINGLE_GROUP);
        assertNotNull(outcome);

        var evaluationResult = outcome.results().get(SUBJECT_ID);
        assertNotNull(evaluationResult);

        var expressionResult = evaluationResult.getExpressionResults().get("Initial Population");
        assertNotNull(expressionResult);
        assertNotNull(expressionResult.getEvaluatedResources());
        assertEquals(1, expressionResult.getEvaluatedResources().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("captureDefParams")
    void evaluateMeasureCaptureDef_withEither3(CaptureDefParams params) {
        var repository = GIVEN_REPO.getRepository();
        var r4MeasureProcessor = new R4MeasureProcessor(repository, MeasureEvaluationOptions.defaultOptions());
        var cqlEngine = Engines.forRepository(repository);

        // Phase 1: evaluate CQL to get composite results
        var compositeResults = r4MeasureProcessor.evaluateMeasureIdWithCqlEngine(
                params.subjectIds(), new IdType("Measure", MEASURE_ID), null, null, new Parameters(), cqlEngine);

        // Phase 2: capture def and report
        var result = r4MeasureProcessor.evaluateMeasureCaptureDef(
                params.measureFactory().apply(repository),
                null,
                null,
                params.reportType(),
                params.subjectIds(),
                params.evalType(),
                cqlEngine,
                compositeResults);

        assertNotNull(result.measureDef());
        assertNotNull(result.measureReport());
        assertEquals(MEASURE_URL, result.measureReport().getMeasure());
    }
}
