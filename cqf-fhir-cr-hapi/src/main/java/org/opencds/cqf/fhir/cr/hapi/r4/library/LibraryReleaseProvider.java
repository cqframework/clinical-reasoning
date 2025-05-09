package org.opencds.cqf.fhir.cr.hapi.r4.library;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Objects;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.hapi.common.IRepositoryFactory;
import org.opencds.cqf.fhir.cr.visitor.ReleaseVisitor;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class LibraryReleaseProvider {
    public static final String vsmCondition = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-condition";

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
     * @param id              the {@link IdType IdType}, always an argument for instance level operations
     * @param version            new version in the form MAJOR.MINOR.PATCH
     * @param versionBehavior    how to handle differences between the user-provided and incumbernt versions
     * @param latestFromTxServer whether or not to query the TxServer if version information is missing from references
     * @return A transaction bundle result of the updated resources
     */
    @Operation(name = "$release", idempotent = true, global = true, type = Library.class)
    @Description(shortDefinition = "$release", value = "Release an existing draft artifact")
    public Bundle releaseOperation(
            @IdParam IdType id,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer", typeName = "Boolean")
                    IPrimitiveType<Boolean> latestFromTxServer,
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
            adapter.getRelatedArtifact().forEach(ra -> {
                checkIfValueSetNeedsCondition(null, (RelatedArtifact) ra, repository);
            });
            return (Bundle) adapter.accept(visitor, params);
        } catch (Exception e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
    }

    public void checkIfValueSetNeedsCondition(
            MetadataResource resource, RelatedArtifact relatedArtifact, Repository hapiFhirRepository)
            throws UnprocessableEntityException {
        if (resource == null
                && relatedArtifact != null
                && relatedArtifact.getResource() != null
                && Objects.equals(Canonicals.getResourceType(relatedArtifact.getResource()), "ValueSet")) {
            var searchResults = BundleHelper.getEntryResources(SearchHelper.searchRepositoryByCanonicalWithPaging(
                    hapiFhirRepository, relatedArtifact.getResource()));
            if (!searchResults.isEmpty()) {
                resource = (MetadataResource) searchResults.get(0);
            }
        }
        if (resource != null && resource.getResourceType() == ResourceType.ValueSet) {
            var valueSet = (ValueSet) resource;
            var isLeaf = !isGrouper(valueSet);
            var maybeConditionExtension = Optional.ofNullable(relatedArtifact)
                    .map(RelatedArtifact::getExtension)
                    .flatMap(list -> list.stream()
                            .map(e -> (Extension) e)
                            .filter(ext -> ext.getUrl().equalsIgnoreCase(vsmCondition))
                            .findFirst());
            if (isLeaf && maybeConditionExtension.isEmpty()) {
                throw new UnprocessableEntityException("Missing condition on ValueSet : " + valueSet.getUrl());
            }
        }
    }

    public boolean isGrouper(MetadataResource resource) {
        return resource.getResourceType() == ResourceType.ValueSet
                && ((ValueSet) resource).hasCompose()
                && !((ValueSet) resource)
                        .getCompose()
                        .getIncludeFirstRep()
                        .getValueSet()
                        .isEmpty();
    }
}
