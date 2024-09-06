package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;

/**
 * Interface for {@link Dstu3MeasureService} and any other concrete classes that implement the same
 * signature.
 */
public interface Dstu3MeasureEvaluatorSingle {

    MeasureReport evaluateMeasure(
        IdType id,
        String periodStart,
        String periodEnd,
        String reportType,
        String subject,
        String practitioner,
        String lastReceivedOn,
        String productLine,
        Bundle additionalData,
        Parameters parameters,
        Endpoint terminologyEndpoint);
}
