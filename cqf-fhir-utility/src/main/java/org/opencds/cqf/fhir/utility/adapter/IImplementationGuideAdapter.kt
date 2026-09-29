package org.opencds.cqf.fhir.utility.adapter;

import java.util.Date;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

/**
 * This interface exposes common functionality across all FHIR Implementation
 * Guide versions.
 */
public interface IImplementationGuideAdapter extends IKnowledgeArtifactAdapter {

    @Override
    default Date getApprovalDate() {
        var approvalDateExt = getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/artifact-approvalDate");
        if (approvalDateExt == null) {
            return null;
        } else if (approvalDateExt.getValue() == null
                || approvalDateExt.getValue().isEmpty()) {
            return null;
        } else if (approvalDateExt.getValue() instanceof IPrimitiveType<?>
                && ((IPrimitiveType<?>) approvalDateExt.getValue()).getValue() instanceof Date) {
            return (Date) ((IPrimitiveType<?>) approvalDateExt.getValue()).getValue();
        }
        return null;
    }
}
