package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;

public class KnowledgeArtifactApproveVisitor implements KnowledgeArtifactVisitor {
    @Override
    public IBaseResource visit(LibraryAdapter library, Repository repository, IBaseParameters approveParameters) {
        Date currentDate = new Date();
        var fhirVersion = library.get().getStructureFhirVersionEnum();
        Date approvalDate = VisitorHelper.getParameter("approvalDate", approveParameters, IPrimitiveType.class)
                .map(d -> (Date) d.getValue())
                .orElse(currentDate);
        String artifactAssessmentType = VisitorHelper.getParameter(
                        "artifactAssessmentType", approveParameters, IPrimitiveType.class)
                .map(s -> (String) s.getValue())
                .orElse("comment");
        Optional<String> artifactAssessmentSummary = VisitorHelper.getParameter(
                        "artifactAssessmentSummary", approveParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue());
        Optional<String> artifactAssessmentTarget = VisitorHelper.getParameter(
                        "artifactAssessmentTarget", approveParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue());
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
        Optional<String> artifactAssessmentRelatedArtifact = VisitorHelper.getParameter(
                        "artifactAssessmentRelatedArtifact", approveParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue());
        Optional<IBaseReference> artifactAssessmentAuthor =
                VisitorHelper.getParameter("artifactAssessmentAuthor", approveParameters, IBaseReference.class);
        var returnBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");

        var assessment = createApprovalAssessment(
                library.getId(),
                artifactAssessmentType,
                artifactAssessmentSummary,
                artifactAssessmentTarget,
                artifactAssessmentRelatedArtifact,
                artifactAssessmentAuthor,
                library.get().getIdElement(),
                fhirVersion);
        library.setApprovalDate(approvalDate);
        setDateElement(library, currentDate, fhirVersion);
        BundleHelper.addEntry(returnBundle, PackageHelper.createEntry(assessment, false));
        BundleHelper.addEntry(returnBundle, PackageHelper.createEntry(library.get(), true));
        return repository.transaction(returnBundle);
    }

    private IBaseResource createApprovalAssessment(
            IIdType id,
            String artifactAssessmentType,
            Optional<String> artifactAssessmentSummary,
            Optional<String> artifactAssessmentTargetCanonical,
            Optional<String> artifactAssessmentRelatedArtifact,
            Optional<IBaseReference> artifactAssessmentAuthor,
            IIdType artifactTargetReference,
            FhirVersionEnum fhirVersion)
            throws UnprocessableEntityException {
        switch (fhirVersion) {
            case DSTU3:
                return org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactApproveVisitor
                        .createApprovalAssessment(
                                id,
                                artifactAssessmentType,
                                artifactAssessmentSummary.map(t -> new org.hl7.fhir.dstu3.model.MarkdownType(t)),
                                artifactAssessmentTargetCanonical.map(t -> new org.hl7.fhir.dstu3.model.UriType(t)),
                                artifactAssessmentRelatedArtifact.map(t -> new org.hl7.fhir.dstu3.model.UriType(t)),
                                artifactAssessmentAuthor.map(t -> (org.hl7.fhir.dstu3.model.Reference) t),
                                new org.hl7.fhir.dstu3.model.Reference(artifactTargetReference));
            case R4:
                return org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactApproveVisitor.createApprovalAssessment(
                        id,
                        artifactAssessmentType,
                        artifactAssessmentSummary.map(t -> new org.hl7.fhir.r4.model.MarkdownType(t)),
                        artifactAssessmentTargetCanonical.map(t -> new org.hl7.fhir.r4.model.CanonicalType(t)),
                        artifactAssessmentRelatedArtifact.map(t -> new org.hl7.fhir.r4.model.CanonicalType(t)),
                        artifactAssessmentAuthor.map(t -> (org.hl7.fhir.r4.model.Reference) t),
                        new org.hl7.fhir.r4.model.Reference(artifactTargetReference));
            case R5:
                return org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactApproveVisitor.createApprovalAssessment(
                        id,
                        artifactAssessmentType,
                        artifactAssessmentSummary.map(t -> new org.hl7.fhir.r5.model.MarkdownType(t)),
                        artifactAssessmentTargetCanonical.map(t -> new org.hl7.fhir.r5.model.CanonicalType(t)),
                        artifactAssessmentRelatedArtifact.map(t -> new org.hl7.fhir.r5.model.CanonicalType(t)),
                        artifactAssessmentAuthor.map(t -> (org.hl7.fhir.r5.model.Reference) t),
                        new org.hl7.fhir.r5.model.Reference(artifactTargetReference));
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    private void setDateElement(LibraryAdapter library, Date currentDate, FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                library.setDateElement(
                        new org.hl7.fhir.dstu3.model.DateTimeType(currentDate, TemporalPrecisionEnum.DAY));
                break;
            case R4:
                library.setDateElement(new org.hl7.fhir.r4.model.DateTimeType(currentDate, TemporalPrecisionEnum.DAY));
                break;
            case R5:
                library.setDateElement(new org.hl7.fhir.r5.model.DateTimeType(currentDate, TemporalPrecisionEnum.DAY));
                break;
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
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
