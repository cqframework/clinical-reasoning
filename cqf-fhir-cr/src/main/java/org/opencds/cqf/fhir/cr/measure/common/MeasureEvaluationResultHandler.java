package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.EvaluationResults;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

/**
 * Exclusively responsible for calling CQL evaluation and collating the results among multiple
 * measure defs in a FHIR version agnostic way.
 */
public class MeasureEvaluationResultHandler {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluationResultHandler.class);

    private static final String EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE = "Exception for subjectId: %s, Message: %s";
    private static final int SUBJECT_LOG_INTERVAL = 50;

    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasureEvaluator measureEvaluator;
    private final MeasureReportDefScorer measureReportDefScorer = new MeasureReportDefScorer();

    public MeasureEvaluationResultHandler(
            MeasureEvaluationOptions measureEvaluationOptions, PopulationBasisValidator populationBasisValidator) {
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measureEvaluator = new MeasureEvaluator(populationBasisValidator);
    }

    /**
     * Method that consumes pre-generated CQL results into Measure defined fields that reference
     * associated CQL expressions This is meant to be called by CQL CLI.
     *
     * @param fhirContext           FHIR context for FHIR version
     * @param evalResultsPerSubject criteria expression evalResultsPerSubject
     * @param measureDef            Measure defined objects
     * @param measureEvalType       the type of evaluation algorithm to apply to Criteria results
     */
    public void processResults(
            FhirContext fhirContext,
            Map<String, EvaluationResult> evalResultsPerSubject,
            MeasureDef measureDef,
            @Nonnull MeasureEvalType measureEvalType) {

        // Populate MeasureDef using MeasureEvaluator
        for (Map.Entry<String, EvaluationResult> entry : evalResultsPerSubject.entrySet()) {
            // subject
            String subjectId = entry.getKey();
            var sub = getSubjectTypeAndId(subjectId);
            var subjectIdPart = sub.getRight();
            var subjectTypePart = sub.getLeft();
            EvaluationResult evalResult = entry.getValue();
            try {
                // populate CQL results into MeasureDef
                measureEvaluator.evaluate(
                        measureDef,
                        measureEvalType,
                        subjectTypePart,
                        subjectIdPart,
                        evalResult,
                        measureEvaluationOptions.getApplyScoringSetMembership());
            } catch (Exception e) {
                // Catch Exceptions from evaluation per subject, but allow rest of subjects to be processed (if
                // applicable)
                var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(subjectId, e.getMessage());
                // Capture error for MeasureReportBuilder
                measureDef.addError(error);
                logger.error(error, e);
            }
        }

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(fhirContext, measureDef);

        // Score all groups and stratifiers using version-agnostic scorer
        // Populates scores in MeasureDef before builders run
        // Note: Scoring is always performed, independent of applyScoring flag
        // (applyScoring controls set membership filtering, not numeric scoring)
        logger.debug("Scoring MeasureDef using MeasureReportDefScorer for measure: {}", measureDef.url());
        measureReportDefScorer.score(measureDef.url(), measureDef);
    }

    /**
     * method used to execute generate CQL results via Library $evaluate, $evaluate-measure, etc
     *
     * @param subjectIds subjects to generate results for
     * @param zonedMeasurementPeriod offset defined measurement period for evaluation
     * @param context cql engine context
     * @param multiLibraryIdMeasureEngineDetails container for engine, library and measure IDs
     * @return CQL results for Library defined in the Measure resource
     */
    public static CompositeEvaluationResultsPerMeasure getEvaluationResults(
            List<String> subjectIds,
            ZonedDateTime zonedMeasurementPeriod,
            CqlEngine context,
            MultiLibraryIdMeasureEngineDetails multiLibraryIdMeasureEngineDetails,
            Map<String, Object> parametersMap) {

        // measure -> subject -> results
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        // Library $evaluate each subject
        // The goal here is to do each measure/library evaluation within the context of a single subject.
        // This means that we will not switch between subject contexts while evaluating measures.
        // Once we've switched to a different subject context, the previous expression cache is dropped.

        final List<String> libraryIdentIds = multiLibraryIdMeasureEngineDetails.getLibraryIdentifiers().stream()
                .map(VersionedIdentifier::getId)
                .toList();

        logger.atDebug()
                .setMessage(
                        "START: Evaluate measure for library idents: (count:{}): [{}], and subjects (count={}): [{}]")
                .addArgument(libraryIdentIds::size)
                .addArgument(() -> showSubsetOfTotal(libraryIdentIds))
                .addArgument(subjectIds::size)
                .addArgument(() -> showSubsetOfTotal(subjectIds))
                .log();

        final long startAllLibrariesAllSubjects = System.currentTimeMillis();
        final int lastIndex = subjectIds.size() - 1;
        for (int subjectIndex = 0; subjectIndex < subjectIds.size(); subjectIndex++) {
            String subjectId = subjectIds.get(subjectIndex);
            if (subjectId == null) {
                throw new InternalErrorException("SubjectId is required in order to calculate.");
            }
            boolean shouldLog = subjectIndex % SUBJECT_LOG_INTERVAL == 0 || subjectIndex == lastIndex;
            throttledDebug(shouldLog)
                    .setMessage("Evaluate measure for library idents: (count:{}): [{}], and single subject [{}/{}]: {}")
                    .addArgument(libraryIdentIds::size)
                    .addArgument(() -> showSubsetOfTotal(libraryIdentIds))
                    .addArgument(subjectIndex)
                    .addArgument(lastIndex)
                    .addArgument(subjectId)
                    .log();
            Pair<String, String> subjectInfo = getSubjectTypeAndId(subjectId);
            String subjectTypePart = subjectInfo.getLeft();
            String subjectIdPart = subjectInfo.getRight();
            context.getState().setContextValue(subjectTypePart, subjectIdPart);
            try {
                var libraryIdentifiers = multiLibraryIdMeasureEngineDetails.getLibraryIdentifiers();

                final long startPerLibraryPerSubject = System.currentTimeMillis();
                throttledDebug(shouldLog)
                        .setMessage("START CQL evaluating libraries: (count:{}): [{}]")
                        .addArgument(libraryIdentIds::size)
                        .addArgument(() -> showSubsetOfTotal(libraryIdentIds))
                        .log();
                var evaluationResultsForMultiLib = multiLibraryIdMeasureEngineDetails
                        .getLibraryEngine()
                        .getEvaluationResult(
                                libraryIdentifiers,
                                subjectId,
                                null,
                                parametersMap,
                                null,
                                null,
                                null,
                                zonedMeasurementPeriod,
                                context);
                throttledDebug(shouldLog)
                        .setMessage("END CQL evaluating libraries [[elapsed: {}ms]] : (count:{}): [{}]")
                        .addArgument(() -> System.currentTimeMillis() - startPerLibraryPerSubject)
                        .addArgument(libraryIdentIds::size)
                        .addArgument(() -> showSubsetOfTotal(libraryIdentIds))
                        .log();

                for (var libraryVersionedIdentifier : libraryIdentifiers) {
                    validateEvaluationResultExistsForIdentifier(
                            libraryVersionedIdentifier, evaluationResultsForMultiLib);

                    var evaluationResult = evaluationResultsForMultiLib.getResultFor(libraryVersionedIdentifier);

                    var measureDefs =
                            multiLibraryIdMeasureEngineDetails.getMeasureDefsForLibrary(libraryVersionedIdentifier);

                    // function evaluation
                    final List<EvaluationResult> functionEvaluationResults =
                            FunctionEvaluationHandler.cqlFunctionEvaluation(
                                    context,
                                    measureDefs,
                                    libraryVersionedIdentifier,
                                    evaluationResult,
                                    subjectTypePart);

                    resultsBuilder.addResults(measureDefs, subjectId, evaluationResult, functionEvaluationResults);

                    Optional.ofNullable(evaluationResultsForMultiLib.getExceptionFor(libraryVersionedIdentifier))
                            .ifPresent(exception -> {
                                var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(
                                        subjectId, exception.getMessage());
                                resultsBuilder.addErrors(measureDefs, error);
                                logger.error(error, exception);
                            });
                }

            } catch (Exception e) {
                // If there's any error we didn't anticipate, catch it here:
                var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(subjectId, e.getMessage());
                var measureDefs = multiLibraryIdMeasureEngineDetails.getAllMeasureDefs();

                resultsBuilder.addErrors(measureDefs, error);
                logger.error(error, e);
            }
        }

        logger.atDebug()
                .setMessage(
                        "END: Evaluate measure for library idents: [[elapsed: {}ms, avgMs: {}]]: (count:{}): [{}], and subjects (count={}): [{}]")
                .addArgument(() -> System.currentTimeMillis() - startAllLibrariesAllSubjects)
                .addArgument(() -> subjectIds.isEmpty()
                        ? 0
                        : (System.currentTimeMillis() - startAllLibrariesAllSubjects) / subjectIds.size())
                .addArgument(libraryIdentIds::size)
                .addArgument(() -> showSubsetOfTotal(libraryIdentIds))
                .addArgument(subjectIds::size)
                .addArgument(() -> showSubsetOfTotal(subjectIds))
                .log();
        return resultsBuilder.build();
    }

    private static LoggingEventBuilder throttledDebug(boolean shouldLog) {
        return shouldLog ? logger.atDebug() : NOPLoggingEventBuilder.singleton();
    }

    private static String showSubsetOfTotal(List<String> subjectIds) {
        final int previewLimit = 5;
        return subjectIds.size() <= previewLimit
                ? String.join(",", subjectIds)
                : String.join(",", subjectIds.subList(0, previewLimit)) + ",...";
    }

    private static Pair<String, String> getSubjectTypeAndId(String subjectId) {
        if (subjectId.contains("/")) {
            String[] subjectIdParts = subjectId.split("/");
            return Pair.of(subjectIdParts[0], subjectIdParts[1]);
        } else {
            throw new InvalidRequestException(
                    "Unable to determine Subject type for id: %s. SubjectIds must be in the format {subjectType}/{subjectId} (e.g. Patient/123)"
                            .formatted(subjectId));
        }
    }

    private static void validateEvaluationResultExistsForIdentifier(
            VersionedIdentifier versionedIdentifierFromQuery, EvaluationResults evaluationResults) {

        var containsResults = evaluationResults.containsResultsFor(versionedIdentifierFromQuery);
        var containsExceptions = evaluationResults.containsExceptionsFor(versionedIdentifierFromQuery);

        if (!containsResults && !containsExceptions) {
            throw new InternalErrorException(
                    "Evaluation result in versionless search not found for identifier with ID: %s"
                            .formatted(versionedIdentifierFromQuery.getId()));
        }
    }
}
