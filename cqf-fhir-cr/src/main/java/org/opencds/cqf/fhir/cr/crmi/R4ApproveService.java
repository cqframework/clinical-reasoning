package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.visitor.ApproveVisitor;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class R4ApproveService {

    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);

    private final IRepository repository;

    public R4ApproveService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * The "approve" operation supports applying an approval to an existing artifact, all its
     * children, regardless of status. The operation sets the date and approvalDate elements
     * of the approved artifact and child artifacts, and is otherwise only allowed to create
     * ArtifactAssessment (Basic or cqf-artifactComment extensions in R4) resources in the
     * repository.
     * The "approve" operation supports the ability of a repository to record commentary on a
     * specific state of an artifact in an ArtifactAssessment (Basic or cqf-artifactComment
     * extension in R4) resource by applying an approval. The artifact assessments which are added
     * by the operation must reference a version of the artifact.
     *
     * @param id                                The logical id of the artifact to approved. The
     *                                          server must know the artifact (e.g. it is defined
     *                                          explicitly in the server's artifacts)
     * @param approvalDate                      The date on which the artifact was approved. If one
     *                                          is not provided the system date will be used.
     * @param artifactAssessmentType            If a comment is submitted as part of the approval,
     *                                          this parameter denotes the type of artifact comment.
     * @param artifactAssessmentSummary         The body of the comment.
     * @param artifactAssessmentTarget          The canonical {@link CanonicalType} url for the
     *                                          artifact being approved. The format is:
     *                                          [system]|[version] - e.g. http://loinc.org|2.56
     * @param artifactAssessmentRelatedArtifact Optional supporting canonical {@link CanonicalType}
     *                                          URL / Reference for the comment.
     * @param artifactAssessmentAuthor          A {@link Reference} Reference to a resource
     *                                          containing information about the entity applying
     *                                          the approval.
     * @return  The {@link Bundle} Bundle result containing all updated artifacts and the
     *          ArtifactAssessment (Basic in R4) resource containing the Approval metadata
     */
    public Bundle approve(
            IdType id,
            IPrimitiveType<Date> approvalDate,
            String artifactAssessmentType,
            String artifactAssessmentSummary,
            CanonicalType artifactAssessmentTarget,
            CanonicalType artifactAssessmentRelatedArtifact,
            Reference artifactAssessmentAuthor) {
        var resource = (MetadataResource) SearchHelper.readRepository(repository, id);
        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }
        if (artifactAssessmentTarget != null) {
            if (Canonicals.getUrl(artifactAssessmentTarget) != null
                    && !Canonicals.getUrl(artifactAssessmentTarget).equals(resource.getUrl())) {
                throw new UnprocessableEntityException(
                        "ArtifactAssessmentTarget URL does not match URL of resource being approved.");
            }
            if (Canonicals.getVersion(artifactAssessmentTarget) != null
                    && !Canonicals.getVersion(artifactAssessmentTarget).equals(resource.getVersion())) {
                throw new UnprocessableEntityException(
                        "ArtifactAssessmentTarget version does not match version of resource being approved.");
            }
        } else {
            String target = "";
            String url = resource.getUrl();
            String version = resource.getVersion();
            if (url != null) {
                target += url;
            }
            if (version != null) {
                if (url != null) {
                    target += "|";
                }
                target += version;
            }
            if (!target.isEmpty()) {
                artifactAssessmentTarget = new CanonicalType(target);
            }
        }
        var params = new Parameters();
        if (approvalDate != null && approvalDate.hasValue()) {
            params.addParameter("approvalDate", new DateType(approvalDate.getValue()));
        }
        if (artifactAssessmentType != null) {
            params.addParameter("artifactAssessmentType", artifactAssessmentType);
        }
        if (artifactAssessmentTarget != null) {
            params.addParameter("artifactAssessmentTarget", artifactAssessmentTarget);
        }
        if (artifactAssessmentSummary != null) {
            params.addParameter("artifactAssessmentSummary", artifactAssessmentSummary);
        }
        if (artifactAssessmentRelatedArtifact != null) {
            params.addParameter("artifactAssessmentRelatedArtifact", artifactAssessmentRelatedArtifact);
        }
        if (artifactAssessmentAuthor != null) {
            params.addParameter("artifactAssessmentAuthor", artifactAssessmentAuthor);
        }
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
        var visitor = new ApproveVisitor(repository);
        return ((Bundle) adapter.accept(visitor, params));
    }
}
