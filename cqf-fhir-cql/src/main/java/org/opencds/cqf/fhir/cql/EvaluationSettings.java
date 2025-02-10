package org.opencds.cqf.fhir.cql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.fhir.npm.NpmProcessor;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;

public class EvaluationSettings {

    private Map<ModelIdentifier, Model> modelCache;
    private Map<VersionedIdentifier, CompiledLibrary> libraryCache;
    private Map<String, List<Code>> valueSetCache;
    private List<LibrarySourceProvider> librarySourceProviders;

    private CqlOptions cqlOptions;

    private RetrieveSettings retrieveSettings;
    private TerminologySettings terminologySettings;
    private NpmProcessor npmProcessor;

    public static EvaluationSettings getDefault() {
        return new EvaluationSettings();
    }

    EvaluationSettings() {
        this.modelCache = new ConcurrentHashMap<>();
        this.libraryCache = new ConcurrentHashMap<>();
        this.valueSetCache = new ConcurrentHashMap<>();
        this.librarySourceProviders = new ArrayList<>();
        this.cqlOptions = CqlOptions.defaultOptions();
        this.retrieveSettings = new RetrieveSettings();
        this.terminologySettings = new TerminologySettings();
        this.npmProcessor = null;
    }

    /**
     * Copy constructor for EvaluationSettings
     * @param settings
     */
    public EvaluationSettings(EvaluationSettings settings) {
        this.modelCache = new ConcurrentHashMap<>(settings.modelCache);
        this.libraryCache = new ConcurrentHashMap<>(settings.libraryCache);
        this.valueSetCache = new ConcurrentHashMap<>(settings.valueSetCache);
        this.cqlOptions = settings.cqlOptions;
        this.retrieveSettings = new RetrieveSettings(settings.retrieveSettings);
        this.terminologySettings = new TerminologySettings(settings.terminologySettings);
        this.librarySourceProviders = new ArrayList<>(settings.librarySourceProviders);
        this.npmProcessor =
                settings.npmProcessor != null ? new NpmProcessor(settings.npmProcessor.getIgContext()) : null;
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

    public TerminologySettings getTerminologySettings() {
        return this.terminologySettings;
    }

    public EvaluationSettings withTerminologySettings(TerminologySettings terminologySettings) {
        setTerminologySettings(terminologySettings);
        return this;
    }

    public void setTerminologySettings(TerminologySettings terminologySettings) {
        this.terminologySettings = terminologySettings;
    }

    public List<LibrarySourceProvider> getLibrarySourceProviders() {
        return librarySourceProviders;
    }

    public void setLibrarySourceProviders(List<LibrarySourceProvider> librarySourceProviders) {
        this.librarySourceProviders = librarySourceProviders;
    }

    public EvaluationSettings withLibrarySourceProviders(List<LibrarySourceProvider> librarySourceProviders) {
        setLibrarySourceProviders(librarySourceProviders);
        return this;
    }

    public NpmProcessor getNpmProcessor() {
        return npmProcessor;
    }

    public void setNpmProcessor(NpmProcessor npmProcessor) {
        this.npmProcessor = npmProcessor;
    }

    public EvaluationSettings withNpmProcessor(NpmProcessor npmProcessor) {
        setNpmProcessor(npmProcessor);
        return this;
    }
}
