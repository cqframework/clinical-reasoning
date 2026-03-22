package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationException;
import org.opencds.cqf.fhir.cr.measure.common.MeasureException;
import org.opencds.cqf.fhir.cr.measure.common.MeasureResolutionException;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoringException;
import org.opencds.cqf.fhir.cr.measure.common.MeasureValidationException;

/**
 * Translates domain-layer {@link MeasureException} subtypes into HAPI FHIR
 * transport-layer exceptions with appropriate HTTP status codes.
 *
 * <p>This is the single boundary where domain exceptions become HTTP responses.
 * Domain and FHIR-translation code must never throw transport exceptions directly.
 */
public final class MeasureExceptionMapper {

    private MeasureExceptionMapper() {}

    /**
     * Converts a {@link MeasureException} to the appropriate HAPI FHIR
     * {@link BaseServerResponseException}.
     *
     * @param e the domain exception
     * @return a transport exception with the correct HTTP status code
     */
    public static BaseServerResponseException map(MeasureException e) {
        if (e instanceof MeasureValidationException) {
            return new InvalidRequestException(e.getMessage(), e);
        }
        if (e instanceof MeasureResolutionException) {
            var ex = new ResourceNotFoundException(e.getMessage());
            ex.initCause(e);
            return ex;
        }
        if (e instanceof MeasureScoringException) {
            return new InternalErrorException(e.getMessage(), e);
        }
        if (e instanceof MeasureEvaluationException) {
            return new InternalErrorException(e.getMessage(), e);
        }
        // Fallback for any future MeasureException subclass
        return new InternalErrorException(e.getMessage(), e);
    }
}
