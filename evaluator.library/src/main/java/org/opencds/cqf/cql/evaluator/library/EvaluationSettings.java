package org.opencds.cqf.cql.evaluator.library;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.ModelIdentifier;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.engine.CqlEngineOptions;

import ca.uhn.fhir.context.FhirContext;

public class EvaluationSettings {

  private Map<ModelIdentifier, Model> globalModelCache;

  private Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache;

  private CqlOptions cqlOptions;

  private CqlEngineOptions engineOptions;

  private FhirContext fhirContext;

  public static EvaluationSettings getDefault() {
    EvaluationSettings settings = new EvaluationSettings();

    settings.setCqlOptions(CqlOptions.defaultOptions());
    settings.setGlobalModelCache(new ConcurrentHashMap<>());
    settings.setEngineOptions(CqlEngineOptions.defaultOptions());
    settings.setLibraryCache(new HashMap<>());

    return settings;
  }

  public static EvaluationSettings newInstance() {
    return new EvaluationSettings();
  }

  public Map<ModelIdentifier, Model> getGlobalModelCache() {
    if (this.globalModelCache == null) {
      this.globalModelCache = new ConcurrentHashMap<>();
    }
    return this.globalModelCache;
  }

  public void setGlobalModelCache(Map<ModelIdentifier, Model> globalModelCache) {
    this.globalModelCache = globalModelCache;
  }

  public EvaluationSettings withGlobalModelCache(Map<ModelIdentifier, Model> globalModelCache) {
    setGlobalModelCache(globalModelCache);
    return this;
  }

  public Map<VersionedIdentifier, Library> getLibraryCache() {
    return this.libraryCache;
  }

  public void setLibraryCache(Map<VersionedIdentifier, Library> libraryCache) {
    this.libraryCache = libraryCache;
  }

  public EvaluationSettings withLibraryCache(Map<VersionedIdentifier, Library> libraryCache) {
    setLibraryCache(libraryCache);
    return this;
  }

  public CqlOptions getCqlOptions() {
    if (this.cqlOptions == null) {
      this.cqlOptions = CqlOptions.defaultOptions();
    }
    return this.cqlOptions;
  }

  public EvaluationSettings withCqlOptions(CqlOptions cqlOptions) {
    setCqlOptions(cqlOptions);
    return this;
  }

  public void setCqlOptions(CqlOptions cqlOptions) {
    this.cqlOptions = cqlOptions;
  }

  public CqlEngineOptions getEngineOptions() {
    return this.engineOptions;
  }

  public void setEngineOptions(CqlEngineOptions engineOptions) {
    this.engineOptions = engineOptions;
  }

  public EvaluationSettings withEngineOptions(CqlEngineOptions engineOptions) {
    setEngineOptions(engineOptions);
    return this;
  }

  public FhirContext getFhirContext() {
    return this.fhirContext;
  }

  public void setFhirContext(FhirContext fhirContext) {
    this.fhirContext = fhirContext;
  }

  public EvaluationSettings withFhirContext(FhirContext fhirContext) {
    setFhirContext(fhirContext);
    return this;
  }

}
