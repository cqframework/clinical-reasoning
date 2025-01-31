package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;

public interface IAdapterFactory {

    public static IAdapterFactory forFhirContext(FhirContext fhirContext) {
        return forFhirVersion(fhirContext.getVersion().getVersion());
    }

    public static IAdapterFactory forFhirVersion(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
            case R4:
                return new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
            case R5:
                return new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported FHIR version: %s", fhirVersion.toString()));
        }
    }

    public static IResourceAdapter createAdapterForResource(IBaseResource resource) {
        return forFhirVersion(resource.getStructureFhirVersionEnum()).createResource(resource);
    }

    /**
     * Creates an adapter that exposes common resource operations across multiple versions of FHIR
     *
     * @param resource A FHIR Resource
     * @return an adapter exposing common api calls
     */
    public IResourceAdapter createResource(IBaseResource resource);

    /**
     * Creates an adapter that exposes common MetadataResource operations across multiple versions of FHIR
     *
     * @param metadataResource A FHIR MetadataResource
     * @return an adapter exposing common api calls
     */
    public IKnowledgeArtifactAdapter createKnowledgeArtifactAdapter(IDomainResource metadataResource);

    /**
     * Creates an adapter that exposes common Library operations across multiple versions of FHIR
     *
     * @param library a FHIR Library Resource
     * @return an adapter exposing common api calls
     */
    public ILibraryAdapter createLibrary(IBaseResource library);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param attachment a FHIR Attachment Structure
     * @return an adapter exposing common api calls
     */
    public IAttachmentAdapter createAttachment(ICompositeType attachment);

    /**
     * Creates an adapter that exposes common Parameters operations across multiple versions of FHIR
     *
     * @param parameters a FHIR Parameters Resource
     * @return an adapter exposing common api calls
     */
    public IParametersAdapter createParameters(IBaseParameters parameters);

    /**
     * Creates an adapter that exposes common ParametersParameterComponent operations across multiple
     * versions of FHIR
     *
     * @param parametersParametersComponent a FHIR ParametersParameterComponent Structure
     * @return an adapter exposing common api calls
     */
    public IParametersParameterComponentAdapter createParametersParameters(
            IBaseBackboneElement parametersParametersComponent);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param endpoint a FHIR Endpoint Resource
     * @return an adapter exposing common api calls
     */
    public IEndpointAdapter createEndpoint(IBaseResource endpoint);

    public ICodeableConceptAdapter createCodeableConcept(ICompositeType codeableConcept);

    public ICodingAdapter createCoding(ICompositeType coding);

    public IElementDefinitionAdapter createElementDefinition(ICompositeType element);
}
