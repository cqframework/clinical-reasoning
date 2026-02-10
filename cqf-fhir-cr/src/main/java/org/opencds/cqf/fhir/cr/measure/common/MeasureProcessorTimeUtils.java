package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.hl7.elm.r1.IntervalTypeSpecifier;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.NamedTypeSpecifier;
import org.hl7.elm.r1.ParameterDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.helper.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasureProcessorTimeUtils {
    private static final Logger logger = LoggerFactory.getLogger(MeasureProcessorTimeUtils.class);

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

    public static Interval getMeasurementPeriod(
            @Nullable ZonedDateTime periodStart, @Nullable ZonedDateTime periodEnd, CqlEngine context) {

        return getDefaultMeasurementPeriod(buildMeasurementPeriod(periodStart, periodEnd), context);
    }

    private static Interval buildMeasurementPeriod(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        if (periodStart == null && periodEnd == null) {
            return null;
        }
        // Operation parameter defined measurementPeriod
        return DateHelper.buildMeasurementPeriodInterval(periodStart, periodEnd);
    }

    /**
     * Get Cql MeasurementPeriod if parameters are empty
     * @param measurementPeriod Interval from operation parameters
     * @param context cql context to extract default values
     * @return operation parameters if populated, otherwise default CQL interval
     */
    public static Interval getDefaultMeasurementPeriod(Interval measurementPeriod, CqlEngine context) {
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
    static Interval cloneIntervalWithUtc(Interval interval) {
        final Object startAsObject = interval.getStart();
        final Object endAsObject = interval.getEnd();

        if (startAsObject instanceof DateTime time && endAsObject instanceof DateTime time1) {
            return new Interval(convertToUtc(time), true, convertToUtc(time1), true);
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
    private static DateTime convertToUtc(DateTime dateTime) {
        if (dateTime == null || dateTime.getDateTime() == null) {
            return null;
        }
        final DateTime newDateTime = new DateTime(dateTime.getDateTime().withOffsetSameLocal(ZoneOffset.UTC));
        newDateTime.setPrecision(dateTime.getPrecision());
        return newDateTime;
    }

    /**
     * Resolve the measurement period (user-provided or CQL default) and add it to the parameters map.
     * <p>
     * When the user provides a measurement period, it is validated/converted against the CQL library's
     * parameter type. When no measurement period is provided, the CQL default is resolved from the
     * library, UTC-cloned, and used instead.
     *
     * @param measurementPeriodParams user-provided measurement period (may be null)
     * @param context CQL engine context
     * @param libraryIdentifiers library identifiers to resolve against
     * @param measureUrls measure URLs for error messages
     * @param parametersMap mutable parameters map to add the measurement period to
     */
    public static void resolveMeasurementPeriodIntoParameters(
            @Nullable Interval measurementPeriodParams,
            CqlEngine context,
            List<VersionedIdentifier> libraryIdentifiers,
            List<String> measureUrls,
            Map<String, Object> parametersMap) {
        var firstLibraryId = libraryIdentifiers.get(0);

        if (measurementPeriodParams != null) {
            // User provided measurement period: validate/convert and add to parameters map
            var elmLibrary = context.getEnvironment().resolveLibrary(firstLibraryId);
            if (elmLibrary == null) {
                throw new InternalErrorException("Could not resolve ELM library for identifier: %s, measure URLs: %s"
                        .formatted(firstLibraryId.getId(), measureUrls));
            }
            var validatedPeriod = validateAndConvertMeasurementPeriod(measurementPeriodParams, elmLibrary, measureUrls);
            if (validatedPeriod != null) {
                parametersMap.put(MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, validatedPeriod);
            }
        } else {
            resolveDefaultMeasurementPeriodWithLibraryStack(
                    context, libraryIdentifiers, firstLibraryId, measureUrls, parametersMap);
        }
    }

    /**
     * Resolve the CQL-default measurement period by pushing libraries onto the CQL engine stack,
     * evaluating the parameter default, UTC-cloning the result, and popping the libraries.
     * <p/>
     * <b>Why the push/pop ceremony is required:</b>
     * <p/>
     * The CQL engine's {@code visitExpression()} — used internally by {@code visitParameterDef()} to
     * evaluate a parameter's {@code default} expression — requires {@code state.getCurrentLibrary()}
     * to be non-null. The library stack is accessed in two places during expression evaluation:
     * <ol>
     *   <li>Error handling ({@code EvaluationVisitor.kt}) — building source backtraces on exception</li>
     *   <li>Coverage reporting ({@code State.kt}) — marking elements as visited</li>
     * </ol>
     * If no library is on the stack, these paths throw a {@code NullPointerException}. There is
     * currently no CQL engine API to evaluate a parameter default without a library on the stack.
     * <p/>
     * <b>To remove this workaround:</b> The CQL engine needs a dedicated API such as
     * {@code CqlEngine.resolveParameterDefault(VersionedIdentifier, String)} that internally
     * manages the library stack, so callers don't need to push/pop libraries themselves.
     *
     * @deprecated This method exists only because the CQL engine lacks a clean API for resolving
     *     parameter defaults. Replace with {@code CqlEngine.resolveParameterDefault()} once it
     *     is available in the CQL engine.
     */
    @Deprecated(forRemoval = true)
    private static void resolveDefaultMeasurementPeriodWithLibraryStack(
            CqlEngine context,
            List<VersionedIdentifier> libraryIdentifiers,
            VersionedIdentifier firstLibraryId,
            List<String> measureUrls,
            Map<String, Object> parametersMap) {
        var compiledLibraries = LibraryInitHandler.initLibraries(context, libraryIdentifiers);
        try {
            var elmLibrary = compiledLibraries.get(0).getLibrary();
            if (elmLibrary == null) {
                throw new InternalErrorException(
                        "Compiled library has no ELM content for identifier: %s, measure URLs: %s"
                                .formatted(firstLibraryId.getId(), measureUrls));
            }
            var defaultPeriod = resolveAndCloneDefaultMeasurementPeriod(context, elmLibrary);
            if (defaultPeriod != null) {
                parametersMap.put(MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, defaultPeriod);
            }
        } finally {
            LibraryInitHandler.popLibraries(context, compiledLibraries);
        }
    }

    /**
     * Validate and convert a user-provided measurement period against the CQL library's parameter type.
     * Does not require CQL engine state — takes the ELM Library directly.
     *
     * @param measurementPeriod user-provided measurement period (may be null)
     * @param elmLibrary the ELM library to check parameter type against
     * @param measureUrls measure URLs for error messages
     * @return the validated/converted measurement period, or null if not provided
     */
    @Nullable
    public static Interval validateAndConvertMeasurementPeriod(
            @Nullable Interval measurementPeriod, Library elmLibrary, List<String> measureUrls) {
        if (measurementPeriod == null) {
            return null;
        }

        ParameterDef pd = findMeasurementPeriodParameterDef(elmLibrary);
        if (pd == null) {
            logger.warn(
                    "Parameter \"{}\" was not found. Unable to validate type.",
                    MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            return measurementPeriod;
        }

        IntervalTypeSpecifier intervalTypeSpecifier = (IntervalTypeSpecifier) pd.getParameterTypeSpecifier();
        if (intervalTypeSpecifier == null) {
            logger.debug(
                    "No ELM type information available. Unable to validate type of \"{}\"",
                    MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            return measurementPeriod;
        }

        if (!(intervalTypeSpecifier.getPointType() instanceof NamedTypeSpecifier pointType)
                || pointType.getName() == null) {
            throw new InternalErrorException(
                    "\"Measurement Period\" parameter has unexpected type specifier for measure URLs: %s"
                            .formatted(measureUrls));
        }
        String targetType = pointType.getName().getLocalPart();
        return convertInterval(measurementPeriod, targetType, measureUrls);
    }

    /**
     * Resolve the CQL default measurement period, UTC-clone it, and return it.
     * <p/>
     * Uses the deprecated {@code CqlEngine.getEvaluationVisitor()} because no non-deprecated
     * CQL API exists for evaluating parameter defaults. When the CQL engine exposes a stable API
     * for this purpose, replace this method.
     *
     * @param context CQL engine with library on the stack
     * @param elmLibrary the ELM library containing the parameter definition
     * @return the UTC-cloned default measurement period, or null if no default is defined
     */
    @SuppressWarnings({"deprecation", "removal"})
    @Nullable
    public static Interval resolveAndCloneDefaultMeasurementPeriod(CqlEngine context, Library elmLibrary) {
        ParameterDef pd = findMeasurementPeriodParameterDef(elmLibrary);
        if (pd == null || pd.getDefault() == null) {
            return null;
        }
        var libraryId = Optional.ofNullable(elmLibrary.getIdentifier())
                .map(VersionedIdentifier::getId)
                .orElse("unknown");
        var evaluationVisitor = context.getEvaluationVisitor();
        var result = evaluationVisitor.visitParameterDef(pd, context.getState());
        if (!(result instanceof Interval defaultPeriod)) {
            throw new InternalErrorException(
                    "\"Measurement Period\" default resolved to %s instead of Interval for library: %s"
                            .formatted(
                                    result == null ? "null" : result.getClass().getSimpleName(), libraryId));
        }
        return cloneIntervalWithUtc(defaultPeriod);
    }

    /**
     * Find the "Measurement Period" parameter definition in a given ELM library.
     * Does not require CQL engine state.
     */
    @Nullable
    static ParameterDef findMeasurementPeriodParameterDef(Library lib) {
        if (lib == null
                || lib.getParameters() == null
                || lib.getParameters().getDef().isEmpty()) {
            return null;
        }

        for (ParameterDef pd : lib.getParameters().getDef()) {
            if (Objects.equals(pd.getName(), MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME)) {
                return pd;
            }
        }

        return null;
    }

    public static Interval convertInterval(Interval interval, String targetType, List<String> measureUrls) {
        var pointType = interval.getPointType();
        if (pointType == null) {
            throw new InternalErrorException(
                    "Measurement period interval has no point type for measure URLs: %s".formatted(measureUrls));
        }
        String sourceTypeQualified = pointType.getTypeName();
        String sourceType = sourceTypeQualified.substring(sourceTypeQualified.lastIndexOf(".") + 1);
        if (sourceType.equals(targetType)) {
            return interval;
        }

        if (sourceType.equals("DateTime") && targetType.equals("Date")) {
            if (interval.getLow() == null || interval.getHigh() == null) {
                throw new InternalErrorException(
                        "Interval has no low or high values for measure URLs: %s".formatted(measureUrls));
            }
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

    public static Date truncateDateTime(DateTime dateTime) {
        OffsetDateTime odt = dateTime.getDateTime();
        if (odt == null) {
            throw new InternalErrorException("dateTime was null");
        }
        return new Date(odt.getYear(), odt.getMonthValue(), odt.getDayOfMonth());
    }

    public static Interval buildMeasurementPeriod(String periodStart, String periodEnd) {
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
