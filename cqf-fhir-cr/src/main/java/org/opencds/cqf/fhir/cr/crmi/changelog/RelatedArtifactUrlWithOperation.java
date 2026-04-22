package org.opencds.cqf.fhir.cr.crmi.changelog;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.fhir.cr.crmi.TransformProperties;

public class RelatedArtifactUrlWithOperation extends ValueAndOperation {

    private final RelatedArtifact fullRelatedArtifact;
    private List<CodeableConceptWithOperation> conditions = new ArrayList<>();
    private final CodeableConceptWithOperation priority = new CodeableConceptWithOperation(null);

    public RelatedArtifact getFullRelatedArtifact() {
        return fullRelatedArtifact;
    }

    public List<CodeableConceptWithOperation> getConditions() {
        return conditions;
    }

    public CodeableConceptWithOperation getPriority() {
        return priority;
    }

    public static class CodeableConceptWithOperation {

        private CodeableConcept value;
        private Operation operation;

        CodeableConceptWithOperation(CodeableConcept e) {
            this.value = e;
        }

        public CodeableConcept getValue() {
            return value;
        }

        public Operation getOperation() {
            return operation;
        }

        public void setOperation(Operation operation) {
            this.operation = operation;
        }
    }

    RelatedArtifactUrlWithOperation(RelatedArtifact relatedArtifact) {
        if (relatedArtifact != null) {
            this.setValue(relatedArtifact.getResource());
            this.conditions = relatedArtifact.getExtensionsByUrl(TransformProperties.vsmCondition).stream()
                    .map(e -> new CodeableConceptWithOperation((CodeableConcept) e.getValue()))
                    .toList();
            var priorities = relatedArtifact.getExtensionsByUrl(TransformProperties.vsmPriority).stream()
                    .map(e -> (CodeableConcept) e.getValue())
                    .toList();
            if (priorities.size() > 1) {
                throw new UnprocessableEntityException("too many priorities");
            } else if (priorities.size() == 1) {
                this.priority.value = priorities.get(0);
            } else {
                this.priority.value =
                        new CodeableConcept(new Coding(TransformProperties.usPHUsageContext, "routine", "Routine"));
            }
        }
        this.fullRelatedArtifact = relatedArtifact;
    }
}
