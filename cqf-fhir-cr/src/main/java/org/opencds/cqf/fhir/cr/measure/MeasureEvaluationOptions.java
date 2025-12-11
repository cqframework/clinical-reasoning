package org.opencds.cqf.fhir.cr.measure;

import java.util.HashMap;
import java.util.Map;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
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
}
