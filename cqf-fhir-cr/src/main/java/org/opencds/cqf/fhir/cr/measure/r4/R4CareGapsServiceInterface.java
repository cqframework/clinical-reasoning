package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.monad.Either3;

/**
 * Describe operations provided by caregaps services for downstream implementors
 */
@SuppressWarnings("squid:S107")
public interface R4CareGapsServiceInterface<
        C extends IPrimitiveType<String>, I extends IIdType, P extends IBaseParameters> {

    P getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String subject,
            List<String> status,
            List<I> measureId,
            List<String> measureIdentifier,
            List<C> measureUrl,
            boolean notDocument);

    List<Either3<I, String, C>> liftMeasureParameters(
            List<I> measureId, List<String> measureIdentifier, List<C> measureUrl);
}
