package org.opencds.cqf.fhir.cql;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.RepositoryTerminologyProvider.EXPANSION_MODE;

public class EvaluationSettings {

    private Map<ModelIdentifier, Model> modelCache;
    private Map<VersionedIdentifier, CompiledLibrary> libraryCache;
    private Map<String, List<Code>> valueSetCache;
    private EXPANSION_MODE expansionMode;

    private CqlOptions cqlOptions;

    private RetrieveSettings retrieveSettings;

    public static EvaluationSettings getDefault() {
        EvaluationSettings settings = new EvaluationSettings();

        var options = CqlOptions.defaultOptions();
        settings.setCqlOptions(options);
        settings.setModelCache(new ConcurrentHashMap<>());
        settings.setLibraryCache(new ConcurrentHashMap<>());
        settings.setValueSetCache(new ConcurrentHashMap<>());
        settings.setRetrieveSettings(new RetrieveSettings());
        settings.setExpansionMode(EXPANSION_MODE.AUTO);

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

    public Map<VersionedIdentifier, CompiledLibrary> getLibraryCache() {
        return this.libraryCache;
    }

    public void setLibraryCache(Map<VersionedIdentifier, CompiledLibrary> libraryCache) {
        this.libraryCache = libraryCache;
    }

    public EvaluationSettings withLibraryCache(Map<VersionedIdentifier, CompiledLibrary> libraryCache) {
        setLibraryCache(libraryCache);
        return this;
    }

    public Map<String, List<Code>> getValueSetCache() {
        return this.valueSetCache;
    }

    public void setValueSetCache(Map<String, List<Code>> valueSetCache) {
        this.valueSetCache = valueSetCache;
    }

    public EvaluationSettings withValueSetCache(Map<String, List<Code>> valueSetCache) {
        setValueSetCache(valueSetCache);
        return this;
    }

    public EXPANSION_MODE getExpansionMode() {
        return this.expansionMode;
    }

    public void setExpansionMode(EXPANSION_MODE expansionMode) {
        this.expansionMode = expansionMode;
    }

    public EvaluationSettings withExpansionMode(EXPANSION_MODE expansionMode) {
        setExpansionMode(expansionMode);
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

    public RetrieveSettings getRetrieveSettings() {
        return this.retrieveSettings;
    }

    public EvaluationSettings withRetrieveSettings(RetrieveSettings retrieveSettings) {
        setRetrieveSettings(retrieveSettings);
        return this;
    }

    public void setRetrieveSettings(RetrieveSettings retrieveSettings) {
        this.retrieveSettings = retrieveSettings;
    }
}
