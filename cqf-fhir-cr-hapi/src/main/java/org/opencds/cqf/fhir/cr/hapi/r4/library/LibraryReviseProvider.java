package org.opencds.cqf.fhir.cr.hapi.r4.library;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;

public class LibraryReviseProvider {

    private final ILibraryProcessorFactory libraryProcessorFactory;

    private final FhirVersionEnum fhirVersion;

    public LibraryReviseProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
        this.fhirVersion = FhirVersionEnum.R4;
    }

    @Operation(name = "revise", idempotent = true, global = true, type = Library.class)
    @Description(shortDefinition = "$revise", value = "Update an existing artifact in 'draft' status")
    public IBaseResource reviseOperation(
            @OperationParam(name = "resource") IBaseResource resource, RequestDetails requestDetails)
            throws FHIRException {

        return libraryProcessorFactory.create(requestDetails).reviseLibrary(resource);
    }
}
