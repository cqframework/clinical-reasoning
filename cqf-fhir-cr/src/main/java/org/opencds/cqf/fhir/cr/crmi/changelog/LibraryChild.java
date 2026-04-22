package org.opencds.cqf.fhir.cr.crmi.changelog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.fhir.cr.crmi.TransformProperties;

public class LibraryChild extends PageBase {

    private final ValueAndOperation purpose = new ValueAndOperation();
    private final ValueAndOperation effectiveStart = new ValueAndOperation();
    private final ValueAndOperation releaseDate = new ValueAndOperation();
    private final List<RelatedArtifactUrlWithOperation> relatedArtifacts = new ArrayList<>();

    @SuppressWarnings("java:S107")
    LibraryChild(
            String name,
            String purpose,
            String title,
            String id,
            String version,
            String url,
            String effectiveStart,
            String releaseDate,
            List<RelatedArtifact> relatedArtifacts) {
        super(title, id, version, name, url, "Library");
        if (!StringUtils.isEmpty(purpose)) {
            this.purpose.setValue(purpose);
        }
        if (!StringUtils.isEmpty(effectiveStart)) {
            this.effectiveStart.setValue(effectiveStart);
        }
        if (!StringUtils.isEmpty(releaseDate)) {
            this.releaseDate.setValue(releaseDate);
        }
        if (!relatedArtifacts.isEmpty()) {
            relatedArtifacts.forEach(ra -> this.relatedArtifacts.add(new RelatedArtifactUrlWithOperation(ra)));
        }
    }

    public ValueAndOperation getPurpose() {
        return purpose;
    }

    public ValueAndOperation getEffectiveStart() {
        return effectiveStart;
    }

    public ValueAndOperation getReleaseDate() {
        return releaseDate;
    }

    public List<RelatedArtifactUrlWithOperation> getRelatedArtifacts() {
        return relatedArtifacts;
    }

    private Optional<RelatedArtifactUrlWithOperation> getRelatedArtifactFromUrl(String target) {
        return this.relatedArtifacts.stream()
                .filter(ra -> ra.getValue() != null && ra.getValue().equals(target))
                .findAny();
    }

    private void tryAddConditionOperation(
            Extension maybeCondition, RelatedArtifactUrlWithOperation target, Operation newOperation) {
        if (maybeCondition.getUrl().equals(TransformProperties.vsmCondition)) {
            target.getConditions().stream()
                    .filter(e -> e.getValue()
                                    .getCodingFirstRep()
                                    .getSystem()
                                    .equals(((CodeableConcept) maybeCondition.getValue())
                                            .getCodingFirstRep()
                                            .getSystem())
                            && e.getValue()
                                    .getCodingFirstRep()
                                    .getCode()
                                    .equals(((CodeableConcept) maybeCondition.getValue())
                                            .getCodingFirstRep()
                                            .getCode()))
                    .findAny()
                    .ifPresent(condition -> condition.setOperation(newOperation));
        }
    }

    private void tryAddPriorityOperation(
            Extension maybePriority, RelatedArtifactUrlWithOperation target, Operation newOperation) {
        if (maybePriority.getUrl().equals(TransformProperties.vsmPriority)
                && (target.getPriority().getValue() != null
                        && target.getPriority()
                                .getValue()
                                .getCodingFirstRep()
                                .getSystem()
                                .equals(((CodeableConcept) maybePriority.getValue())
                                        .getCodingFirstRep()
                                        .getSystem())
                        && target.getPriority()
                                .getValue()
                                .getCodingFirstRep()
                                .getCode()
                                .equals(((CodeableConcept) maybePriority.getValue())
                                        .getCodingFirstRep()
                                        .getCode()))) {
            // priority will always be replace because:
            // insert = an extension exists where it did not before, which is a replacement from "routine"
            // to "emergent"
            // delete = an extension does not exist where it did before, which is a replacement from
            // "emergent" to "routine"
            newOperation.setType(ChangeLog.REPLACE);
            target.getPriority().setOperation(newOperation);
        }
    }

    @Override
    public void addOperation(String type, String path, Object currentValue, Object originalValue) {
        if (type != null) {
            super.addOperation(type, path, currentValue, originalValue);
            var newOperation = new Operation(type, path, currentValue, originalValue);
            if (path != null && path.contains("elatedArtifact")) {
                addOperationHandleRelatedArtifacts(path, currentValue, originalValue, newOperation);
            } else if (path != null && path.equals("name")) {
                this.getName().setOperation(newOperation);
            } else if (path != null && path.contains("purpose")) {
                this.purpose.setOperation(newOperation);
            } else if (path != null && path.equals("approvalDate")) {
                this.releaseDate.setOperation(newOperation);
            } else if (path != null && path.contains("effectivePeriod")) {
                this.effectiveStart.setOperation(newOperation);
            }
        }
    }

    private void addOperationHandleRelatedArtifacts(
            String path, Object currentValue, Object originalValue, Operation newOperation) {
        Optional<RelatedArtifactUrlWithOperation> operationTarget = Optional.empty();
        if (currentValue instanceof RelatedArtifact currentRelatedArtifact) {
            operationTarget = getRelatedArtifactFromUrl(currentRelatedArtifact.getResource());
        } else if (originalValue instanceof RelatedArtifact originalRelatedArtifact) {
            operationTarget = getRelatedArtifactFromUrl(originalRelatedArtifact.getResource());
        } else if (path.contains("[")) {
            var matcher = Pattern.compile("relatedArtifact\\[(\\d+)]").matcher(path);
            if (matcher.find()) {
                var relatedArtifactIndex = Integer.parseInt(matcher.group(1));
                operationTarget = Optional.of(this.relatedArtifacts.get(relatedArtifactIndex));
            }
        }
        if (operationTarget.isPresent()) {
            if (path.contains("xtension[")) {
                var matcher = Pattern.compile("xtension\\[(\\d+)]").matcher(path);
                if (matcher.find()) {
                    var extension = operationTarget
                            .get()
                            .getFullRelatedArtifact()
                            .getExtension()
                            .get(Integer.parseInt(matcher.group(1)));
                    tryAddConditionOperation(extension, operationTarget.orElse(null), newOperation);
                    tryAddPriorityOperation(extension, operationTarget.orElse(null), newOperation);
                }
            } else if (currentValue instanceof Extension currentExtension) {
                tryAddConditionOperation(currentExtension, operationTarget.orElse(null), newOperation);
                tryAddPriorityOperation(currentExtension, operationTarget.orElse(null), newOperation);
            } else if (originalValue instanceof Extension originalExtension) {
                tryAddConditionOperation(originalExtension, operationTarget.orElse(null), newOperation);
                tryAddPriorityOperation(originalExtension, operationTarget.orElse(null), newOperation);
            } else {
                operationTarget.get().setOperation(newOperation);
            }
        }
    }
}
