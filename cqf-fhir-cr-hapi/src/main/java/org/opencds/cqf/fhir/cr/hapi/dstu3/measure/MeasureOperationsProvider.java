package org.opencds.cqf.fhir.cr.hapi.dstu3.measure;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.fhir.cr.hapi.dstu3.IMeasureServiceFactory;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S107")
public class MeasureOperationsProvider {
    private final IMeasureServiceFactory dstu3MeasureProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public MeasureOperationsProvider(IMeasureServiceFactory dstu3MeasureProcessorFactory) {
        this.dstu3MeasureProcessorFactory = dstu3MeasureProcessorFactory;
        fhirVersion = FhirVersionEnum.DSTU3;
    }

    /**
     * Implements the <a href=
     * "https://www.hl7.org/fhir/operation-measure-evaluate-measure.html">$evaluate-measure</a>
     * operation found in the
     * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>. This implementation aims to be compatible with the CQF
     * IG.
     *
     * @param id             the id of the Measure to evaluate
     * @param periodStart    The start of the reporting period
     * @param periodEnd      The end of the reporting period
     * @param reportType     The type of MeasureReport to generate
     * @param patient        the patient to use as the subject to use for the
     *                          evaluation
     * @param practitioner   the practitioner to use for the evaluation
     * @param lastReceivedOn the date the results of this measure were last
     *                          received.
     * @param productLine    the productLine (e.g. Medicare, Medicaid, etc) to use
     *                          for the evaluation. This is a non-standard parameter.
     * @param additionalData the data bundle containing additional data
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param requestDetails The details (such as tenant) of this request. Usually
     *                          autopopulated HAPI.
     * @return the calculated MeasureReport
     */
    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE_MEASURE, idempotent = true, type = Measure.class)
    public MeasureReport evaluateMeasure(
            @IdParam IdType id,
            @OperationParam(name = "periodStart") DateType periodStart,
            @OperationParam(name = "periodEnd") DateType periodEnd,
            @OperationParam(name = "reportType") CodeType reportType,
            @OperationParam(name = "patient") StringType patient,
            @OperationParam(name = "practitioner") StringType practitioner,
            @OperationParam(name = "lastReceivedOn") DateTimeType lastReceivedOn,
            @OperationParam(name = "productLine") StringType productLine,
            @OperationParam(name = "additionalData") Bundle additionalData,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "parameters") Parameters parameters,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return dstu3MeasureProcessorFactory
                .create(requestDetails)
                .evaluateMeasure(
                        id,
                        getStringValue(periodStart),
                        getStringValue(periodEnd),
                        getStringValue(reportType),
                        getStringValue(patient),
                        getStringValue(practitioner),
                        getStringValue(lastReceivedOn),
                        getStringValue(productLine),
                        additionalData,
                        parameters,
                        (Endpoint) getEndpoint(fhirVersion, terminologyEndpoint));
    }
}
