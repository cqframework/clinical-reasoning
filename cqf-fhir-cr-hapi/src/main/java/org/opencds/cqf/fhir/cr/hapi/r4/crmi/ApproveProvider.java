package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_APPROVE;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.Date;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.r4.IApproveServiceFactory;

public class ApproveProvider {

    private final IApproveServiceFactory r4ApproveServiceFactory;

    public ApproveProvider(IApproveServiceFactory r4ApproveServiceFactory) {
        this.r4ApproveServiceFactory = r4ApproveServiceFactory;
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
     * @param requestDetails                    The {@link RequestDetails RequestDetails}
     * @return  The {@link Bundle} Bundle result containing all updated artifacts and the
     *          ArtifactAssessment (Basic in R4) resource containing the Approval metadata
     */
    @Operation(
            name = CRMI_OPERATION_APPROVE,
            idempotent = true,
            global = true,
            type = MetadataResource.class,
            canonicalUrl = "http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-approve")
    @Description(
            shortDefinition = CRMI_OPERATION_APPROVE,
            value = "Apply an approval to an existing artifact, regardless of status.")
    public Bundle approveOperation(
            @IdParam IdType id,
            @OperationParam(name = "approvalDate", typeName = "Date") IPrimitiveType<Date> approvalDate,
            @OperationParam(name = "artifactAssessmentType") StringType artifactAssessmentType,
            @OperationParam(name = "artifactAssessmentSummary") StringType artifactAssessmentSummary,
            @OperationParam(name = "artifactAssessmentTarget") CanonicalType artifactAssessmentTarget,
            @OperationParam(name = "artifactAssessmentRelatedArtifact") CanonicalType artifactAssessmentRelatedArtifact,
            @OperationParam(name = "artifactAssessmentAuthor") Reference artifactAssessmentAuthor,
            RequestDetails requestDetails) {
        return r4ApproveServiceFactory
                .create(requestDetails)
                .approve(
                        id,
                        approvalDate,
                        getStringValue(artifactAssessmentType),
                        getStringValue(artifactAssessmentSummary),
                        artifactAssessmentTarget,
                        artifactAssessmentRelatedArtifact,
                        artifactAssessmentAuthor);
    }
}
