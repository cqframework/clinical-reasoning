package org.opencds.cqf.cql.evaluator.measure;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opencds.cqf.cql.evaluator.fhir.util.ValidationProfile;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

public class MeasureEvaluationOptions {
  public static MeasureEvaluationOptions defaultOptions() {
    return new MeasureEvaluationOptions();
  }

  private boolean isThreadedEnabled = false;

  private Executor measureExecutor;
  private Integer threadedBatchSize = 200;
  private Integer numThreads = null;
  private boolean isValidationEnabled = false;
  private Map<String, ValidationProfile> validationProfiles = new HashMap<>();

  public Executor getMeasureExecutor(){
    return this.measureExecutor;
  }

  public void setMeasureExecutor(Executor cqlExecutor){
    this.measureExecutor = cqlExecutor;
  }
  public boolean isThreadedEnabled() {
    return this.isThreadedEnabled;
  }

  public void setThreadedEnabled(Boolean isThreadedEnabled) {
    this.isThreadedEnabled = isThreadedEnabled;
  }

  public Integer getThreadedBatchSize() {
    return this.threadedBatchSize;
  }

  public void setThreadedBatchSize(Integer threadedBatchSize) {
    this.threadedBatchSize = threadedBatchSize;
  }

  public Integer getNumThreads() {
    if (this.numThreads == null) {
      return Runtime.getRuntime().availableProcessors();
    }

    return this.numThreads;
  }

  public void setNumThreads(Integer value) {
    this.numThreads = value;
  }

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
