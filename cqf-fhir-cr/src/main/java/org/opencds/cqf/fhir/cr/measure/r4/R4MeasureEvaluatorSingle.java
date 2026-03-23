package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;

/**
 * Interface to capture measure evaluation on a single measure.
 */
public interface R4MeasureEvaluatorSingle {

    MeasureReport evaluate(
            IdType measureId,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String practitioner);
}
