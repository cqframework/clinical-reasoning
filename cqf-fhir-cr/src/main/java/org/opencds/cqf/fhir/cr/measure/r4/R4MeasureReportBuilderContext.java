package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CRITERIA_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_POPULATION_DESCRIPTION_URL;

import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;
import org.opencds.cqf.fhir.cr.measure.common.MeasureValidationException;

/**
 * Package-private context class for building R4 MeasureReports.
 * Extracted from R4MeasureReportBuilder inner class.
 */
class R4MeasureReportBuilderContext {
    private final MeasureDef measureDef;
    private final MeasureEvaluationState state;
    private final MeasureReport measureReport;

    private final HashMap<String, Reference> evaluatedResourceReferences = new HashMap<>();
    private final HashMap<String, Reference> supplementalDataReferences = new HashMap<>();
    private final Map<String, Resource> contained = new HashMap<>();

    public R4MeasureReportBuilderContext(
            MeasureDef measureDef, MeasureEvaluationState state, MeasureReport measureReport) {
        this.measureDef = measureDef;
        this.state = state;
        this.measureReport = measureReport;
    }

    // For error messages:
    public String getMeasureUrl() {
        return this.measureDef.url();
    }

    public Map<String, Resource> contained() {
        return this.contained;
    }

    public void addContained(Resource r) {
        this.contained.putIfAbsent(this.getId(r), r);
    }

    public MeasureReport report() {
        return this.measureReport;
    }

    public MeasureDef measureDef() {
        return this.measureDef;
    }

    public MeasureEvaluationState state() {
        return this.state;
    }

    public Map<String, Reference> evaluatedResourceReferences() {
        return this.evaluatedResourceReferences;
    }

    public Map<String, Reference> supplementalDataReferences() {
        return this.supplementalDataReferences;
    }

    public Reference addSupplementalDataReference(String id) {
        validateReference(id);
        return this.supplementalDataReferences().computeIfAbsent(id, x -> new Reference(id));
    }

    public Reference addEvaluatedResourceReference(String id) {
        validateReference(id);
        return this.evaluatedResourceReferences().computeIfAbsent(id, x -> new Reference(id));
    }

    public boolean hasEvaluatedResource(String id) {
        validateReference(id);
        return this.evaluatedResourceReferences().containsKey(id);
    }

    public void addCriteriaExtensionToReference(Reference reference, String criteriaId) {
        if (criteriaId == null) throw new AssertionError("CriteriaId is required for extension references");
        var ext = new Extension(EXT_CRITERIA_REFERENCE_URL, new StringType(criteriaId));
        addExtensionIfNotExists(reference, ext);
    }

    public void addCriteriaExtensionToSupplementalData(Resource resource, String criteriaId, String description) {
        var id = getId(resource);

        // This is not an evaluated resource, so add it to the contained resources
        if (!hasEvaluatedResource(id)) {
            this.addContained(resource);
            id = "#" + resource.getIdElement().getIdPart();
        }
        var ref = addSupplementalDataReference(id);
        addCriteriaExtensionToReference(ref, criteriaId);
        if (description != null && !description.isEmpty()) {
            addExtensionIfNotExists(ref, new Extension(EXT_POPULATION_DESCRIPTION_URL, new StringType(description)));
        }
    }

    public void addCriteriaExtensionToEvaluatedResource(Resource resource, String criteriaId) {
        var id = getId(resource);
        var ref = addEvaluatedResourceReference(id);
        addCriteriaExtensionToReference(ref, criteriaId);
    }

    private String getId(Resource resource) {
        return resource.fhirType() + "/" + resource.getIdElement().getIdPart();
    }

    private void addExtensionIfNotExists(Element element, Extension ext) {
        for (var e : element.getExtension()) {
            if (e.getUrl().equals(ext.getUrl()) && e.getValue().equalsShallow(ext.getValue())) {
                return;
            }
        }

        element.addExtension(ext);
    }

    private void validateReference(String reference) {
        // Can't be null
        if (reference == null) {
            throw new MeasureValidationException("validated reference is null");
        }

        // If it's a contained reference, must be just the Guid and nothing else
        if (reference.startsWith("#") && reference.contains("/")) {
            throw new MeasureValidationException("Invalid contained reference: " + reference);
        }

        // If it's a full reference, it must be type/id and that's it
        if (!reference.startsWith("#") && reference.split("/").length != 2) {
            throw new MeasureValidationException("Invalid full reference: " + reference);
        }
    }

    public void addOperationOutcomes() {
        var errorMsgs = this.state.errors();
        for (var error : errorMsgs) {
            addContained(createOperationOutcome(error));
        }
    }

    private OperationOutcome createOperationOutcome(String errorMsg) {
        OperationOutcome op = new OperationOutcome();
        op.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(IssueType.EXCEPTION)
                .setDiagnostics(errorMsg);
        return op;
    }
}
