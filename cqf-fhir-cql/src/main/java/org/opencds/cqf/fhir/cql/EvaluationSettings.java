package org.opencds.cqf.fhir.cql;

import jakarta.annotation.Nonnull;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;

public class EvaluationSettings {

    private Map<ModelIdentifier, Model> modelCache;
    private Map<VersionedIdentifier, CompiledLibrary> libraryCache;
    private Map<String, List<Code>> valueSetCache;

    private CqlOptions cqlOptions;

    private RetrieveSettings retrieveSettings;
    private TerminologySettings terminologySettings;
    private ZoneId clientTimezone;

    public static EvaluationSettings getDefault() {
        EvaluationSettings settings = new EvaluationSettings();

        var options = CqlOptions.defaultOptions();
        settings.setCqlOptions(options);
        settings.setModelCache(new ConcurrentHashMap<>());
        settings.setLibraryCache(new ConcurrentHashMap<>());
        settings.setValueSetCache(new ConcurrentHashMap<>());
        settings.setRetrieveSettings(new RetrieveSettings());
        settings.setTerminologySettings(new TerminologySettings());
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

    public CqlOptions getCqlOptions() {
        return this.cqlOptions;
    }

    public EvaluationSettings withCqlOptions(CqlOptions cqlOptions) {
        setCqlOptions(cqlOptions);
        return this;
    }

    public EvaluationSettings setCqlOptions(CqlOptions cqlOptions) {
        this.cqlOptions = cqlOptions;
        return this;
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

    public TerminologySettings getTerminologySettings() {
        return this.terminologySettings;
    }

    public EvaluationSettings withTerminologySettings(TerminologySettings terminologySettings) {
        setTerminologySettings(terminologySettings);
        return this;
    }

    public EvaluationSettings setTerminologySettings(TerminologySettings terminologySettings) {
        this.terminologySettings = terminologySettings;
        return this;
    }

    @Nonnull
    public ZoneId getClientTimezone() {
        return Optional.ofNullable(clientTimezone).orElse(ZoneOffset.UTC);
    }

    public EvaluationSettings setClientTimezone(ZoneId clientTimezone) {
        this.clientTimezone = clientTimezone;
        return this;
    }
}
