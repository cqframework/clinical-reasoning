package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;

public interface IAdapterFactory {

    static IAdapterFactory forFhirContext(FhirContext fhirContext) {
        return forFhirVersion(fhirContext.getVersion().getVersion());
    }

    static IAdapterFactory forFhirVersion(FhirVersionEnum fhirVersion) {
        return switch (fhirVersion) {
            case DSTU3 -> new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
            case R4 -> new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
            case R5 -> new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
            default -> throw new IllegalArgumentException(
                    String.format("Unsupported FHIR version: %s", fhirVersion.toString()));
        };
    }

    /**
     * Creates an adapter that exposes common resource operations across multiple versions of FHIR
     *
     * @param resource A FHIR Resource
     * @return an adapter exposing common api calls
     */
    static IResourceAdapter createAdapterForResource(IBaseResource resource) {
        return forFhirVersion(resource.getStructureFhirVersionEnum()).createResource(resource);
    }

    /**
     * Creates an adapter that exposes common resource operations across multiple versions of FHIR
     *
     * @param resource A FHIR Resource
     * @return an adapter exposing common api calls
     */
    IResourceAdapter createResource(IBaseResource resource);

    /**
     * Creates an adapter that exposes common MetadataResource operations across multiple versions of FHIR
     *
     * @param metadataResource A FHIR MetadataResource
     * @return an adapter exposing common api calls
     */
    IKnowledgeArtifactAdapter createKnowledgeArtifactAdapter(IDomainResource metadataResource);

    /**
     * Creates an adapter that exposes common Library operations across multiple versions of FHIR
     *
     * @param library a FHIR Library Resource
     * @return an adapter exposing common api calls
     */
    ILibraryAdapter createLibrary(IBaseResource library);

    /**
     * Creates an adapter that exposes common Library operations across multiple versions of FHIR
     *
     * @param planDefinition a FHIR PlanDefinition Resource
     * @return an adapter exposing common api calls
     */
    IPlanDefinitionAdapter createPlanDefinition(IBaseResource planDefinition);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param attachment a FHIR Attachment Structure
     * @return an adapter exposing common api calls
     */
    IAttachmentAdapter createAttachment(ICompositeType attachment);

    /**
     * Creates an adapter that exposes common Parameters operations across multiple versions of FHIR
     *
     * @param parameters a FHIR Parameters Resource
     * @return an adapter exposing common api calls
     */
    IParametersAdapter createParameters(IBaseParameters parameters);

    /**
     * Creates an adapter that exposes common ParametersParameterComponent operations across multiple
     * versions of FHIR
     *
     * @param parametersParameterComponent a FHIR ParametersParameterComponent Structure
     * @return an adapter exposing common api calls
     */
    IParametersParameterComponentAdapter createParametersParameter(IBaseBackboneElement parametersParameterComponent);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param endpoint a FHIR Endpoint Resource
     * @return an adapter exposing common api calls
     */
    IEndpointAdapter createEndpoint(IBaseResource endpoint);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param codeableConcept a FHIR CodeableConcept object
     * @return an adapter exposing common api calls
     */
    ICodeableConceptAdapter createCodeableConcept(ICompositeType codeableConcept);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param coding a FHIR Coding object
     * @return an adapter exposing common api calls
     */
    ICodingAdapter createCoding(ICompositeType coding);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param element a FHIR ElementDefinition object
     * @return an adapter exposing common api calls
     */
    IElementDefinitionAdapter createElementDefinition(ICompositeType element);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param action a FHIR RequestOrchestrationActionComponent object
     * @return an adapter exposing common api calls
     */
    IRequestActionAdapter createRequestAction(IBaseBackboneElement action);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param dataRequirement a FHIR DataRequirement object
     * @return an adapter exposing common api calls
     */
    IDataRequirementAdapter createDataRequirement(ICompositeType dataRequirement);
}
