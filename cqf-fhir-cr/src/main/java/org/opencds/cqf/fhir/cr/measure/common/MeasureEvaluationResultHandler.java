package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.EvaluationResultsForMultiLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exclusively responsible for calling CQL evaluation and collating the results among multiple
 * measure defs in a FHIR version agnostic way.
 */
public class MeasureEvaluationResultHandler {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluationResultHandler.class);

    private static final String EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE = "Exception for subjectId: %s, Message: %s";

    private MeasureEvaluationResultHandler() {
        // static class
    }

    /**
     * Method that consumes pre-generated CQL results into Measure defined fields that reference associated CQL expressions
     * This is meant to be called by CQL CLI.
     *
     * @param evalResultsPerSubject criteria expression evalResultsPerSubject
     * @param measureDef Measure defined objects
     * @param measureEvalType the type of evaluation algorithm to apply to Criteria results
     * @param applyScoring whether Measure Evaluator will apply set membership per measure scoring algorithm
     * @param populationBasisValidator the validator class to use for checking consistency of results
     */
    public static void processResults(
            Map<String, EvaluationResult> evalResultsPerSubject,
            MeasureDef measureDef,
            @Nonnull MeasureEvalType measureEvalType,
            boolean applyScoring,
            PopulationBasisValidator populationBasisValidator) {
        MeasureEvaluator evaluator = new MeasureEvaluator(populationBasisValidator);
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
                evaluator.evaluate(
                        measureDef, measureEvalType, subjectTypePart, subjectIdPart, evalResult, applyScoring);
            } catch (Exception e) {
                // Catch Exceptions from evaluation per subject, but allow rest of subjects to be processed (if
                // applicable)
                var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(subjectId, e.getMessage());
                // Capture error for MeasureReportBuilder
                measureDef.addError(error);
                logger.error(error, e);
            }
        }

        evaluator.postEvaluation(measureDef);
    }

    /**
     * method used to execute generate CQL results via Library $evaluate, $evaluate-measure, etc
     *
     * @param subjectIds subjects to generate results for
     * @param zonedMeasurementPeriod offset defined measurement period for evaluation
     * @param context cql engine context
     * @param multiLibraryIdMeasureEngineDetails container for engine, library and measure IDs
     * @param continuousVariableObservationConverter used for continuous variable scoring FHIR version
     *                                               specific
     * @return CQL results for Library defined in the Measure resource
     */
    public static <T extends ICompositeType> CompositeEvaluationResultsPerMeasure getEvaluationResults(
            List<String> subjectIds,
            ZonedDateTime zonedMeasurementPeriod,
            CqlEngine context,
            MultiLibraryIdMeasureEngineDetails multiLibraryIdMeasureEngineDetails,
            ContinuousVariableObservationConverter<T> continuousVariableObservationConverter) {

        // measure -> subject -> results
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        // Library $evaluate each subject
        // The goal here is to do each measure/library evaluation within the context of a single subject.
        // This means that we will not switch between subject contexts while evaluating measures.
        // Once we've switched to a different subject context, the previous expression cache is dropped.
        for (String subjectId : subjectIds) {
            if (subjectId == null) {
                throw new InternalErrorException("SubjectId is required in order to calculate.");
            }
            Pair<String, String> subjectInfo = getSubjectTypeAndId(subjectId);
            String subjectTypePart = subjectInfo.getLeft();
            String subjectIdPart = subjectInfo.getRight();
            context.getState().setContextValue(subjectTypePart, subjectIdPart);
            try {
                var libraryIdentifiers = multiLibraryIdMeasureEngineDetails.getLibraryIdentifiers();

                var evaluationResultsForMultiLib = multiLibraryIdMeasureEngineDetails
                        .getLibraryEngine()
                        .getEvaluationResult(
                                libraryIdentifiers,
                                subjectId,
                                null,
                                null,
                                null,
                                null,
                                null,
                                zonedMeasurementPeriod,
                                context);

                for (var libraryVersionedIdentifier : libraryIdentifiers) {
                    validateEvaluationResultExistsForIdentifier(
                            libraryVersionedIdentifier, evaluationResultsForMultiLib);
                    var evaluationResult = evaluationResultsForMultiLib.getResultFor(libraryVersionedIdentifier);

                    // LUKETODO:  add functionality for warnings versus errors from CQL results and some clear tests
                    var measureDefs =
                            multiLibraryIdMeasureEngineDetails.getMeasureDefsForLibrary(libraryVersionedIdentifier);

                    final List<EvaluationResult> measureObservationResults =
                            ContinuousVariableObservationHandler.continuousVariableEvaluation(
                                    context,
                                    measureDefs,
                                    libraryVersionedIdentifier,
                                    evaluationResult,
                                    subjectTypePart,
                                    continuousVariableObservationConverter);

                    resultsBuilder.addResults(measureDefs, subjectId, evaluationResult, measureObservationResults);

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

        return resultsBuilder.build();
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
            VersionedIdentifier versionedIdentifierFromQuery,
            EvaluationResultsForMultiLib evaluationResultsForMultiLib) {

        var containsResults = evaluationResultsForMultiLib.containsResultsFor(versionedIdentifierFromQuery);
        var containsExceptions = evaluationResultsForMultiLib.containsExceptionsFor(versionedIdentifierFromQuery);

        if (!containsResults && !containsExceptions) {
            throw new InternalErrorException(
                    "Evaluation result in versionless search not found for identifier with ID: %s"
                            .formatted(versionedIdentifierFromQuery.getId()));
        }
    }
}
