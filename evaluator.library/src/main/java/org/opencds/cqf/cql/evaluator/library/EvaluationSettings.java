package org.opencds.cqf.cql.evaluator.library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.ModelIdentifier;
import org.opencds.cqf.cql.evaluator.CqlOptions;

import ca.uhn.fhir.context.FhirContext;

public class EvaluationSettings {


  private static final Map<ModelIdentifier, Model> GLOBAL_MODEL_CACHE = new ConcurrentHashMap<>();
  private static final Map<VersionedIdentifier, Library> GLOBAL_LIBRARY_CACHE =
      new ConcurrentHashMap<>();

  private Map<ModelIdentifier, Model> modelCache;
  private Map<VersionedIdentifier, Library> libraryCache;

  private CqlOptions cqlOptions;

  private FhirContext fhirContext;

  public static EvaluationSettings getDefault() {
    EvaluationSettings settings = new EvaluationSettings();

    var options = CqlOptions.defaultOptions();
    settings.setCqlOptions(options);
    settings.setModelCache(GLOBAL_MODEL_CACHE);
    settings.setLibraryCache(GLOBAL_LIBRARY_CACHE);

    return settings;
  }

  public Map<ModelIdentifier, Model> getModelCache() {
    return this.modelCache;
  }

  public void setModelCache(Map<ModelIdentifier, Model> modelCache) {
    this.modelCache = modelCache;
  }

  public EvaluationSettings withModelCache(Map<ModelIdentifier, Model> modelCache) {
    setModelCache(modelCache);
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
    return this.cqlOptions;
  }

  public EvaluationSettings withCqlOptions(CqlOptions cqlOptions) {
    setCqlOptions(cqlOptions);
    return this;
  }

  public void setCqlOptions(CqlOptions cqlOptions) {
    this.cqlOptions = cqlOptions;
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
