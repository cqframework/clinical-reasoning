package org.opencds.cqf.cql.evaluator.measure;

import java.util.HashMap;
import java.util.Map;
import org.opencds.cqf.cql.evaluator.fhir.util.ValidationProfile;

public class MeasureEvaluationOptions {
  public static MeasureEvaluationOptions defaultOptions() {
    return new MeasureEvaluationOptions();
  }
  private boolean isValidationEnabled = false;
  private Map<String, ValidationProfile> validationProfiles = new HashMap<>();

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
}
