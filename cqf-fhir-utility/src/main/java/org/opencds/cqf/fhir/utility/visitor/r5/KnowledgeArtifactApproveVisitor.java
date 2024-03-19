package org.opencds.cqf.fhir.utility.visitor.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.ArtifactAssessment;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.MarkdownType;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;

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
            artifactAssessment.setDate(new Date());
            var content = artifactAssessment.getContentFirstRep();
            if (artifactAssessmentSummary.isPresent()) {
                content.setSummaryElement(artifactAssessmentSummary.get());
            }
            if (artifactAssessmentAuthor.isPresent()) {
                content.setAuthor(artifactAssessmentAuthor.get());
            }
            if (StringUtils.isNotBlank(artifactAssessmentType)) {
                content.setInformationType(
                        ArtifactAssessment.ArtifactAssessmentInformationType.fromCode(artifactAssessmentType));
            }
            if (artifactAssessmentTargetCanonical.isPresent()) {
                content.addRelatedArtifact()
                        .setType(RelatedArtifactType.DERIVEDFROM)
                        .setResourceElement(artifactAssessmentTargetCanonical.get());
            }
            if (artifactAssessmentRelatedArtifact.isPresent()) {
                content.addRelatedArtifact()
                        .setType(RelatedArtifactType.CITATION)
                        .setResourceElement(artifactAssessmentRelatedArtifact.get());
            }
        } catch (FHIRException e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
        return artifactAssessment;
    }
}
