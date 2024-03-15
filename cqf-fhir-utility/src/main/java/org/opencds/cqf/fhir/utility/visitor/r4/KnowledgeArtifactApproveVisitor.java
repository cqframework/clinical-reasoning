package org.opencds.cqf.fhir.utility.visitor.r4;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;

public class KnowledgeArtifactApproveVisitor {

    public static ArtifactAssessment createApprovalAssessment(
            IIdType id,
            String artifactAssessmentType,
            Optional<MarkdownType> artifactAssessmentSummary,
            Optional<CanonicalType> artifactAssessmentTargetCanonical,
            Optional<CanonicalType> artifactAssessmentRelatedArtifact,
            Optional<Reference> artifactAssessmentAuthor,
            Reference artifactTargetReference)
            throws UnprocessableEntityException {
        // TODO: check for existing matching comment?
        ArtifactAssessment artifactAssessment;
        try {
            artifactAssessment = new ArtifactAssessment(new Reference(id));
            artifactAssessment.createArtifactComment(
                    ArtifactAssessmentContentInformationType.fromCode(artifactAssessmentType),
                    artifactTargetReference,
                    artifactAssessmentTargetCanonical,
                    artifactAssessmentSummary,
                    artifactAssessmentRelatedArtifact,
                    artifactAssessmentAuthor);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
        return artifactAssessment;
    }
}
