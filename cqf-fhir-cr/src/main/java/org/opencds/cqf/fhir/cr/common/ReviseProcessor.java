package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.fhir.utility.SearchHelper;

public class ReviseProcessor implements IReviseProcessor {

    private final IRepository repository;

    public ReviseProcessor(IRepository repository) {
        this.repository = repository;
    }

    @Override
    public IBaseResource reviseResource(IBaseResource resource) {
        var existingResource = (MetadataResource) SearchHelper.readRepository(repository, resource.getIdElement());
        if (existingResource == null) {
            throw new ResourceNotFoundException(resource.getIdElement());
        }

        if (!existingResource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
            throw new IllegalStateException(String.format("Current resource status is '%s'. Only resources with status of 'draft' can be revised.", existingResource.getStatus().toString()));
        }

        var proposedResource = (MetadataResource) resource;

        if (!proposedResource.getStatus().equals(Enumerations.PublicationStatus.DRAFT)) {
            throw new IllegalStateException(String.format("The resource status can not be updated from 'draft'. The proposed resource has status: %s", proposedResource.getStatus().toString()));
        }

        repository.update(proposedResource);

        return resource;
    }
}
