package org.opencds.cqf.fhir.cr.hapi.r4.measure;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringOrReferenceValue;
import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureEvaluatorMultipleFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureEvaluatorSingleFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

@SuppressWarnings("java:S107")
public class MeasureOperationsProvider {

    private final R4MeasureEvaluatorSingleFactory r4MeasureServiceFactory;
    private final R4MeasureEvaluatorMultipleFactory r4MultiMeasureServiceFactory;
    private final StringTimePeriodHandler stringTimePeriodHandler;
    private final FhirVersionEnum fhirVersion;

    public MeasureOperationsProvider(
            R4MeasureEvaluatorSingleFactory r4MeasureServiceFactory,
            R4MeasureEvaluatorMultipleFactory r4MultiMeasureServiceFactory,
            StringTimePeriodHandler stringTimePeriodHandler) {
        this.r4MeasureServiceFactory = r4MeasureServiceFactory;
        this.r4MultiMeasureServiceFactory = r4MultiMeasureServiceFactory;
        this.stringTimePeriodHandler = stringTimePeriodHandler;
        fhirVersion = FhirVersionEnum.R4;
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
     * @param contentEndpoint     the FHIR {@link Endpoint} Endpoint resource or url to use to access content (i.e. libraries) referenced by the Measure.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param dataEndpoint        the FHIR {@link Endpoint} Endpoint resource or url to use to access data referenced by retrieve operations in libraries
     *                               referenced by the Measure.
     * @param requestDetails The details (such as tenant) of this request. Usually
     *                          autopopulated HAPI.
     * @return the calculated MeasureReport
     */
    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE_MEASURE, idempotent = true, type = Measure.class)
    public MeasureReport evaluateMeasure(
            @IdParam IdType id,
            @OperationParam(name = "periodStart") ParametersParameterComponent periodStart,
            @OperationParam(name = "periodEnd") ParametersParameterComponent periodEnd,
            @OperationParam(name = "reportType") ParametersParameterComponent reportType,
            @OperationParam(name = "subject") ParametersParameterComponent subject,
            @OperationParam(name = "practitioner") ParametersParameterComponent practitioner,
            @OperationParam(name = "lastReceivedOn") ParametersParameterComponent lastReceivedOn,
            @OperationParam(name = "productLine") StringType productLine,
            @OperationParam(name = "additionalData") Bundle additionalData,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "parameters") Parameters parameters,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return r4MeasureServiceFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.forMiddle3(id),
                        stringTimePeriodHandler.getStartZonedDateTime(
                                getStringValue(fhirVersion, periodStart), requestDetails),
                        stringTimePeriodHandler.getEndZonedDateTime(
                                getStringValue(fhirVersion, periodEnd), requestDetails),
                        getStringValue(fhirVersion, reportType),
                        getStringOrReferenceValue(fhirVersion, subject),
                        getStringValue(fhirVersion, lastReceivedOn),
                        (Endpoint) getEndpoint(fhirVersion, contentEndpoint),
                        (Endpoint) getEndpoint(fhirVersion, terminologyEndpoint),
                        (Endpoint) getEndpoint(fhirVersion, dataEndpoint),
                        additionalData,
                        parameters,
                        getStringValue(productLine),
                        getStringOrReferenceValue(fhirVersion, practitioner));
    }

    /**
     * Implements the <a href=
     * "https://hl7.org/fhir/us/davinci-deqm/STU5/OperationDefinition-evaluate.html">$evaluate</a>
     * operation found in the
     * <a href="https://hl7.org/fhir/us/davinci-deqm/STU5/index.html">DEQM IG</a>.
     *
     * @param measureId       the ids of the Measures to evaluate
     * @param measureUrl      the urls of the Measures to evaluate
     * @param measureIdentifier the identifiers of the Measures to evaluate
     * @param measure         the references to the Measures to evaluate
     * @param periodStart     The start of the reporting period
     * @param periodEnd       The end of the reporting period
     * @param reportType      The type of MeasureReport to generate
     * @param subject         the subject to use for the evaluation
     * @param practitioner    the practitioner to use for the evaluation
     * @param lastReceivedOn  the date the results of this measure were last received.
     * @param productLine     the productLine (e.g. Medicare, Medicaid, etc) to use for
     *                          the evaluation. This is a non-standard parameter.
     * @param additionalData      the data bundle containing additional data
     * @param contentEndpoint     the FHIR {@link Endpoint} Endpoint resource or url to use to access content (i.e. libraries) referenced by the Measure.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param dataEndpoint        the FHIR {@link Endpoint} Endpoint resource or url to use to access data referenced by retrieve operations in libraries
     *                               referenced by the Measure.
     * @param parameters      additional parameters for evaluation
     * @param reporter        The reporter for this evaluation, if applicable.
     * @return a Parameters resource containing a Bundle for each Measure evaluated.
     */
    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE, idempotent = true, type = Measure.class)
    public Parameters evaluate(
            @OperationParam(name = "measureId") List<IdType> measureId,
            @OperationParam(name = "measureUrl") List<StringType> measureUrl,
            @OperationParam(name = "measureIdentifier") List<StringType> measureIdentifier,
            @OperationParam(name = "measure") List<StringType> measure,
            @OperationParam(name = "periodStart") ParametersParameterComponent periodStart,
            @OperationParam(name = "periodEnd") ParametersParameterComponent periodEnd,
            @OperationParam(name = "reportType") ParametersParameterComponent reportType,
            @OperationParam(name = "subject") ParametersParameterComponent subject,
            @OperationParam(name = "practitioner") ParametersParameterComponent practitioner,
            @OperationParam(name = "lastReceivedOn") ParametersParameterComponent lastReceivedOn,
            @OperationParam(name = "productLine") StringType productLine,
            @OperationParam(name = "additionalData") Bundle additionalData,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "reporter") StringType reporter,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return r4MultiMeasureServiceFactory
                .create(requestDetails)
                .evaluate(
                        measureId, // List<IdType>
                        measureUrl == null
                                ? null
                                : measureUrl.stream()
                                        .map(ParameterHelper::getStringValue)
                                        .toList(), // List<String>
                        measureIdentifier == null
                                ? null
                                : measureIdentifier.stream()
                                        .map(ParameterHelper::getStringValue)
                                        .toList(), // List<Identifier>
                        stringTimePeriodHandler.getStartZonedDateTime(
                                getStringValue(fhirVersion, periodStart), requestDetails),
                        stringTimePeriodHandler.getEndZonedDateTime(
                                getStringValue(fhirVersion, periodEnd), requestDetails),
                        getStringValue(fhirVersion, reportType),
                        getStringOrReferenceValue(fhirVersion, subject),
                        (Endpoint) getEndpoint(fhirVersion, contentEndpoint),
                        (Endpoint) getEndpoint(fhirVersion, terminologyEndpoint),
                        (Endpoint) getEndpoint(fhirVersion, dataEndpoint),
                        additionalData,
                        parameters,
                        getStringValue(productLine),
                        getStringValue(reporter));
    }
}
