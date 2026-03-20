package org.opencds.cqf.fhir.cr.hapi.r4.library;

import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class LibraryCreateChangelogProvider {

    private final ILibraryProcessorFactory libraryProcessorFactory;

    private final FhirVersionEnum fhirVersion;

    public LibraryCreateChangelogProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
        this.fhirVersion = FhirVersionEnum.R4;
    }

    @Operation(name = "$create-changelog", idempotent = true, global = true, type = Library.class)
    @Description(
            shortDefinition = "$create-changelog",
            value = "Create a changelog object which can be easily rendered into a table")
    public IBaseResource crmiArtifactDiff(
            RequestDetails requestDetails,
            @OperationParam(name = "source") String source,
            @OperationParam(name = "target") String target,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint)
            throws UnprocessableEntityException, ResourceNotFoundException {
        IIdType sourceId = getIdType(fhirVersion, "Library", source);
        IIdType targetId = getIdType(fhirVersion, "Library", target);

        return libraryProcessorFactory
                .create(requestDetails)
                .createChangelog(
                        Eithers.for3(null, sourceId, null), Eithers.for3(null, targetId, null), terminologyEndpoint);
    }
}
