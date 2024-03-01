package org.opencds.cqf.fhir.utility.visitor.r4;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;
import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.r4.PackageHelper;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public class KnowledgeArtifactApproveVisitor implements KnowledgeArtifactVisitor {
    @Override
    public Bundle visit(LibraryAdapter library, Repository repository, IBaseParameters approveParameters) {
        Date currentDate = new Date();
        Date approvalDate = MetadataResourceHelper.getParameter("approvalDate", approveParameters, DateType.class)
                .map(d -> d.getValue())
                .orElse(currentDate);
        String artifactAssessmentType = MetadataResourceHelper.getParameter(
                        "artifactAssessmentType", approveParameters, CodeType.class)
                .map(s -> s.getValue())
                .orElse("comment");
        Optional<MarkdownType> artifactAssessmentSummary =
                MetadataResourceHelper.getParameter("artifactAssessmentSummary", approveParameters, MarkdownType.class);
        Optional<CanonicalType> artifactAssessmentTarget =
                MetadataResourceHelper.getParameter("artifactAssessmentTarget", approveParameters, CanonicalType.class);
        if (artifactAssessmentTarget.isPresent()) {
            if (!Canonicals.getUrl(artifactAssessmentTarget.get()).equals(library.getUrl())) {
                throw new UnprocessableEntityException(
                        "ArtifactCommentTarget URL does not match URL of resource being approved.");
            }
            if (library.hasVersion()) {
                if (!Canonicals.getVersion(artifactAssessmentTarget.get()).equals(library.getVersion())) {
                    throw new UnprocessableEntityException(
                            "ArtifactCommentTarget version does not match version of resource being approved.");
                }
            }
        }
        Optional<CanonicalType> artifactAssessmentRelatedArtifact = MetadataResourceHelper.getParameter(
                "artifactAssessmentRelatedArtifact", approveParameters, CanonicalType.class);
        Optional<Reference> artifactAssessmentAuthor =
                MetadataResourceHelper.getParameter("artifactAssessmentAuthor", approveParameters, Reference.class);
        Bundle returnBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        ArtifactAssessment assessment = createApprovalAssessment(
                library.getId(),
                artifactAssessmentType,
                artifactAssessmentSummary,
                artifactAssessmentTarget,
                artifactAssessmentRelatedArtifact,
                artifactAssessmentAuthor,
                new Reference(library.get().getIdElement()));
        library.setApprovalDate(approvalDate);
        DateTimeType theDate = new DateTimeType(currentDate, TemporalPrecisionEnum.DAY);
        library.setDateElement(theDate);
        returnBundle.addEntry(PackageHelper.createEntry(assessment, false));
        returnBundle.addEntry(PackageHelper.createEntry((Library) library.get(), true));
        return repository.transaction(returnBundle);

        // DependencyInfo --document here that there is a need for figuring out how to determine which package the
        // dependency is in.
        // what is dependency, where did it originate? potentially the package?
    }

    ArtifactAssessment createApprovalAssessment(
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

    @Override
    public IBase visit(KnowledgeArtifactAdapter library, Repository repository, IBaseParameters draftParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(
            PlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }
}
