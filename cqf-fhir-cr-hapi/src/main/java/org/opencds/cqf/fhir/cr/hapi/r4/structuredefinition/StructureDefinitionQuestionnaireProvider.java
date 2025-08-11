package org.opencds.cqf.fhir.cr.hapi.r4.structuredefinition;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

@SuppressWarnings("java:S107")
public class StructureDefinitionQuestionnaireProvider {
    private final IQuestionnaireProcessorFactory questionnaireProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public StructureDefinitionQuestionnaireProvider(IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        this.questionnaireProcessorFactory = questionnaireProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements the <a href=
     * "https://hl7.org/fhir/structuredefinition-operation-questionnaire.html">$questionnaire</a>
     * operation.
     *
     * @param id                  The id of the StructureDefinition.
     * @param profile 			 The StructureDefinition to base the Questionnaire on. Used when the operation is invoked at the 'type' level.
     * @param canonical           The canonical identifier for the StructureDefinition (optionally version-specific).
     * @param url             	 Canonical URL of the StructureDefinition when invoked at the resource type level. This is exclusive with the profile and canonical parameters.
     * @param version             Version of the StructureDefinition when invoked at the resource type level. This is exclusive with the profile and canonical parameters.
     * @param supportedOnly       If true (default: false), the questionnaire will only include those elements marked as "mustSupport='true'" in the StructureDefinition.
     * @param requiredOnly        If true (default: false), the questionnaire will only include those elements marked as "min>0" in the StructureDefinition.
     * @param contentEndpoint     The FHIR {@link Endpoint} Endpoint resource or url to use to access content (i.e. libraries) referenced by the StructureDefinition.
     * @param terminologyEndpoint The FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, and membership testing)
     *                               referenced by the StructureDefinition.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                               autopopulated HAPI.
     * @return The questionnaire form generated based on the StructureDefinition.
     */
    @Operation(name = ProviderConstants.CR_OPERATION_QUESTIONNAIRE, idempotent = true, type = StructureDefinition.class)
    public Questionnaire questionnaire(
            @IdParam IdType id,
            @OperationParam(name = "profile") StructureDefinition profile,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "supportedOnly") BooleanType supportedOnly,
            @OperationParam(name = "requiredOnly") BooleanType requiredOnly,
            @OperationParam(name = "contentEndpoint") Parameters.ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails) {
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var contentEndpointParam = getEndpoint(fhirVersion, contentEndpoint);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        return (Questionnaire) questionnaireProcessorFactory
                .create(requestDetails)
                .generateQuestionnaire(
                        Eithers.for3(canonicalType, id, profile),
                        supportedOnly == null ? Boolean.FALSE : supportedOnly.booleanValue(),
                        requiredOnly == null ? Boolean.FALSE : requiredOnly.booleanValue(),
                        contentEndpointParam,
                        terminologyEndpointParam,
                        null);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_QUESTIONNAIRE, idempotent = true, type = StructureDefinition.class)
    public Questionnaire questionnaire(
            @OperationParam(name = "profile") StructureDefinition profile,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "supportedOnly") BooleanType supportedOnly,
            @OperationParam(name = "requiredOnly") BooleanType requiredOnly,
            @OperationParam(name = "contentEndpoint") Parameters.ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails) {
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var contentEndpointParam = getEndpoint(fhirVersion, contentEndpoint);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        return (Questionnaire) questionnaireProcessorFactory
                .create(requestDetails)
                .generateQuestionnaire(
                        Eithers.for3(canonicalType, null, profile),
                        supportedOnly == null ? Boolean.FALSE : supportedOnly.booleanValue(),
                        requiredOnly == null ? Boolean.FALSE : requiredOnly.booleanValue(),
                        contentEndpointParam,
                        terminologyEndpointParam,
                        null);
    }
}
