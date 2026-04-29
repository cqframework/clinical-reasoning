package org.opencds.cqf.fhir.cr.helpers;

import ca.uhn.fhir.context.FhirContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

/**
 * Honors the CDC opioid-cds {@code dataDateRoller} extension on test fixtures
 * by shifting a resource's date fields forward so the data stays "current"
 * relative to today's wall clock. Without this, fixtures with hard-coded dates
 * eventually fall outside CQL windows like
 * {@code where date from Rx.authoredOn 2 years or less on or before Today()}
 * and tests rot.
 *
 * <p>The extension structure (see fixture JSON) is:
 * <pre>
 * extension: [{
 *   url: "<a href="http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/dataDateRoller">...</a>",
 *   extension: [
 *     { url: "dateLastUpdated", valueDateTime: "2023-04-28" },
 *     { url: "frequency",       valueDuration: { value: 30.0, unit: "days", ... } }
 *   ]
 * }]
 * </pre>
 *
 * <p>This helper reads {@code dateLastUpdated}, computes
 * {@code offsetDays = today − dateLastUpdated}, and shifts known date fields
 * for each supported resource type by that many days. The {@code frequency}
 * sub-extension is informational and not currently used. After rolling, the
 * {@code dataDateRoller} extension is stripped so subsequent reads are no-ops
 * (idempotent).
 */
public final class DataDateRollerHelper {

    public static final String EXT_URL = "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/dataDateRoller";
    public static final String EXT_DATE_LAST_UPDATED = "dateLastUpdated";

    /**
     * Date fields we shift forward when a resource carries the
     * {@code dataDateRoller} extension. Intentionally narrow:
     * <ul>
     *   <li>{@code MedicationRequest} dates are rolled because CQL "active
     *       prescription" windows like
     *       {@code where date from Rx.authoredOn 2 years or less on or before Today()}
     *       require them to stay near "now".</li>
     *   <li>{@code Observation} dates are NOT rolled. The opioid-cds tests
     *       depend on their historical observations staying outside windows
     *       like "in last 12 months" — rolling them forward would silently
     *       flip the recommendation branch (e.g. trigger "Positive Cocaine"
     *       instead of "Annual Urine Screening Check"). The fixtures keep
     *       the {@code dataDateRoller} extension for documentation, but the
     *       helper ignores it on Observations.</li>
     *   <li>{@code Patient.birthDate} is not rolled either; patient age is
     *       not gated by these tests.</li>
     * </ul>
     * Add a resource type here only when a test's CQL has a wall-clock
     * window that needs the data to stay current.
     */
    private static final Map<String, List<String>> ROLLABLE_PATHS = Map.of(
            "MedicationRequest",
            List.of("authoredOn", "dispenseRequest.validityPeriod.start", "dispenseRequest.validityPeriod.end"));

    private DataDateRollerHelper() {}

    /**
     * If the resource carries a {@code dataDateRoller} extension, shift its date
     * fields forward by {@code today − dateLastUpdated} days and strip the
     * extension. No-op for resources without the extension or whose type is not
     * in {@link #ROLLABLE_PATHS}.
     */
    public static void rollIfAnnotated(IBaseResource resource, LocalDate today, FhirContext ctx) {
        if (!(resource instanceof IBaseHasExtensions hasExtensions)) {
            return;
        }
        IBaseExtension<?, ?> rollerExt = findExtension(hasExtensions, EXT_URL);
        if (rollerExt == null) {
            return;
        }
        IBaseExtension<?, ?> anchorExt = findExtension(rollerExt, EXT_DATE_LAST_UPDATED);
        if (anchorExt == null || !(anchorExt.getValue() instanceof IPrimitiveType<?> anchorPrim)) {
            return;
        }
        LocalDate anchor = toLocalDate(anchorPrim);
        if (anchor == null) {
            return;
        }
        long offsetDays = ChronoUnit.DAYS.between(anchor, today);
        if (offsetDays != 0) {
            List<String> paths = ROLLABLE_PATHS.get(resource.fhirType());
            if (paths != null) {
                paths.forEach(path -> rollAtPath(resource, path, offsetDays, ctx));
            }
            // Resource types not in ROLLABLE_PATHS are intentionally left alone
            // — see ROLLABLE_PATHS Javadoc.
        }
        // Idempotency: subsequent reads of the same cached resource skip rolling.
        hasExtensions.getExtension().removeIf(e -> EXT_URL.equals(e.getUrl()));
    }

    private static IBaseExtension<?, ?> findExtension(IBaseHasExtensions container, String url) {
        for (var ext : container.getExtension()) {
            if (url.equals(ext.getUrl())) {
                return ext;
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static IBaseExtension<?, ?> findExtension(IBaseExtension<?, ?> parent, String url) {
        for (Object raw : parent.getExtension()) {
            if (raw instanceof IBaseExtension ext && url.equals(ext.getUrl())) {
                return ext;
            }
        }
        return null;
    }

    private static void rollAtPath(IBaseResource resource, String path, long offsetDays, FhirContext ctx) {
        List<IPrimitiveType> primitives;
        try {
            primitives = ctx.newTerser().getValues(resource, path, IPrimitiveType.class);
        } catch (RuntimeException e) {
            // Path not valid for this FHIR version (e.g. effectiveInstant on DSTU3). Skip.
            return;
        }
        for (IPrimitiveType<?> primitive : primitives) {
            rollPrimitive(primitive, offsetDays);
        }
    }

    private static void rollPrimitive(IPrimitiveType<?> primitive, long offsetDays) {
        // Always go through the string form to avoid timezone/DST off-by-one errors
        // that arise when shifting a wall-clock Date through java.time.Instant.
        String stringValue = primitive.getValueAsString();
        if (stringValue == null || stringValue.length() < 10) {
            return;
        }
        try {
            LocalDate shiftedDate =
                    LocalDate.parse(stringValue.substring(0, 10)).plusDays(offsetDays);
            String tail = stringValue.substring(10);
            primitive.setValueAsString(shiftedDate + tail);
        } catch (RuntimeException ignored) {
            // Non-date string (shouldn't happen for date fields), leave as-is.
        }
    }

    private static LocalDate toLocalDate(IPrimitiveType<?> primitive) {
        String stringValue = primitive.getValueAsString();
        if (stringValue == null || stringValue.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(stringValue.substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
