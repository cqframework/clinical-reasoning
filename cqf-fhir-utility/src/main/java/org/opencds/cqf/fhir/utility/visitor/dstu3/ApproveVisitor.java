package org.opencds.cqf.fhir.utility.visitor.dstu3;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.MarkdownType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment;

public class ApproveVisitor {

    public static ArtifactAssessment createApprovalAssessment(
            IIdType id,
            CodeType artifactAssessmentType,
            Optional<MarkdownType> artifactAssessmentSummary,
            Optional<UriType> artifactAssessmentTargetCanonical,
            Optional<UriType> artifactAssessmentRelatedArtifact,
            Optional<Reference> artifactAssessmentAuthor,
            Reference artifactTargetReference)
            throws UnprocessableEntityException {
        // TODO: check for existing matching comment?
        ArtifactAssessment artifactAssessment;
        try {
            artifactAssessment = new ArtifactAssessment(new Reference(id));
            artifactAssessment.createArtifactComment(
                    artifactAssessmentType,
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
