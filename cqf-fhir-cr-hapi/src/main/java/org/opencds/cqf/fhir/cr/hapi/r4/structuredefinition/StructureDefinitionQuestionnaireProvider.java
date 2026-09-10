package org.opencds.cqf.fhir.cr.hapi.r4.structuredefinition;

import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;

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
     * @param resource 			 The StructureDefinition to base the Questionnaire on. Used when the operation is invoked at the 'type' level.
     * @param url             	 Canonical URL's of the StructureDefinitions when invoked at the resource type level.
     * @param supportedOnly       If true (default: false), the questionnaire will only include those elements marked as "mustSupport='true'" in the StructureDefinition.
     * @param minimalOnly        If true (default: false), the questionnaire will only include those elements marked as "min>0" in the StructureDefinition.
     * @param contentEndpoint     The FHIR {@link Endpoint} Endpoint resource or url to use to access content (i.e. libraries) referenced by the StructureDefinition.
     * @param terminologyEndpoint The FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, and membership testing)
     *                               referenced by the StructureDefinition.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                               autopopulated HAPI.
     * @return The questionnaire form generated based on the StructureDefinition.
     */
    @Operation(name = ProviderConstants.CR_OPERATION_QUESTIONNAIRE, idempotent = true, type = StructureDefinition.class)
    public Questionnaire questionnaire(
            @OperationParam(name = "resource") StructureDefinition resource,
            @OperationParam(name = "url", typeName = "uri") List<IPrimitiveType<String>> url,
            @OperationParam(name = "supportedOnly") BooleanType supportedOnly,
            @OperationParam(name = "minimalOnly") BooleanType minimalOnly,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails) {
        return (Questionnaire) questionnaireProcessorFactory
                .create(requestDetails)
                .generateQuestionnaire(
                        resource,
                        url,
                        supportedOnly == null ? Boolean.FALSE : supportedOnly.booleanValue(),
                        minimalOnly == null ? Boolean.FALSE : minimalOnly.booleanValue(),
                        getEndpoint(fhirVersion, contentEndpoint),
                        getEndpoint(fhirVersion, terminologyEndpoint),
                        null);
    }
}
