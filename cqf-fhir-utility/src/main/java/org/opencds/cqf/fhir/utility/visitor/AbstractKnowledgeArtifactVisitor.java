package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

public abstract class AbstractKnowledgeArtifactVisitor implements IKnowledgeArtifactVisitor {
    String isOwnedUrl = "http://hl7.org/fhir/StructureDefinition/artifact-isOwned";

    protected List<IBaseBackboneElement> findArtifactCommentsToUpdate(
            IBaseResource artifact, String releaseVersion, Repository repository) {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            return org.opencds.cqf.fhir.utility.visitor.dstu3.ReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.dstu3.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource) {
            return org.opencds.cqf.fhir.utility.visitor.r4.ReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r4.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource) {
            return org.opencds.cqf.fhir.utility.visitor.r5.ReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r5.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else {
            throw new UnprocessableEntityException("Version not supported");
        }
    }

    protected List<IDomainResource> getComponents(
            IKnowledgeArtifactAdapter adapter, Repository repository, ArrayList<IDomainResource> resourcesToUpdate) {
        adapter.getOwnedRelatedArtifacts().stream().forEach(c -> {
            final var preReleaseReference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(c);
            Optional<IKnowledgeArtifactAdapter> maybeArtifact =
                    VisitorHelper.tryGetLatestVersion(preReleaseReference, repository);
            if (maybeArtifact.isPresent()) {
                if (resourcesToUpdate.stream().noneMatch(rtu -> rtu.getId()
                        .equals(maybeArtifact.get().getId().toString()))) {
                    resourcesToUpdate.add(maybeArtifact.get().get());
                    getComponents(maybeArtifact.get(), repository, resourcesToUpdate);
                }
            } else {
                throw new ResourceNotFoundException("Unexpected resource not found when getting components");
            }
        });

        return resourcesToUpdate;
    }
}
