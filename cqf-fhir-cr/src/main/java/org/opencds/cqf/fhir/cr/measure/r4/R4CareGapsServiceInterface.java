package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.utility.monad.Either3;

/**
 * Describe operations provided by caregaps services for downstream implementors
 */
public interface R4CareGapsServiceInterface {

    Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String subject,
            List<String> status,
            List<IdType> measureId,
            List<String> measureIdentifier,
            List<CanonicalType> measureUrl,
            boolean notDocument);

    List<Either3<IdType, String, CanonicalType>> liftMeasureParameters(
            List<IdType> measureId, List<String> measureIdentifier, List<CanonicalType> measureUrl);
}
