package org.opencds.cqf.fhir.cr.hapi.r4.measure;

import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEnvironment;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;

/**
 * Maps FHIR R4 operation parameters into version-agnostic domain records for measure evaluation.
 */
public class R4MeasureOperationMapper {
    private final StringTimePeriodHandler timePeriodHandler;

    public R4MeasureOperationMapper(StringTimePeriodHandler timePeriodHandler) {
        this.timePeriodHandler = timePeriodHandler;
    }

    public MeasureEvaluationRequest toRequest(
            String periodStart,
            String periodEnd,
            String reportType,
            String subjectId,
            String practitioner,
            String lastReceivedOn,
            String productLine,
            RequestDetails requestDetails) {
        return new MeasureEvaluationRequest(
                timePeriodHandler.getStartZonedDateTime(periodStart, requestDetails),
                timePeriodHandler.getEndZonedDateTime(periodEnd, requestDetails),
                reportType,
                subjectId,
                practitioner,
                lastReceivedOn,
                productLine);
    }

    public MeasureEnvironment toEnvironment(
            ParametersParameterComponent contentEndpoint,
            ParametersParameterComponent terminologyEndpoint,
            ParametersParameterComponent dataEndpoint,
            Bundle additionalData) {
        return new MeasureEnvironment(
                getEndpoint(FhirVersionEnum.R4, contentEndpoint),
                getEndpoint(FhirVersionEnum.R4, terminologyEndpoint),
                getEndpoint(FhirVersionEnum.R4, dataEndpoint),
                additionalData);
    }
}
