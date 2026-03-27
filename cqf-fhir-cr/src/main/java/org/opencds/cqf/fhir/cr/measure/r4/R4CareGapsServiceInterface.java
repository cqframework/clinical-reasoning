package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Describe operations provided by caregaps services for downstream implementors
 */
@SuppressWarnings("squid:S107")
public interface R4CareGapsServiceInterface {

    Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String subject,
            List<String> status,
            List<MeasureReference> measureRefs,
            boolean notDocument);
}
