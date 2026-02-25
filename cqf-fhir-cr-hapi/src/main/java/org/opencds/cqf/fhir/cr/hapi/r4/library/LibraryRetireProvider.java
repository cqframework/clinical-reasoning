package org.opencds.cqf.fhir.cr.hapi.r4.library;

import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_RETIRE;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class LibraryRetireProvider {

    private final ILibraryProcessorFactory libraryProcessorFactory;

    private final FhirVersionEnum fhirVersion;

    public LibraryRetireProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
        this.fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Retires an existing artifact if it has status Draft.
     *
     * @param id the {@link IdType IdType}, always an argument for instance level operations
     * @return A transaction {@link Bundle Bundle} result of the retired resources
     */
    @Operation(name = CRMI_OPERATION_RETIRE, idempotent = true, global = true, type = Library.class)
    @Description(shortDefinition = CRMI_OPERATION_RETIRE, value = "Retire an existing active artifact")
    public IBaseBundle withdrawOperation(@IdParam IdType id, RequestDetails requestDetails) throws FHIRException {
        return libraryProcessorFactory.create(requestDetails).retireLibrary(Eithers.forMiddle3(id), new Parameters());
    }
}
