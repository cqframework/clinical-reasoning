package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.collect.ListMultimap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.IntervalTypeSpecifier;
import org.hl7.elm.r1.NamedTypeSpecifier;
import org.hl7.elm.r1.ParameterDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.SearchableLibraryIdentifier;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.helper.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasureProcessorUtils {
    private static final Logger logger = LoggerFactory.getLogger(MeasureProcessorUtils.class);
    private static final String EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE = "Exception for subjectId: %s, Message: %s";

    /**
     * Method that processes CQL Results into Measure defined fields that reference associated CQL expressions
     * @param results criteria expression results
     * @param measureDef Measure defined objects
     * @param measureEvalType the type of evaluation algorithm to apply to Criteria results
     * @param applyScoring whether Measure Evaluator will apply set membership per measure scoring algorithm
     * @param populationBasisValidator the validator class to use for checking consistency of results
     */
    public void processResults(
            Map<String, EvaluationResult> results,
            MeasureDef measureDef,
            @Nonnull MeasureEvalType measureEvalType,
            boolean applyScoring,
            PopulationBasisValidator populationBasisValidator) {
        MeasureEvaluator evaluator = new MeasureEvaluator(populationBasisValidator);
        // Populate MeasureDef using MeasureEvaluator
        for (Map.Entry<String, EvaluationResult> entry : results.entrySet()) {
            // subject
            String subjectId = entry.getKey();
            var sub = getSubjectTypeAndId(subjectId);
            var subjectIdPart = sub.getRight();
            var subjectTypePart = sub.getLeft();
            // cql results
            EvaluationResult evalResult = entry.getValue();
            try {
                // populate results into MeasureDef
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
    }

    /**
     * Measures with defined scoring type of 'continuous-variable' where a defined 'measure-observation' population is used to evaluate results of 'measure-population'.
     * This method is a downstream calculation given it requires calculated results before it can be called.
     * Results are then added to associated MeasureDef
     * @param measureDef measure defined objects that are populated from criteria expression results
     * @param context cql engine context used to evaluate results
     */
    public void continuousVariableObservation(MeasureDef measureDef, CqlEngine context) {
        // Continuous Variable?
        for (GroupDef groupDef : measureDef.groups()) {
            // Measure Observation defined?
            if (groupDef.measureScoring().equals(MeasureScoring.CONTINUOUSVARIABLE)
                    && groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION) != null) {

                PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
                PopulationDef measureObservation = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);

                // Inject MeasurePopulation results into Measure Observation Function
                for (Object resource : measurePopulation.getResources()) {
                    Object observationResult = evaluateObservationCriteria(
                            resource,
                            measureObservation.expression(),
                            measureObservation.getEvaluatedResources(),
                            groupDef.isBooleanBasis(),
                            context);
                    measureObservation.addResource(observationResult);
                }
            }
        }
    }

    /**
     * method used to convert measurement period Interval object into ZonedDateTime
     * @param interval measurementPeriod interval
     * @return ZonedDateTime interval with appropriate offset
     */
    @Nullable
    public static ZonedDateTime getZonedTimeZoneForEval(@Nullable Interval interval) {
        return Optional.ofNullable(interval)
                .map(Interval::getLow)
                .filter(DateTime.class::isInstance)
                .map(DateTime.class::cast)
                .map(DateTime::getZoneOffset)
                .map(zoneOffset -> LocalDateTime.now().atOffset(zoneOffset).toZonedDateTime())
                .orElse(null);
    }

    /**
     * Extract measurement period defined within requested CQL file
     * @param context cql engine context
     * @return ParameterDef containing appropriately defined measurementPeriod
     */
    public ParameterDef getMeasurementPeriodParameterDef(CqlEngine context) {
        org.hl7.elm.r1.Library lib = context.getState().getCurrentLibrary();

        if (lib.getParameters() == null
                || lib.getParameters().getDef() == null
                || lib.getParameters().getDef().isEmpty()) {
            return null;
        }

        for (ParameterDef pd : lib.getParameters().getDef()) {
            if (pd.getName().equals(MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME)) {
                return pd;
            }
        }

        return null;
    }

    /**
     * method to set measurement period on cql engine context.
     * Priority is operation parameter defined value, otherwise default CQL value is used
     * @param measurementPeriod Interval defined by operation parameters to override default CQL value
     * @param context cql engine context used to set measurement period parameter
     */
    @SuppressWarnings({"deprecation", "removal"})
    public void setMeasurementPeriod(Interval measurementPeriod, CqlEngine context, List<String> measureUrls) {
        ParameterDef pd = this.getMeasurementPeriodParameterDef(context);
        if (pd == null) {
            logger.warn(
                    "Parameter \"{}\" was not found. Unable to validate type.",
                    MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            context.getState()
                    .setParameter(null, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, measurementPeriod);
            return;
        }

        if (measurementPeriod == null && pd.getDefault() == null) {
            logger.warn(
                    "No default or value supplied for Parameter \"{}\". This may result in incorrect results or errors.",
                    MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            return;
        }

        // Use the default, skip validation
        if (measurementPeriod == null) {
            measurementPeriod = (Interval) context.getEvaluationVisitor().visitParameterDef(pd, context.getState());

            context.getState()
                    .setParameter(
                            null,
                            MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME,
                            cloneIntervalWithUtc(measurementPeriod));
            return;
        }

        IntervalTypeSpecifier intervalTypeSpecifier = (IntervalTypeSpecifier) pd.getParameterTypeSpecifier();
        if (intervalTypeSpecifier == null) {
            logger.debug(
                    "No ELM type information available. Unable to validate type of \"{}\"",
                    MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            context.getState()
                    .setParameter(null, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, measurementPeriod);
            return;
        }

        NamedTypeSpecifier pointType = (NamedTypeSpecifier) intervalTypeSpecifier.getPointType();
        String targetType = pointType.getName().getLocalPart();
        Interval convertedPeriod = convertInterval(measurementPeriod, targetType, measureUrls);

        context.getState().setParameter(null, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, convertedPeriod);
    }

    /**
     * Get Cql MeasurementPeriod if parameters are empty
     * @param measurementPeriod Interval from operation parameters
     * @param context cql context to extract default values
     * @return operation parameters if populated, otherwise default CQL interval
     */
    public Interval getDefaultMeasurementPeriod(Interval measurementPeriod, CqlEngine context) {
        if (measurementPeriod == null) {
            return (Interval)
                    context.getState().getParameters().get(MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
        } else {
            return measurementPeriod;
        }
    }

    /**
     * Convert an Interval from some other timezone to UTC, including both the start and end.
     * For example, 2020-01-16T12:00:00-07:00-2020-01-16T12:59:59-07:00 becomes
     * 2020-01-16T12:00:00Z-2020-01-16T12:59:59Z
     *
     * @param interval The original interval with some offset.
     * @return The original dateTime but converted to UTC with the same local timestamp.
     */
    private static Interval cloneIntervalWithUtc(Interval interval) {
        final Object startAsObject = interval.getStart();
        final Object endAsObject = interval.getEnd();

        if (startAsObject instanceof DateTime time && endAsObject instanceof DateTime time1) {
            return new Interval(cloneDateTimeWithUtc(time), true, cloneDateTimeWithUtc(time1), true);
        }

        // Give up and just return the original Interval
        return interval;
    }

    /**
     * Convert a DateTime from some other timezone to UTC.
     * For example, 2020-01-16T12:00:00-07:00 becomes 2020-01-16T12:00:00Z
     *
     * @param dateTime The original dateTime with some offset.
     * @return The original dateTime but converted to UTC with the same local timestamp.
     */
    private static DateTime cloneDateTimeWithUtc(DateTime dateTime) {
        final DateTime newDateTime = new DateTime(dateTime.getDateTime().withOffsetSameLocal(ZoneOffset.UTC));
        newDateTime.setPrecision(dateTime.getPrecision());
        return newDateTime;
    }

    public Interval convertInterval(Interval interval, String targetType, List<String> measureUrls) {
        String sourceTypeQualified = interval.getPointType().getTypeName();
        String sourceType = sourceTypeQualified.substring(sourceTypeQualified.lastIndexOf(".") + 1);
        if (sourceType.equals(targetType)) {
            return interval;
        }

        if (sourceType.equals("DateTime") && targetType.equals("Date")) {
            logger.debug(
                    "A DateTime interval was provided and a Date interval was expected. The DateTime will be truncated.");
            return new Interval(
                    truncateDateTime((DateTime) interval.getLow()),
                    interval.getLowClosed(),
                    truncateDateTime((DateTime) interval.getHigh()),
                    interval.getHighClosed());
        }

        throw new InvalidRequestException(
                "The interval type of %s did not match the expected type of %s and no conversion was possible for measure URLs (first 5 only shown): %s."
                        .formatted(
                                sourceType,
                                targetType,
                                measureUrls.stream().limit(5).toList()));
    }

    public Date truncateDateTime(DateTime dateTime) {
        OffsetDateTime odt = dateTime.getDateTime();
        return new Date(odt.getYear(), odt.getMonthValue(), odt.getDayOfMonth());
    }

    /**
     * method used to extract evaluated resources touched by CQL criteria expressions
     * @param outEvaluatedResources set object used to capture resources touched
     * @param context cql engine context
     */
    public void captureEvaluatedResources(Set<Object> outEvaluatedResources, CqlEngine context) {
        if (outEvaluatedResources != null && context.getState().getEvaluatedResources() != null) {
            outEvaluatedResources.addAll(context.getState().getEvaluatedResources());
        }
        clearEvaluatedResources(context);
    }

    // reset evaluated resources followed by a context evaluation
    private void clearEvaluatedResources(CqlEngine context) {
        context.getState().clearEvaluatedResources();
    }

    /**
     * method used to evaluate cql expression defined for 'continuous variable' scoring type measures that have 'measure observation' to calculate
     * This method is called as a second round of processing given it uses 'measure population' results as input data for function
     * @param resource object that stores results of cql
     * @param criteriaExpression expression name to call
     * @param outEvaluatedResources set to store evaluated resources touched
     * @param isBooleanBasis the type of result created from expression
     * @param context cql engine context used to evaluate expression
     * @return cql results for subject requested
     */
    @SuppressWarnings({"deprecation", "removal"})
    public Object evaluateObservationCriteria(
            Object resource,
            String criteriaExpression,
            Set<Object> outEvaluatedResources,
            boolean isBooleanBasis,
            CqlEngine context) {

        var ed = Libraries.resolveExpressionRef(
                criteriaExpression, context.getState().getCurrentLibrary());

        if (!(ed instanceof FunctionDef functionDef)) {
            throw new InvalidRequestException(
                    "Measure observation %s does not reference a function definition".formatted(criteriaExpression));
        }

        Object result;

        context.getState().pushActivationFrame(functionDef, functionDef.getContext());

        try {
            if (!isBooleanBasis) {
                // subject based observations don't have a parameter to pass in
                context.getState()
                        .push(new Variable(
                                        ((FunctionDef) ed).getOperand().get(0).getName())
                                .withValue(resource));
            }
            result = context.getEvaluationVisitor().visitExpression(ed.getExpression(), context.getState());
        } finally {
            context.getState().popActivationFrame();
        }

        captureEvaluatedResources(outEvaluatedResources, context);

        return result;
    }

    public CompositeEvaluationResultsPerMeasure getEvaluationResults(
            List<String> subjectIds,
            ZonedDateTime zonedMeasurementPeriod,
            CqlEngine context,
            MeasureLibraryIdEngineDetails measureLibraryIdEngineDetails) {

        return getEvaluationResults(
                subjectIds, zonedMeasurementPeriod, context, List.of(measureLibraryIdEngineDetails));
    }

    // LUKETODO: redo javadoc
    /**
     * method used to execute generate CQL results via Library $evaluate
     *
     * @param subjectIds subjects to generate results for
     * @param zonedMeasurementPeriod offset defined measurement period for evaluation
     * @param context cql engine context
     * @param measureLibraryIdEngineDetailsList contains details of measureId, libraryId, and LibraryEngine
     * @return CQL results for Library defined in the Measure resource
     */
    public CompositeEvaluationResultsPerMeasure getEvaluationResults(
            List<String> subjectIds,
            ZonedDateTime zonedMeasurementPeriod,
            CqlEngine context,
            List<MeasureLibraryIdEngineDetails> measureLibraryIdEngineDetailsList) {

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
            // LUKETODO:  we reset the cache outside of the measure loop
            Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectId);
            String subjectTypePart = subjectInfo.getLeft();
            String subjectIdPart = subjectInfo.getRight();
            context.getState().setContextValue(subjectTypePart, subjectIdPart);
            for (var measureLibraryIdEngine : measureLibraryIdEngineDetailsList) {
                try {
                    var libraryId = measureLibraryIdEngine.libraryId();
                    logger.info("1234: libraryId: {}", libraryId.getId());
                    var evaluationResult = measureLibraryIdEngine
                        .engine()
                        // LUKETODO:  make CQL CqlEngine changes to take multiple versioned identifiers and
                        // then initState for mulitple libraries
                        // this seems to also reset the cache
                        // LUKETODO:  call the new mulitple versionidentifier method once it's available in
                        // CQL
                        .getEvaluationResult(
                            libraryId,
                            subjectId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            zonedMeasurementPeriod,
                            context);

                    resultsBuilder.addResult(measureLibraryIdEngine.measureId(), subjectId, evaluationResult);
                } catch (Exception e) {
                    // Catch Exceptions from evaluation per subject, but allow rest of subjects to be processed (if
                    // applicable)
                    var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(subjectId, e.getMessage());
                    resultsBuilder.addError(measureLibraryIdEngine.measureId(), error);
                    logger.error(error, e);
                }
            }
        }

        return resultsBuilder.build();
    }

    public CompositeEvaluationResultsPerMeasure getEvaluationResults2(
            List<String> subjectIds,
            ZonedDateTime zonedMeasurementPeriod,
            CqlEngine context,
            LibraryEngine libraryEngineForMultipleLibraries,
            ListMultimap<VersionedIdentifier,IIdType> versionedIdMeasureIdPairs) {// LUKETODO: rename

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
            // LUKETODO:  we reset the cache outside of the measure loop
            Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectId);
            String subjectTypePart = subjectInfo.getLeft();
            String subjectIdPart = subjectInfo.getRight();
            context.getState().setContextValue(subjectTypePart, subjectIdPart);
            try {
                // LUKETODO:  is library identifier ordering important here?
                var libraryIdentifiers = List.copyOf(versionedIdMeasureIdPairs.keySet());
                // LUKETODO:  try to do one library at a time and see if there are the same errors:

                for (VersionedIdentifier libraryVersionedIdentifier : libraryIdentifiers) {
                    var evaluationResultsPerLibraryIdentifier = libraryEngineForMultipleLibraries.getEvaluationResult(
                        libraryVersionedIdentifier,
                        subjectId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        zonedMeasurementPeriod,
                        context);

                    var measureIds = versionedIdMeasureIdPairs.get(libraryVersionedIdentifier);

                    for (IIdType measureId : measureIds) {
                        resultsBuilder.addResult(
                            measureId,
                            subjectId,
                            evaluationResultsPerLibraryIdentifier);
                    }
                }

                // LUKETODO: look into the right way to implement this later?
                // LUKETODO: if we do it this way, we get invalid interval CQL errors among others:  why??????
//                var evaluationResultsPerLibraryIdentifier = libraryEngineForMultipleLibraries.getEvaluationResult(
//                        libraryIdentifiers,
//                        subjectId,
//                        null,
//                        null,
//                        null,
//                        null,
//                        null,
//                        zonedMeasurementPeriod,
//                        context);
//
//                for (var libraryVersionedIdentifier: libraryIdentifiers) {
//                    validateEvaluationResultExistsForIdentifier(
//                        libraryVersionedIdentifier,
//                        evaluationResultsPerLibraryIdentifier);
//
//                    var evaluationResultForLibraryIdentifier = evaluationResultsPerLibraryIdentifier.get(SearchableLibraryIdentifier.fromIdentifier(libraryVersionedIdentifier));
//
//                    var measureIds = versionedIdMeasureIdPairs.get(libraryVersionedIdentifier);
//
//                    for (IIdType measureId : measureIds) {
//                        resultsBuilder.addResult(
//                            measureId,
//                            subjectId,
//                            evaluationResultForLibraryIdentifier);
//                    }
//                }

            } catch (Exception e) {
                // Catch Exceptions from evaluation per subject, but allow rest of subjects to be processed (if
                // applicable)
                var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(subjectId, e.getMessage());
                var measureIds = List.copyOf(versionedIdMeasureIdPairs.values());

                resultsBuilder.addErrors(measureIds, error);
                logger.error(error, e);
            }
        }

        return resultsBuilder.build();
    }

    private void validateEvaluationResultExistsForIdentifier(
            VersionedIdentifier versionedIdentifierFromQuery,
            Map<SearchableLibraryIdentifier, EvaluationResult> evaluationResultsPerVersionedIdentifier) {

        var doNoneMatch = evaluationResultsPerVersionedIdentifier.entrySet().stream()
            .noneMatch( entry -> entry.getKey().matches(versionedIdentifierFromQuery));

        if (doNoneMatch) {
            throw new InternalErrorException(
                "Evaluation result in versionless search not found for identifier with ID: %s"
                    .formatted(versionedIdentifierFromQuery.getId()));
        }
    }

    private static String printEvaluationResult(EvaluationResult evaluationResult) {
        if (evaluationResult == null) {
            return "null";
        }
        return "EvaluationResult{" +
                "expressionResults=" + evaluationResult.expressionResults.entrySet().stream().map(entry -> new DefaultMapEntry<>(
            entry.getKey(), printExpressionResult(entry.getValue()))).toList() + "}";
    }

    private static String printExpressionResult(ExpressionResult expressionResult) {
        if (expressionResult == null) {
            return "null";
        }

        return "ExpressionResult{value=" + expressionResult.value() +
                ", type=" + expressionResult.evaluatedResources() + '}';
    }

    public Pair<String, String> getSubjectTypeAndId(String subjectId) {
        if (subjectId.contains("/")) {
            String[] subjectIdParts = subjectId.split("/");
            return Pair.of(subjectIdParts[0], subjectIdParts[1]);
        } else {
            throw new InvalidRequestException(
                    "Unable to determine Subject type for id: %s. SubjectIds must be in the format {subjectType}/{subjectId} (e.g. Patient/123)"
                            .formatted(subjectId));
        }
    }

    public MeasureEvalType getEvalType(MeasureEvalType evalType, String reportType, List<String> subjectIds) {
        if (evalType == null) {
            evalType = MeasureEvalType.fromCode(reportType)
                    .orElse(
                            subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                                    ? MeasureEvalType.POPULATION
                                    : MeasureEvalType.SUBJECT);
        }
        return evalType;
    }

    public Interval buildMeasurementPeriod(String periodStart, String periodEnd) {
        if (periodStart == null || periodEnd == null) {
            return null;
        } else {
            // resolve the measurement period
            return new Interval(
                    DateHelper.resolveRequestDate(periodStart, true),
                    true,
                    DateHelper.resolveRequestDate(periodEnd, false),
                    true);
        }
    }
}
