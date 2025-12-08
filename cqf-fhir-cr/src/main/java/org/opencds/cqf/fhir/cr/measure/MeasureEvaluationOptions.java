package org.opencds.cqf.fhir.cr.measure;

import java.util.HashMap;
import java.util.Map;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.measure.common.DefCaptureCallback;
import org.opencds.cqf.fhir.utility.ValidationProfile;

public class MeasureEvaluationOptions {
    public static MeasureEvaluationOptions defaultOptions() {
        var options = new MeasureEvaluationOptions();
        options.setEvaluationSettings(EvaluationSettings.getDefault());
        return options;
    }

    private boolean applyScoringSetMembership = true;
    private boolean isValidationEnabled = false;
    private Map<String, ValidationProfile> validationProfiles = new HashMap<>();
    private SubjectProviderOptions subjectProviderOptions;

    private EvaluationSettings evaluationSettings = null;

    private DefCaptureCallback defCaptureCallback = null;

    public boolean isValidationEnabled() {
        return this.isValidationEnabled;
    }

    public MeasureEvaluationOptions setValidationEnabled(boolean enableValidation) {
        this.isValidationEnabled = enableValidation;
        return this;
    }

    public Map<String, ValidationProfile> getValidationProfiles() {
        return validationProfiles;
    }

    public MeasureEvaluationOptions setValidationProfiles(Map<String, ValidationProfile> validationProfiles) {
        this.validationProfiles = validationProfiles;
        return this;
    }

    public MeasureEvaluationOptions setEvaluationSettings(EvaluationSettings evaluationSettings) {
        this.evaluationSettings = evaluationSettings;
        return this;
    }

    public EvaluationSettings getEvaluationSettings() {
        return this.evaluationSettings;
    }

    public SubjectProviderOptions getSubjectProviderOptions() {
        if (this.subjectProviderOptions == null) {
            subjectProviderOptions = new SubjectProviderOptions();
        }
        return subjectProviderOptions;
    }

    public MeasureEvaluationOptions setApplyScoringSetMembership(boolean applyScoringSetMembership) {
        this.applyScoringSetMembership = applyScoringSetMembership;
        return this;
    }

    public boolean getApplyScoringSetMembership() {
        return this.applyScoringSetMembership;
    }

    /**
     * Get the DefCaptureCallback for capturing MeasureDef snapshots during evaluation.
     * <p>
     * This callback is invoked by MeasureProcessor implementations after processResults()
     * completes, allowing test frameworks to capture immutable snapshots of the MeasureDef state.
     * <p>
     * If null (default), no snapshot capture occurs (zero production impact).
     *
     * @return the DefCaptureCallback, or null if not set
     * @see DefCaptureCallback
     */
    public DefCaptureCallback getDefCaptureCallback() {
        return this.defCaptureCallback;
    }

    /**
     * Set the DefCaptureCallback for capturing MeasureDef snapshots during evaluation.
     * <p>
     * This is an opt-in mechanism for testing frameworks. When set, the callback will be
     * invoked after processResults() completes with an immutable snapshot of the MeasureDef.
     * <p>
     * Setting to null (default) disables snapshot capture entirely.
     *
     * @param defCaptureCallback the callback to invoke, or null to disable
     * @return this MeasureEvaluationOptions instance for method chaining
     * @see DefCaptureCallback
     */
    public MeasureEvaluationOptions setDefCaptureCallback(DefCaptureCallback defCaptureCallback) {
        this.defCaptureCallback = defCaptureCallback;
        return this;
    }
}
