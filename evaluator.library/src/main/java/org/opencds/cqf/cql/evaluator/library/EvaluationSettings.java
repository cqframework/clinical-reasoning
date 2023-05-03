package org.opencds.cqf.cql.evaluator.library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.ModelIdentifier;
import org.opencds.cqf.cql.evaluator.CqlOptions;

public class EvaluationSettings {

  private Map<ModelIdentifier, Model> modelCache;
  private Map<VersionedIdentifier, Library> libraryCache;

  private CqlOptions cqlOptions;

  public static EvaluationSettings getDefault() {
    EvaluationSettings settings = new EvaluationSettings();

    var options = CqlOptions.defaultOptions();
    settings.setCqlOptions(options);
    settings.setModelCache(new ConcurrentHashMap<>());
    settings.setLibraryCache(new ConcurrentHashMap<>());

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
}
