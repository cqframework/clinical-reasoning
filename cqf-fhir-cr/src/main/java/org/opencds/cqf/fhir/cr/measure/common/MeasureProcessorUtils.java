package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.IntervalTypeSpecifier;
import org.hl7.elm.r1.NamedTypeSpecifier;
import org.hl7.elm.r1.ParameterDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.EvaluationResultsForMultiLib;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
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
                Map<String, Set<Object>> subjectResources = measurePopulation.getSubjectResources();

                for (Map.Entry<String, Set<Object>> entry : subjectResources.entrySet()) {
                    String subjectId = entry.getKey();
                    Set<Object> resourcesForSubject = entry.getValue();

                    for (Object resource : resourcesForSubject) {
                        Object observationResult = evaluateObservationCriteria(
                                resource,
                                measureObservation.expression(),
                                measureObservation.getEvaluatedResources(),
                                groupDef.isBooleanBasis(),
                                context);
                        measureObservation.addResource(observationResult);
                        measureObservation.addResource(subjectId, observationResult);
                    }
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
                        .push(new Variable(functionDef.getOperand().get(0).getName()).withValue(resource));
            }
            result = context.getEvaluationVisitor().visitExpression(ed.getExpression(), context.getState());

            // LUKETODO:  get rid of this during final cleanup
            String id;
            Period period;
            if (resource instanceof Encounter encounter) {
                id = encounter.getId();
                period = encounter.getPeriod();
            } else {
                id = null;
                period = null;
            }
            logger.info("id: {}, period: {}, expression result: {}", id, printPeriod(period), result);
            // wrap result as Observation

        } finally {
            context.getState().popActivationFrame();
        }

        captureEvaluatedResources(outEvaluatedResources, context);

        return result;
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ssXXX");

    private String printPeriod(Period period) {
        if (period == null) {
            return "null";
        }
        return "start: %s, finish: %s"
                .formatted(DATE_FORMAT.format(period.getStart()), DATE_FORMAT.format(period.getEnd()));
    }

    /**
     * method used to execute generate CQL results via Library $evaluate
     *
     * @param subjectIds subjects to generate results for
     * @param zonedMeasurementPeriod offset defined measurement period for evaluation
     * @param context cql engine context
     * @param multiLibraryIdMeasureEngineDetails container for engine, library and measure IDs
     * @return CQL results for Library defined in the Measure resource
     */
    public CompositeEvaluationResultsPerMeasure getEvaluationResults(
            List<String> subjectIds,
            ZonedDateTime zonedMeasurementPeriod,
            CqlEngine context,
            MultiLibraryIdMeasureEngineDetails multiLibraryIdMeasureEngineDetails) {

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
            Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectId);
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
                    // standard CQL expression results
                    var evaluationResult = evaluationResultsForMultiLib.getResultFor(libraryVersionedIdentifier);

                    if (evaluationResult == null) {
                        // this should never happen due to validateEvaluationResultExistsForIdentifier
                        throw new IllegalStateException(
                                "No evaluation result found for library: %s".formatted(libraryVersionedIdentifier));
                    }

                    // measure Observation Path, have to re-initialize everything again
                    // TODO: extend library evaluated context so library initialization isn't having to be built for
                    // both, and takes advantage of caching
                    var compiledLibraries = getCompiledLibraries(libraryIdentifiers, context);

                    var libraries = compiledLibraries.stream()
                            .map(CompiledLibrary::getLibrary)
                            .toList();

                    // Add back the libraries to the stack, since we popped them off during CQL
                    context.getState().init(libraries);

                    // Measurement Period: operation parameter defined measurement period
                    // this necessary?
                    // Interval measurementPeriodParams = buildMeasurementPeriod(periodStart, periodEnd);

                    //                    setMeasurementPeriod(
                    //                        measurementPeriodParams,
                    //                        context,
                    //                        Optional.ofNullable(measureDef.).map(List::of).orElse(List.of("Unknown
                    // Measure URL")));
                    // one Library may be linked to multiple Measures
                    var measureDefs =
                            multiLibraryIdMeasureEngineDetails.getMeasureIdsForLibrary(libraryVersionedIdentifier);
                    List<IIdType> measureIds = measureDeftoIIdType(measureDefs);

                    for (MeasureDef measureDef : measureDefs) {
                        // if measure contains measure-observation, otherwise short circuit
                        if (hasMeasureObservation(measureDef)) {

                            // get function for measure-observation from populationDef
                            for (GroupDef groupDef : measureDef.groups()) {
                                for (PopulationDef populationDef : groupDef.populations()) {
                                    // each measureObservation is evaluated
                                    if (populationDef.type().equals(MeasurePopulationType.MEASUREOBSERVATION)) {
                                        // get criteria input for results to get (measure-population, numerator,
                                        // denominator)
                                        var criteriaPopulationId = populationDef.getCriteriaReference();
                                        // function that will be evaluated
                                        var observationExpression = populationDef.expression();
                                        // get expression from criteriaPopulation reference
                                        String criteriaExpressionInput = groupDef.populations().stream()
                                                .filter(t -> t.id().equals(criteriaPopulationId))
                                                .map(PopulationDef::expression)
                                                .findFirst()
                                                .orElse(null);
                                        ExpressionResult expressionResult =
                                                tryGetExpressionResult(criteriaExpressionInput, evaluationResult);
                                        // makes expression results iterable
                                        var resultsIter =
                                                getResultIterable(evaluationResult, expressionResult, subjectTypePart);
                                        // make new expression name for uniquely extracting results
                                        // this will be used in MeasureEvaluator
                                        var expressionName = criteriaPopulationId + "-" + observationExpression;
                                        // loop through measure-population results
                                        int i = 0;
                                        Map<Object, Object> functionResults = new HashMap<>();
                                        Set<Object> evaluatedResources = new HashSet<>();
                                        for (Object result : resultsIter) {
                                            Object observationResult = evaluateObservationCriteria(
                                                    result,
                                                    observationExpression,
                                                    evaluatedResources,
                                                    groupDef.isBooleanBasis(),
                                                    context);
                                            var observationId = expressionName + "-" + i;
                                            // wrap result in Observation resource to avoid duplicate results data loss
                                            // in set object
                                            Observation observation = wrapResultAsObservation(
                                                    observationId, observationId, observationResult);
                                            // add function results to existing EvaluationResult under new expression
                                            // name
                                            // need a way to capture input parameter here too, otherwise we have no way
                                            // to connect input objects related to output object
                                            // key= input parameter to function
                                            // value= the output Observation resource containing calculated value
                                            functionResults.put(result, observation);
                                        }
                                        evaluationResult.expressionResults.put(
                                                expressionName,
                                                new ExpressionResult(functionResults, evaluatedResources));
                                    }
                                }
                            }
                        }
                    }

                    resultsBuilder.addResults(measureIds, subjectId, evaluationResult);

                    Optional.ofNullable(evaluationResultsForMultiLib.getExceptionFor(libraryVersionedIdentifier))
                            .ifPresent(exception -> {
                                var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(
                                        subjectId, exception.getMessage());
                                resultsBuilder.addErrors(measureIds, error);
                                logger.error(error, exception);
                            });
                }

            } catch (Exception e) {
                // If there's any error we didn't anticipate, catch it here:
                var error = EXCEPTION_FOR_SUBJECT_ID_MESSAGE_TEMPLATE.formatted(subjectId, e.getMessage());
                var measureIds = measureDeftoIIdType(multiLibraryIdMeasureEngineDetails.getAllMeasureIds());

                resultsBuilder.addErrors(measureIds, error);
                logger.error(error, e);
            }
        }

        return resultsBuilder.build();
    }

    public ExpressionResult tryGetExpressionResult(String expressionName, EvaluationResult evaluationResult) {
        if (evaluationResult == null || expressionName == null) {
            throw new InvalidRequestException("evaluationResult is null: " + expressionName);
        }

        final Map<String, ExpressionResult> expressionResults = evaluationResult.expressionResults;

        if (!expressionResults.containsKey(expressionName)) {
            throw new InvalidRequestException("Could not find expression result for expression: " + expressionName);
        }

        return evaluationResult.expressionResults.get(expressionName);
    }

    public List<CompiledLibrary> getCompiledLibraries(List<VersionedIdentifier> ids, CqlEngine context) {
        try {
            var resolvedLibraryResults =
                    context.getEnvironment().getLibraryManager().resolveLibraries(ids);

            var allErrors = resolvedLibraryResults.allErrors();
            if (resolvedLibraryResults.hasErrors() || ids.size() > allErrors.size()) {
                return resolvedLibraryResults.allCompiledLibraries();
            }

            if (ids.size() == 1) {
                final List<CqlCompilerException> cqlCompilerExceptions =
                        resolvedLibraryResults.getErrorsFor(ids.get(0));

                if (cqlCompilerExceptions.size() == 1) {
                    throw new IllegalStateException(
                            "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded."
                                    .formatted(ids.get(0).getId()),
                            cqlCompilerExceptions.get(0));
                } else {
                    throw new IllegalStateException(
                            "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded. Errors: %s"
                                    .formatted(
                                            ids.get(0).getId(),
                                            cqlCompilerExceptions.stream()
                                                    .map(CqlCompilerException::getMessage)
                                                    .reduce((s1, s2) -> s1 + "; " + s2)
                                                    .orElse("No error messages found.")));
                }
            }

            throw new IllegalStateException(
                    "Unable to load CQL/ELM for libraries: %s Verify that the Library resource is available in your environment and has CQL/ELM content embedded. Errors: %s"
                            .formatted(ids, allErrors));

        } catch (CqlIncludeException exception) {
            throw new IllegalStateException(
                    "Unable to load CQL/ELM for libraries: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded."
                            .formatted(
                                    ids.stream().map(VersionedIdentifier::getId).toList()),
                    exception);
        }
    }

    protected Observation wrapResultAsObservation(String id, String observationName, Object result) {

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(id);
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setValue(convertToQuantity(result));
        obs.setCode(cc);
        return obs;
    }

    public Quantity convertToQuantity(Object obj) {
        if (obj == null) return null;

        Quantity q = new Quantity();

        if (obj instanceof Quantity existing) {
            return existing;
        } else if (obj instanceof Number number) {
            q.setValue(number.doubleValue());
        } else if (obj instanceof String s) {
            try {
                q.setValue(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("String is not a valid number: " + s, e);
            }
        } else {
            throw new IllegalArgumentException("Cannot convert object of type " + obj.getClass() + " to Quantity");
        }

        return q;
    }

    private List<IIdType> measureDeftoIIdType(List<MeasureDef> measureDefs) {
        return measureDefs.stream()
                .map(t -> new IdType(t.id()))
                .map(x -> (IIdType) x)
                .toList();
    }

    private Iterable<?> getResultIterable(
            EvaluationResult evaluationResult, ExpressionResult expressionResult, String subjectTypePart) {
        if (expressionResult.value() instanceof Boolean) {
            if ((Boolean.TRUE.equals(expressionResult.value()))) {
                // if Boolean, returns context by SubjectType
                Object booleanResult =
                        evaluationResult.forExpression(subjectTypePart).value();
                // remove evaluated resources
                return Collections.singletonList(booleanResult);
            } else {
                // false result shows nothing
                return Collections.emptyList();
            }
        }

        Object value = expressionResult.value();
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        } else {
            return Collections.singletonList(value);
        }
    }
    /**
     * Checks if a MeasureDef has at least one PopulationDef of type MEASUREOBSERVATION
     * across all of its groups.
     *
     * @param measureDef the MeasureDef to check
     * @return true if any PopulationDef in any GroupDef is MEASUREOBSERVATION
     */
    public static boolean hasMeasureObservation(MeasureDef measureDef) {
        if (measureDef == null || measureDef.groups() == null) {
            return false;
        }

        return measureDef.groups().stream()
                .filter(group -> group.populations() != null)
                .flatMap(group -> group.populations().stream())
                .anyMatch(pop -> pop.type() == MeasurePopulationType.MEASUREOBSERVATION);
    }

    private void validateEvaluationResultExistsForIdentifier(
            VersionedIdentifier versionedIdentifierFromQuery,
            EvaluationResultsForMultiLib evaluationResultsForMultiLib) {

        // LUKETODO:  this should hopefully be fixed with the next version of CQL
        var containsResults = evaluationResultsForMultiLib.containsResultsFor(versionedIdentifierFromQuery);
        var containsExceptions = evaluationResultsForMultiLib.containsExceptionsFor(versionedIdentifierFromQuery);

        if (!containsResults && !containsExceptions) {
            throw new InternalErrorException(
                    "Evaluation result in versionless search not found for identifier with ID: %s"
                            .formatted(versionedIdentifierFromQuery.getId()));
        }
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
