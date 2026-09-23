package org.opencds.cqf.fhir.cr.crmi.changelog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.RelatedArtifact;

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
        // An operation on one of a relatedArtifact's extensions is a condition or a priority. Both are
        // decided by comparing the two sides in ChangeLog.addConditionsAndPriorities. Deliberately
        // drop diffs positionally reported operations here.
        var isExtensionOperation =
                path.contains("xtension[") || currentValue instanceof Extension || originalValue instanceof Extension;
        if (operationTarget.isPresent() && !isExtensionOperation) {
            operationTarget.get().setOperation(newOperation);
        }
    }
}
