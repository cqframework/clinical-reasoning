package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class DiagnosticReportResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public DiagnosticReportResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public DiagnosticReport resolve(
            IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        var diagnosticReport = new DiagnosticReport();

        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
        diagnosticReport.setSubject(new Reference(subjectId));

        if (activityDefinition.hasCode()) {
            diagnosticReport.setCode(activityDefinition.getCode());
        } else {
            throw new FHIRException("Missing required ActivityDefinition.code property for DiagnosticReport");
        }

        if (activityDefinition.hasRelatedArtifact()) {
            List<Attachment> presentedFormAttachments = new ArrayList<>();
            for (var artifact : activityDefinition.getRelatedArtifact()) {
                var attachment = new Attachment();

                if (artifact.hasUrl()) {
                    attachment.setUrl(artifact.getUrl());
                }

                if (artifact.hasDisplay()) {
                    attachment.setTitle(artifact.getDisplay());
                }
                presentedFormAttachments.add(attachment);
            }
            diagnosticReport.setPresentedForm(presentedFormAttachments);
        }

        return diagnosticReport;
    }
}
