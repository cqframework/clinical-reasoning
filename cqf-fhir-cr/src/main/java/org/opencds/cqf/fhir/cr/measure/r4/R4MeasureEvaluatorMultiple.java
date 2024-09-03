package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import java.util.List;

/**
 * Interface for {@link R4MultiMeasureService} and any other concrete classes that implement the same
 * signature.
 */
public interface R4MeasureEvaluatorMultiple {

    Bundle evaluate(
        List<IdType> measureId,
        List<String> measureUrl,
        String periodStart,
        String periodEnd,
        String reportType,
        String subject, // practitioner passed in here
        Endpoint contentEndpoint,
        Endpoint terminologyEndpoint,
        Endpoint dataEndpoint,
        Bundle additionalData,
        Parameters parameters,
        String productLine,
        String reporter);
}
