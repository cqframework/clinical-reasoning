package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

public class ApproveVisitor extends BaseKnowledgeArtifactVisitor {
    public ApproveVisitor(Repository repository) {
        super(repository);
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters approveParameters) {
        Date currentDate = new Date();
        Date approvalDate = VisitorHelper.getDateParameter("approvalDate", approveParameters)
                .orElse(currentDate);
        String artifactAssessmentType = VisitorHelper.getStringParameter("artifactAssessmentType", approveParameters)
                .orElse("comment");
        Optional<String> artifactAssessmentSummary =
                VisitorHelper.getStringParameter("artifactAssessmentSummary", approveParameters);
        Optional<String> artifactAssessmentTarget =
                VisitorHelper.getStringParameter("artifactAssessmentTarget", approveParameters);
        if (artifactAssessmentTarget.isPresent()) {
            if (!Canonicals.getUrl(artifactAssessmentTarget.get()).equals(adapter.getUrl())) {
                throw new UnprocessableEntityException(
                        "ArtifactCommentTarget URL does not match URL of resource being approved.");
            }
            if (adapter.hasVersion()
                    && !Canonicals.getVersion(artifactAssessmentTarget.get()).equals(adapter.getVersion())) {
                throw new UnprocessableEntityException(
                        "ArtifactCommentTarget version does not match version of resource being approved.");
            }
        }
        Optional<String> artifactAssessmentRelatedArtifact =
                VisitorHelper.getStringParameter("artifactAssessmentRelatedArtifact", approveParameters);
        Optional<IBaseReference> artifactAssessmentAuthor =
                VisitorHelper.getParameter("artifactAssessmentAuthor", approveParameters);
        var returnBundle = BundleHelper.newBundle(fhirVersion(), null, "transaction");

        var assessment = createApprovalAssessment(
                adapter.getId(),
                artifactAssessmentType,
                artifactAssessmentSummary,
                artifactAssessmentTarget,
                artifactAssessmentRelatedArtifact,
                artifactAssessmentAuthor,
                adapter.get().getIdElement());
        adapter.setApprovalDate(approvalDate);
        setDateElement(adapter, currentDate, fhirVersion());
        BundleHelper.addEntry(returnBundle, PackageHelper.createEntry(assessment, false));
        BundleHelper.addEntry(returnBundle, PackageHelper.createEntry(adapter.get(), true));
        return repository.transaction(returnBundle);
    }

    private IBaseResource createApprovalAssessment(
            IIdType id,
            String artifactAssessmentType,
            Optional<String> artifactAssessmentSummary,
            Optional<String> artifactAssessmentTargetCanonical,
            Optional<String> artifactAssessmentRelatedArtifact,
            Optional<IBaseReference> artifactAssessmentAuthor,
            IIdType artifactTargetReference)
            throws UnprocessableEntityException {
        // check for existing matching comment?
        try {
            switch (fhirVersion()) {
                case DSTU3:
                    return new org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment(
                                    new org.hl7.fhir.dstu3.model.Reference(id))
                            .createArtifactComment(
                                    new org.hl7.fhir.dstu3.model.CodeType(artifactAssessmentType),
                                    new org.hl7.fhir.dstu3.model.Reference(artifactTargetReference),
                                    artifactAssessmentTargetCanonical.map(org.hl7.fhir.dstu3.model.UriType::new),
                                    artifactAssessmentSummary.map(org.hl7.fhir.dstu3.model.MarkdownType::new),
                                    artifactAssessmentRelatedArtifact.map(org.hl7.fhir.dstu3.model.UriType::new),
                                    artifactAssessmentAuthor.map(t -> (org.hl7.fhir.dstu3.model.Reference) t));
                case R4:
                    return new org.opencds.cqf.fhir.utility.r4.ArtifactAssessment(
                                    new org.hl7.fhir.r4.model.Reference(id))
                            .createArtifactComment(
                                    new org.hl7.fhir.r4.model.CodeType(artifactAssessmentType),
                                    new org.hl7.fhir.r4.model.Reference(artifactTargetReference),
                                    artifactAssessmentTargetCanonical.map(org.hl7.fhir.r4.model.CanonicalType::new),
                                    artifactAssessmentSummary.map(org.hl7.fhir.r4.model.MarkdownType::new),
                                    artifactAssessmentRelatedArtifact.map(org.hl7.fhir.r4.model.CanonicalType::new),
                                    artifactAssessmentAuthor.map(t -> (org.hl7.fhir.r4.model.Reference) t));
                case R5:
                    var r5ArtifactAssessment = new org.hl7.fhir.r5.model.ArtifactAssessment(
                                    new org.hl7.fhir.r5.model.Reference(id))
                            .setDate(new Date());
                    var content = r5ArtifactAssessment
                            .getContentFirstRep()
                            .setSummaryElement(artifactAssessmentSummary
                                    .map(org.hl7.fhir.r5.model.MarkdownType::new)
                                    .orElse(null))
                            .setAuthor(artifactAssessmentAuthor
                                    .map(t -> (org.hl7.fhir.r5.model.Reference) t)
                                    .orElse(null))
                            .setInformationType(
                                    org.hl7.fhir.r5.model.ArtifactAssessment.ArtifactAssessmentInformationType.fromCode(
                                            artifactAssessmentType));
                    if (artifactAssessmentTargetCanonical.isPresent()) {
                        content.addRelatedArtifact()
                                .setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DERIVEDFROM)
                                .setResourceElement(artifactAssessmentTargetCanonical
                                        .map(org.hl7.fhir.r5.model.CanonicalType::new)
                                        .get());
                    }
                    if (artifactAssessmentRelatedArtifact.isPresent()) {
                        content.addRelatedArtifact()
                                .setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.CITATION)
                                .setResourceElement(artifactAssessmentRelatedArtifact
                                        .map(org.hl7.fhir.r5.model.CanonicalType::new)
                                        .get());
                    }
                    return r5ArtifactAssessment;

                default:
                    throw new UnprocessableEntityException("Unsupported version of FHIR: %s"
                            .formatted(fhirVersion().getFhirVersionString()));
            }
        } catch (FHIRException e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
    }

    private void setDateElement(IKnowledgeArtifactAdapter adapter, Date currentDate, FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                adapter.setDateElement(
                        new org.hl7.fhir.dstu3.model.DateTimeType(currentDate, TemporalPrecisionEnum.DAY));
                break;
            case R4:
                adapter.setDateElement(new org.hl7.fhir.r4.model.DateTimeType(currentDate, TemporalPrecisionEnum.DAY));
                break;
            case R5:
                adapter.setDateElement(new org.hl7.fhir.r5.model.DateTimeType(currentDate, TemporalPrecisionEnum.DAY));
                break;

            default:
                throw new UnprocessableEntityException(
                        "Unsupported version of FHIR: %s".formatted(fhirVersion.getFhirVersionString()));
        }
    }
}
