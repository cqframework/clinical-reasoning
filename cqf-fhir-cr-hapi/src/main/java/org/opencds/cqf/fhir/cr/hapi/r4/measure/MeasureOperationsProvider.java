package org.opencds.cqf.fhir.cr.hapi.r4.measure;

import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureEvaluatorSingleFactory;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class MeasureOperationsProvider {

    private final R4MeasureEvaluatorSingleFactory r4MeasureServiceFactory;
    private final StringTimePeriodHandler stringTimePeriodHandler;

    public MeasureOperationsProvider(
            R4MeasureEvaluatorSingleFactory r4MeasureServiceFactory, StringTimePeriodHandler stringTimePeriodHandler) {
        this.r4MeasureServiceFactory = r4MeasureServiceFactory;
        this.stringTimePeriodHandler = stringTimePeriodHandler;
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
     * @param subject        the subject to use for the evaluation
     * @param practitioner   the practitioner to use for the evaluation
     * @param lastReceivedOn the date the results of this measure were last
     *                          received.
     * @param productLine    the productLine (e.g. Medicare, Medicaid, etc) to use
     *                          for the evaluation. This is a non-standard parameter.
     * @param additionalData the data bundle containing additional data
     * @param requestDetails The details (such as tenant) of this request. Usually
     *                          autopopulated HAPI.
     * @return the calculated MeasureReport
     */
    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE_MEASURE, idempotent = true, type = Measure.class)
    public MeasureReport evaluateMeasure(
            @IdParam IdType id,
            @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd,
            @OperationParam(name = "reportType") String reportType,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "practitioner") String practitioner,
            @OperationParam(name = "lastReceivedOn") String lastReceivedOn,
            @OperationParam(name = "productLine") String productLine,
            @OperationParam(name = "additionalData") Bundle additionalData,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            @OperationParam(name = "parameters") Parameters parameters,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return r4MeasureServiceFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.forMiddle3(id),
                        stringTimePeriodHandler.getStartZonedDateTime(periodStart, requestDetails),
                        stringTimePeriodHandler.getEndZonedDateTime(periodEnd, requestDetails),
                        reportType,
                        subject,
                        lastReceivedOn,
                        null,
                        terminologyEndpoint,
                        null,
                        additionalData,
                        parameters,
                        productLine,
                        practitioner);
    }
}
