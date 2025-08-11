package org.opencds.cqf.fhir.cr.hapi.r4.questionnaire;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

@SuppressWarnings("java:S107")
public class QuestionnairePopulateProvider {
    private final IQuestionnaireProcessorFactory questionnaireProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public QuestionnairePopulateProvider(IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        this.questionnaireProcessorFactory = questionnaireProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements the <a href=
     * "http://build.fhir.org/ig/HL7/sdc/OperationDefinition-Questionnaire-populate.html">$populate</a>
     * operation found in the
     * <a href="http://build.fhir.org/ig/HL7/sdc/index.html">Structured Data Capture (SDC) IG</a>.
     *
     * @param id                  The id of the Questionnaire to populate.
     * @param questionnaire       The Questionnaire to populate. Used when the operation is invoked at the 'type' level.
     * @param canonical           The canonical identifier for the questionnaire (optionally version-specific).
     * @param url             	 Canonical URL of the Questionnaire when invoked at the resource type level. This is exclusive with the questionnaire and canonical parameters.
     * @param version             Version of the Questionnaire when invoked at the resource type level. This is exclusive with the questionnaire and canonical parameters.
     * @param subject             The subject(s) that is/are the target of the Questionnaire.
     * @param context			 Resources containing information to be used to help populate the QuestionnaireResponse.
     * @param launchContext       The Questionnaire Launch Context extension containing Resources that provide context for form processing logic (pre-population) when creating/displaying/editing a QuestionnaireResponse.
     * @param parameters			 Any input parameters defined in libraries referenced by the Questionnaire.
     * @param local				 Whether the server should use what resources and other knowledge it has about the referenced subject when pre-populating answers to questions.
     * @param useServerData       Whether to use data from the server performing the evaluation.
     * @param data                Data to be made available during CQL evaluation.
     * @param bundle              Legacy support for data parameter.
     * @param dataEndpoint        The FHIR {@link Endpoint} Endpoint resource or url to use to access data referenced by retrieve operations in libraries
     *                               referenced by the Questionnaire.
     * @param contentEndpoint     The FHIR {@link Endpoint} Endpoint resource or url to use to access content (i.e. libraries) referenced by the Questionnaire.
     * @param terminologyEndpoint The FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, and membership testing)
     *                               referenced by the Questionnaire.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                               autopopulated HAPI.
     * @return The partially (or fully)-populated set of answers for the specified Questionnaire.
     */
    @Operation(name = ProviderConstants.CR_OPERATION_POPULATE, idempotent = true, type = Questionnaire.class)
    public QuestionnaireResponse populate(
            @IdParam IdType id,
            @OperationParam(name = "questionnaire") Questionnaire questionnaire,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "subject") Reference subject,
            @OperationParam(name = "context") List<Parameters.ParametersParameterComponent> context,
            @OperationParam(name = "launchContext") Extension launchContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "local") BooleanType local,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "bundle") Bundle bundle,
            @OperationParam(name = "dataEndpoint") Parameters.ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") Parameters.ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        CanonicalType canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        Bundle dataToUse = data == null ? bundle : data;
        var subjectId = subject == null ? null : subject.getReference();
        var dataEndpointParam = getEndpoint(fhirVersion, dataEndpoint);
        var contentEndpointParam = getEndpoint(fhirVersion, contentEndpoint);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        return (QuestionnaireResponse) questionnaireProcessorFactory
                .create(requestDetails)
                .populate(
                        Eithers.for3(canonicalType, id, questionnaire),
                        subjectId,
                        context,
                        launchContext,
                        parameters,
                        dataToUse,
                        isUseServerData(local, useServerData),
                        dataEndpointParam,
                        contentEndpointParam,
                        terminologyEndpointParam);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_POPULATE, idempotent = true, type = Questionnaire.class)
    public QuestionnaireResponse populate(
            @OperationParam(name = "questionnaire") Questionnaire questionnaire,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "subject") Reference subject,
            @OperationParam(name = "context") List<Parameters.ParametersParameterComponent> context,
            @OperationParam(name = "launchContext") Extension launchContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "local") BooleanType local,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "bundle") Bundle bundle,
            @OperationParam(name = "dataEndpoint") Parameters.ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") Parameters.ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        CanonicalType canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        Bundle dataToUse = data == null ? bundle : data;
        var subjectId = subject == null ? null : subject.getReference();
        var dataEndpointParam = getEndpoint(fhirVersion, dataEndpoint);
        var contentEndpointParam = getEndpoint(fhirVersion, contentEndpoint);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        return (QuestionnaireResponse) questionnaireProcessorFactory
                .create(requestDetails)
                .populate(
                        Eithers.for3(canonicalType, null, questionnaire),
                        subjectId,
                        context,
                        launchContext,
                        parameters,
                        dataToUse,
                        isUseServerData(local, useServerData),
                        dataEndpointParam,
                        contentEndpointParam,
                        terminologyEndpointParam);
    }

    private boolean isUseServerData(BooleanType local, BooleanType useServerData) {
        if (local != null) {
            return local.booleanValue();
        }

        if (useServerData != null) {
            return useServerData.booleanValue();
        }

        return true;
    }
}
