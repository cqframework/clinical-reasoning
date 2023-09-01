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

    private boolean isValidationEnabled = false;
    private Map<String, ValidationProfile> validationProfiles = new HashMap<>();

    private EvaluationSettings evaluationSettings = null;

    public boolean isValidationEnabled() {
        return this.isValidationEnabled;
    }

    public void setValidationEnabled(boolean enableValidation) {
        this.isValidationEnabled = enableValidation;
    }

    public Map<String, ValidationProfile> getValidationProfiles() {
        return validationProfiles;
    }

    public void setValidationProfiles(Map<String, ValidationProfile> validationProfiles) {
        this.validationProfiles = validationProfiles;
    }

    public void setEvaluationSettings(EvaluationSettings evaluationSettings) {
        this.evaluationSettings = evaluationSettings;
    }

    public EvaluationSettings getEvaluationSettings() {
        return this.evaluationSettings;
    }
}
