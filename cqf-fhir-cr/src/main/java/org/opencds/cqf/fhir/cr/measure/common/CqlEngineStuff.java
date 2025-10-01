package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.IntervalTypeSpecifier;
import org.hl7.elm.r1.NamedTypeSpecifier;
import org.hl7.elm.r1.ParameterDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;

public class CqlEngineStuff {
    private static final Logger logger = LoggerFactory.getLogger(CqlEngineStuff.class);

    private final MeasureProcessorUtils measureProcessorUtils = new MeasureProcessorUtils();

    private void preLibraryEvaluationPeriodProcessing(
        List<VersionedIdentifier> libraryVersionedIdentifiers,
        List<Measure> measures,
        Parameters parameters,
        CqlEngine context,
        Interval measurementPeriodParams) {

        try (var cqlEngineCloseable = new CqlEngineCloseable(context, libraryVersionedIdentifiers)) {

            final Map<String, Object> parametersMap =
                Optional.ofNullable(parameters)
                    .map(this::resolveParameterMap)
                    .orElse(null);

            cqlEngineCloseable.setArgParameters(parametersMap);

            // LUKETODO:  this relies on the CqlEngine
            setMeasurementPeriod(
                measurementPeriodParams,
                context,
                measures.stream()
                    .map(Measure::getUrl)
                    .map(url -> Optional.ofNullable(url).orElse("Unknown Measure URL"))
                    .toList());
        }
    }

    private Interval postLibraryEvaluationPeriodProcessingAndContinuousVariableObservation(
            List<VersionedIdentifier> libraryVersionedIdentifiers,
            Measure measure,
            MeasureDef measureDef,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            CqlEngine context) {

        try (var cqlEngineCloseable = new CqlEngineCloseable(context, libraryVersionedIdentifiers)) {

            // Measurement Period: operation parameter defined measurement period
            // LUKETODO:  this doesn't rely on the CqlEngine
            Interval measurementPeriodParams = buildMeasurementPeriod(periodStart, periodEnd);

            // LUKETODO:  this relies on the CqlEngine
            setMeasurementPeriod(
                measurementPeriodParams,
                context,
                Optional.ofNullable(measure.getUrl()).map(List::of).orElse(List.of("Unknown Measure URL")));

            // DON'T pop the library off the stack yet, because we need it for continuousVariableObservation()

            // Populate populationDefs that require MeasureDef results
            // LUKETODO:  this relies on the CqlEngine
            continuousVariableObservation(measureDef, context);

            // LUKETODO:  this relies on the CqlEngine
            return getDefaultMeasurementPeriod(measurementPeriodParams, context);
        }
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
        } finally {
            context.getState().popActivationFrame();
        }

        captureEvaluatedResources(outEvaluatedResources, context);

        return result;
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
            // LUKETODO:  they really really really don't want us to use this:
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
     *
     * Convert a DateTime from some other timezone to UTC.
     * For example, 2020-01-16T12:00:00-07:00 becomes 2020-01-16T12:00:00Z
     *
     * @param dateTime The original dateTime with some offset.
     * @return The original dateTime but converted to UTC with the same local timestamp.
     */
    private static DateTime cloneDateTimeWithUtc(DateTime dateTime) {
        final DateTime newDateTime = new DateTime(dateTime.getDateTime().withOffsetSameLocal(
            ZoneOffset.UTC));
        newDateTime.setPrecision(dateTime.getPrecision());
        return newDateTime;
    }

    /**
     * Extract measurement period defined within requested CQL file
     * @param context cql engine context
     * @return ParameterDef containing appropriately defined measurementPeriod
     */
    private ParameterDef getMeasurementPeriodParameterDef(CqlEngine context) {
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

    public Interval buildMeasurementPeriod(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        Interval measurementPeriod = null;
        if (periodStart != null && periodEnd != null) {
            // Operation parameter defined measurementPeriod
            var helper = new R4DateHelper();
            measurementPeriod = helper.buildMeasurementPeriodInterval(periodStart, periodEnd);
        }
        return measurementPeriod;
    }

    protected void setArgParameters(Parameters parameters, CqlEngine context, List<CompiledLibrary> libs) {
        if (parameters != null) {
            Map<String, Object> paramMap = resolveParameterMap(parameters);
            for (CompiledLibrary lib : libs) {
                context.getState().setParameters(lib.getLibrary(), paramMap);

                if (lib.getLibrary().getIncludes() != null) {
                    lib.getLibrary()
                        .getIncludes()
                        .getDef()
                        .forEach(includeDef -> paramMap.forEach((paramKey, paramValue) -> context.getState()
                            .setParameter(includeDef.getLocalIdentifier(), paramKey, paramValue)));
                }
            }
        }
    }

    private Map<String, Object> resolveParameterMap(Parameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();
        R4FhirModelResolver modelResolver = new R4FhirModelResolver();
        parameters.getParameter().forEach(param -> {
            Object value;
            if (param.hasResource()) {
                value = param.getResource();
            } else {
                value = param.getValue();
                if (value instanceof IPrimitiveType<?> type) {
                    // TODO: handle Code, CodeableConcept, Quantity, etc
                    // resolves Date/Time values
                    value = modelResolver.toJavaPrimitive(type.getValue(), value);
                }
            }
            if (parameterMap.containsKey(param.getName())) {
                if (parameterMap.get(param.getName()) instanceof List) {
                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        var list = (List<Object>) parameterMap.get(param.getName());
                        list.add(value);
                    }
                } else {
                    // We need a mutable list here, otherwise, retrieving the list above will fail with
                    // UnsupportedOperationException
                    parameterMap.put(
                        param.getName(), new ArrayList<>(
                            Arrays.asList(parameterMap.get(param.getName()), value)));
                }
            } else {
                parameterMap.put(param.getName(), value);
            }
        });
        return parameterMap;
    }
}
