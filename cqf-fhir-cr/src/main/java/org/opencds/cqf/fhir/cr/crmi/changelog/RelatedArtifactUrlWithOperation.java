package org.opencds.cqf.fhir.cr.crmi.changelog;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UsageContext;
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
            this.conditions = retrieveValues(relatedArtifact, RelatedArtifactUrlWithOperation::conditionValue).stream()
                    .map(CodeableConceptWithOperation::new)
                    .toList();
            var priorities = retrieveValues(relatedArtifact, RelatedArtifactUrlWithOperation::priorityValue);
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

    private static List<CodeableConcept> retrieveValues(
            RelatedArtifact relatedArtifact, Function<Extension, Optional<CodeableConcept>> reader) {
        return relatedArtifact.getExtension().stream()
                .map(reader)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * The condition a manifest relatedArtifact extension states, if present
     */
    private static Optional<CodeableConcept> conditionValue(Extension extension) {
        return referenceValue(extension, TransformProperties.VSM_CONDITION_CODE);
    }

    /** The priority a manifest relatedArtifact extension states, if present. */
    private static Optional<CodeableConcept> priorityValue(Extension extension) {
        return referenceValue(extension, TransformProperties.VSM_PRIORITY_CODE);
    }

    /**
     * The CodeableConcept a crmi-intendedUsageContext extension states for one usage context code.
     */
    private static Optional<CodeableConcept> referenceValue(Extension extension, String usageContextCode) {
        if (extension == null
                || !TransformProperties.CRMI_INTENDED_USAGE_CONTEXT_EXT_URL.equals(extension.getUrl())
                // the value is a UsageContext, not a CodeableConcept -> casting blindly would throw
                || !(extension.getValue() instanceof UsageContext usageContext)
                || !usageContext.hasCode()
                || !usageContextCode.equals(usageContext.getCode().getCode())
                || !usageContext.hasValueCodeableConcept()) {
            return Optional.empty();
        }
        return Optional.of(usageContext.getValueCodeableConcept());
    }
}
