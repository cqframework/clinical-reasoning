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

    private MeasureProcessorTimeUtils() {}

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
            resolveDefaultMeasurementPeriod(context, firstLibraryId, parametersMap);
        }
    }

    /**
     * Resolve the CQL-default measurement period using the CQL engine's
     * {@code resolveParameterDefault()} API, UTC-clone it, and add it to the parameters map.
     *
     * @param context CQL engine context
     * @param firstLibraryId the first library identifier to resolve the parameter against
     * @param parametersMap mutable parameters map to add the measurement period to
     */
    private static void resolveDefaultMeasurementPeriod(
            CqlEngine context, VersionedIdentifier firstLibraryId, Map<String, Object> parametersMap) {
        // Pre-check: resolve the ELM library and verify the Measurement Period parameter exists.
        // If the library can't be resolved (e.g., missing CQL/ELM content), the exception propagates.
        var elmLibrary = context.getEnvironment().resolveLibrary(firstLibraryId);
        if (elmLibrary == null || findMeasurementPeriodParameterDef(elmLibrary) == null) {
            return;
        }

        var result =
                context.resolveParameterDefault(firstLibraryId, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
        if (result == null) {
            return;
        }

        if (!(result instanceof Interval defaultPeriod)) {
            throw new InternalErrorException(
                    "\"Measurement Period\" default resolved to %s instead of Interval for library: %s"
                            .formatted(result.getClass().getSimpleName(), firstLibraryId.getId()));
        }

        parametersMap.put(MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, cloneIntervalWithUtc(defaultPeriod));
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
