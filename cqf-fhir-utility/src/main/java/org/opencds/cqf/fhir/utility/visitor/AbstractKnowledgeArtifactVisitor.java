package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;

public abstract class AbstractKnowledgeArtifactVisitor implements IKnowledgeArtifactVisitor {
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
}
