package org.opencds.cqf.fhir.cr.hapi.dstu3.measure;

import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEnvironment;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;

/**
 * Maps DSTU3 FHIR operation parameters into version-agnostic domain records
 * ({@link MeasureEvaluationRequest} and {@link MeasureEnvironment}).
 */
public class Dstu3MeasureOperationMapper {
    private final StringTimePeriodHandler timePeriodHandler;

    public Dstu3MeasureOperationMapper(StringTimePeriodHandler timePeriodHandler) {
        this.timePeriodHandler = timePeriodHandler;
    }

    public MeasureEvaluationRequest toRequest(
            String periodStart,
            String periodEnd,
            String reportType,
            String subject,
            String practitioner,
            String lastReceivedOn,
            String productLine,
            RequestDetails requestDetails) {
        return new MeasureEvaluationRequest(
                timePeriodHandler.getStartZonedDateTime(periodStart, requestDetails),
                timePeriodHandler.getEndZonedDateTime(periodEnd, requestDetails),
                reportType,
                subject,
                practitioner,
                lastReceivedOn,
                productLine);
    }

    public MeasureEnvironment toEnvironment(ParametersParameterComponent terminologyEndpoint, Bundle additionalData) {
        return new MeasureEnvironment(
                null, getEndpoint(FhirVersionEnum.DSTU3, terminologyEndpoint), null, additionalData);
    }
}
