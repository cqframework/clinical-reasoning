package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.DateUtils;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used immediately after receiving a REST call by $evaluate-measure and any potential variants to validate and convert
 * period start and end inputs to timezones with offsets.  The offset is determined from the request header a value for "Timezone".
 * <p/>
 * This class takes a fallback timezone that's used in case the request header does not contain a value for "Timezone".
 * <p/>
 * Currently, these are the date/time formats supported:
 * <ol>
 *     <li>yyyy</li>
 *     <li>yyyy-MM</li>
 *     <li>yyyy-MM-dd</li>
 *     <li>yyyy-MM-ddTHH:mm:ss</li>
 * </ol>
 * <p/>
 * Also used for various operations to serialize/deserialize dates to/from JSON classes.
 */
public class StringTimePeriodHandler {
    private static final Logger ourLog = LoggerFactory.getLogger(StringTimePeriodHandler.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_INPUT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_MM_INPUT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_MM_DD_INPUT = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS_INPUT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final DateTimeFormatter DATE_TIME_FORMATTER_JSON_SERIALIZE = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final Map<Integer, DateTimeFormatter> VALID_DATE_TIME_FORMATTERS_BY_FORMAT_LENGTH = Map.of(
            4, DATE_TIME_FORMATTER_YYYY_INPUT,
            7, DATE_TIME_FORMATTER_YYYY_MM_INPUT,
            10, DATE_TIME_FORMATTER_YYYY_MM_DD_INPUT,
            19, DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS_INPUT);

    // The default, in order to signal to clinical-reasoning that none is set
    private final ZoneId fallbackTimezone;

    public StringTimePeriodHandler(ZoneId fallbackTimezone) {
        this.fallbackTimezone = fallbackTimezone;
    }

    /**
     * Meant to serialize a ZonedDateTime into a String to pass to a JSON object.
     */
    public String serialize(ZonedDateTime zoneDateTime) {
        return DATE_TIME_FORMATTER_JSON_SERIALIZE.format(zoneDateTime);
    }

    /**
     * Meant to deserialize a String from a JSON object back into a ZonedDateTime.
     */
    public ZonedDateTime deSerialize(String inputDateString) {
        return ZonedDateTime.parse(inputDateString, DATE_TIME_FORMATTER_JSON_SERIALIZE);
    }

    /**
     * Get the start period as a parsed ZoneDateTime (ex 2024 to 2024-01-01T00:00:00-07:00).
     *
     * @param inputDateTimeString A String representation of the period start date in yyyy, yyyy-MM, YYYY-MM-dd, or yyyy-MM-ddTHH:mm:ss
     * @param requestDetails RequestDetails that may or may not contain a Timezone header
     * @return the parsed start date/time with zone info
     */
    @Nullable
    public ZonedDateTime getStartZonedDateTime(@Nullable String inputDateTimeString, RequestDetails requestDetails) {
        ourLog.debug("transforming String start date: {} to ZonedDateTime", inputDateTimeString);
        return getStartZonedDateTime(inputDateTimeString, getClientTimezoneOrInvalidRequest(requestDetails));
    }

    /**
     * Get the start period as a parsed ZoneDateTime (ex 2024 to 2024-01-01T00:00:00-07:00).
     *
     * @param inputDateTimeString A String representation of the period start date in yyyy, yyyy-MM, YYYY-MM-dd, or yyyy-MM-ddTHH:mm:ss
     * @param timezone A ZoneId with which to convert the timestamp
     * @return the parsed start date/time with zone info
     */
    @Nullable
    public ZonedDateTime getStartZonedDateTime(@Nullable String inputDateTimeString, ZoneId timezone) {
        return getZonedDateTime(
                inputDateTimeString,
                timezone,
                true,
                // start date/time
                DateUtils::extractLocalDateTimeForRangeStartOrEmpty);
    }

    /**
     * Get the end period as a parsed ZoneDateTime (ex 2024 to 2024-12-31T23:59:59-07:00).
     *
     * @param inputDateTimeString A String representation of the period start date in yyyy, yyyy-MM, YYYY-MM-dd, or yyyy-MM-ddTHH:mm:ss
     * @param requestDetails RequestDetails that may or may not contain a Timezone header
     * @return the parsed end date/time with zone info
     */
    @Nullable
    public ZonedDateTime getEndZonedDateTime(@Nullable String inputDateTimeString, RequestDetails requestDetails) {
        ourLog.debug("transforming String end date: {} to ZonedDateTime", inputDateTimeString);
        return getEndZonedDateTime(inputDateTimeString, getClientTimezoneOrInvalidRequest(requestDetails));
    }

    /**
     * Get the end period as a parsed ZoneDateTime (ex 2024 to 2024-12-31T23:59:59-07:00).
     *
     * @param inputDateTimeString A String representation of the period start date in yyyy, yyyy-MM, YYYY-MM-dd, or yyyy-MM-ddTHH:mm:ss
     * @param timezone A ZoneId with which to convert the timestamp
     * @return the parsed end date/time with zone info
     */
    @Nullable
    public ZonedDateTime getEndZonedDateTime(@Nullable String inputDateTimeString, ZoneId timezone) {
        return getZonedDateTime(
                inputDateTimeString,
                timezone,
                false,
                // end date/time
                DateUtils::extractLocalDateTimeForRangeEndOrEmpty);
    }

    private ZonedDateTime getZonedDateTime(
            String inputDateTimeString,
            ZoneId timezone,
            boolean isStart,
            Function<TemporalAccessor, Optional<LocalDateTime>> startOrEndExtractFunction) {

        // We may pass null periods to clinical-reasoning
        if (inputDateTimeString == null) {
            return null;
        }

        final DateTimeFormatter dateTimeFormat = validateAndGetDateTimeFormat(inputDateTimeString);

        final LocalDateTime localDateTime =
                validateAndGetLocalDateTime(inputDateTimeString, dateTimeFormat, startOrEndExtractFunction, isStart);

        final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, timezone);

        ourLog.debug(
                "successfully transformed String date: {} to ZonedDateTime: {}", inputDateTimeString, zonedDateTime);

        return zonedDateTime;
    }

