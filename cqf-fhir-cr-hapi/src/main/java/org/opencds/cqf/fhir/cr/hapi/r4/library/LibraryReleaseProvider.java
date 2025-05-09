package org.opencds.cqf.fhir.cr.hapi.r4.library;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.hapi.common.IRepositoryFactory;
import org.opencds.cqf.fhir.cr.visitor.ReleaseVisitor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class LibraryReleaseProvider {
    private final IRepositoryFactory repositoryFactory;
    private final IAdapterFactory adapterFactory;

    public LibraryReleaseProvider(IRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
        adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    }

    /**
     * Sets the status of an existing artifact to Active if it has status Draft.
     *
     * @param requestDetails     the {@link RequestDetails RequestDetails}
     * @param id                 the {@link IdType IdType}, always an argument for instance level operations
     * @param version            new version in the form MAJOR.MINOR.PATCH
     * @param versionBehavior    how to handle differences between the user-provided and incumbent versions
     * @param latestFromTxServer whether to query the TxServer if version information is missing from references
     * @return A transaction bundle result of the updated resources
     */
    @Operation(name = "$release", idempotent = true, global = true, type = Library.class)
    @Description(shortDefinition = "$release", value = "Release an existing draft artifact")
    public Bundle releaseOperation(
            @IdParam IdType id,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer") BooleanType latestFromTxServer,
            @OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            @OperationParam(name = "releaseLabel") String releaseLabel,
            RequestDetails requestDetails)
            throws FHIRException {
        var repository = repositoryFactory.create(requestDetails);
        var library = (Library) SearchHelper.readRepository(repository, id);
        if (library == null) {
            throw new ResourceNotFoundException(id);
        }
        var params = new Parameters();
        if (version != null) {
            params.addParameter("version", version);
        }
        if (versionBehavior != null) {
            params.addParameter("versionBehavior", versionBehavior);
        }
        if (latestFromTxServer != null && latestFromTxServer.hasValue()) {
            params.addParameter("latestFromTxServer", latestFromTxServer.getValue());
        }
        if (requireNonExperimental != null) {
            params.addParameter("requireNonExperimental", requireNonExperimental);
        }
        if (releaseLabel != null) {
            params.addParameter("releaseLabel", releaseLabel);
        }
        if (terminologyEndpoint != null) {
            params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        }
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
        try {
            var visitor = new ReleaseVisitor(repository);
            return (Bundle) adapter.accept(visitor, params);
        } catch (Exception e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
    }
}
