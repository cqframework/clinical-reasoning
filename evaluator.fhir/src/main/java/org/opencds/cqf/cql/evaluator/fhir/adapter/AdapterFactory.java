package org.opencds.cqf.cql.evaluator.fhir.adapter;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;

public interface AdapterFactory {

    /**
     * Creates an adapter that exposes common resource operations across multiple
     * versions of FHIR
     * 
     * @param resource A FHIR Resource
     * @return an adapter exposing common api calls
     */
    public ResourceAdapter createResource(IBaseResource resource);

    /**
     * Creates an adapter that exposes common Library operations across multiple
     * versions of FHIR
     * 
     * @param library a FHIR Library Resource
     * @return an adapter exposing common api calls
     */
    public LibraryAdapter createLibrary(IBaseResource library);

    /**
     * Creates an adapter that exposes common Attachment operations across multiple
     * versions of FHIR
     * 
     * @param attachment a FHIR Attachment Structure
     * @return an adapter exposing common api calls
     */
    public AttachmentAdapter createAttachment(ICompositeType attachment);

    /**
     * Creates an adapter that exposes common Parameters operations across multiple
     * versions of FHIR
     * 
     * @param parameters a FHIR Parameters Resource
     * @return an adapter exposing common api calls
     */
    public ParametersAdapter createParameters(IBaseParameters parameters);

    /**
     * Creates an adapter that exposes common ParametersParameterComponent
     * operations across multiple versions of FHIR
     * 
     * @param parametersParametersComponent a FHIR ParametersParameterComponent
     *                                      Structure
     * @return an adapter exposing common api calls
     */
    public ParametersParameterComponentAdapter createParametersParameters(
            IBaseBackboneElement parametersParametersComponent);
}