    private LocalDateTime validateAndGetLocalDateTime(
            String period,
            DateTimeFormatter dateTimeFormatter,
            Function<TemporalAccessor, Optional<LocalDateTime>> temporalAccessorToLocalDateTimeConverter,
            boolean isStart) {
        return DateUtils.parseDateTimeStringIfValid(period, dateTimeFormatter)
                .flatMap(temporalAccessorToLocalDateTimeConverter)
                .orElseThrow(() -> {
                    ourLog.warn(
                            "Period {}: {} has an unsupported format",
                            isStart ? "start" : "end",
                            period);

                    return new InvalidRequestException(String.format(
                            "Period %s: %s has an unsupported format",
                            isStart ? "start" : "end", period));
                });
    }

    private DateTimeFormatter validateAndGetDateTimeFormat(String inputDateTimeString) {
        final DateTimeFormatter dateTimeFormatter =
                VALID_DATE_TIME_FORMATTERS_BY_FORMAT_LENGTH.get(inputDateTimeString.length());

        if (dateTimeFormatter == null) {
            ourLog.warn("Unsupported Date/Time format for input: {}", inputDateTimeString);

            throw new InvalidRequestException(
                    String.format("Unsupported Date/Time format for input: %s", inputDateTimeString));
        }

        return dateTimeFormatter;
    }

    private ZoneId getClientTimezoneOrInvalidRequest(RequestDetails requestDetails) {
        final String clientTimezoneString = requestDetails.getHeader(Constants.HEADER_CLIENT_TIMEZONE);

        if (Strings.isNotBlank(clientTimezoneString)) {
            try {
                return ZoneId.of(clientTimezoneString);
            } catch (Exception exception) {
                ourLog.warn("Invalid value for Timezone header: {}", clientTimezoneString);
                throw new InvalidRequestException(
                        String.format("Invalid value for Timezone header: %s", clientTimezoneString));
            }
        }

        return fallbackTimezone;
    }
}
