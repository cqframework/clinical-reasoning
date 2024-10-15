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

/**
 * This class contains settings used to set up CQL evaluation. This class is immutable once constructed.
 * Use the Builder to create and instance of this class, and the "toBuilder()" function to create a
 * new mutable builder to clone these settings if required.
 */
public class EvaluationSettings {

    private final Map<ModelIdentifier, Model> modelCache;
    private final Map<VersionedIdentifier, CompiledLibrary> libraryCache;
    private final Map<String, List<Code>> valueSetCache;
    private final List<LibrarySourceProvider> librarySourceProviders;
    private final CqlOptions cqlOptions;
    private final RetrieveSettings retrieveSettings;
    private final TerminologySettings terminologySettings;
    private final NpmProcessor npmProcessor;

    private EvaluationSettings(
            Map<ModelIdentifier, Model> modelCache,
            Map<VersionedIdentifier, CompiledLibrary> libraryCache,
            Map<String, List<Code>> valueSetCache,
            List<LibrarySourceProvider> librarySourceProviders,
            CqlOptions cqlOptions,
            RetrieveSettings retrieveSettings,
            TerminologySettings terminologySettings,
            NpmProcessor npmProcessor) {
        this.modelCache = modelCache;
        this.libraryCache = libraryCache;
        this.valueSetCache = valueSetCache;
        this.librarySourceProviders = librarySourceProviders;
        this.cqlOptions = cqlOptions;
        this.retrieveSettings = retrieveSettings;
        this.terminologySettings = terminologySettings;
        this.npmProcessor = npmProcessor;
    }

    public static EvaluationSettings.Builder builder() {
        return new Builder();
    }

    public EvaluationSettings.Builder toBuilder() {
        return new EvaluationSettings.Builder()
                .cqlOptions(this.cqlOptions)
                .modelCache(this.modelCache)
                .libraryCache(this.libraryCache)
                .valueSetCache(this.valueSetCache)
                .retrieveSettings(this.retrieveSettings)
                .terminologySettings(this.terminologySettings)
                .librarySourceProviders(new ArrayList<>(this.librarySourceProviders))
                .npmProcessor(this.npmProcessor);
    }

    public static EvaluationSettings getDefault() {
        var options = CqlOptions.defaultOptions();
        var builder = new EvaluationSettings.Builder()
                .cqlOptions(options)
                .modelCache(new ConcurrentHashMap<>())
                .libraryCache(new ConcurrentHashMap<>())
                .valueSetCache(new ConcurrentHashMap<>())
                .retrieveSettings(new RetrieveSettings())
                .terminologySettings(new TerminologySettings())
                .librarySourceProviders(new ArrayList<>());
        return builder.build();
    }

    public Map<ModelIdentifier, Model> getModelCache() {
        return this.modelCache;
    }

    public Map<VersionedIdentifier, CompiledLibrary> getLibraryCache() {
        return this.libraryCache;
    }

    public Map<String, List<Code>> getValueSetCache() {
        return this.valueSetCache;
    }

    public CqlOptions getCqlOptions() {
        return this.cqlOptions;
    }

    public RetrieveSettings getRetrieveSettings() {
        return this.retrieveSettings;
    }

    public TerminologySettings getTerminologySettings() {
        return this.terminologySettings;
    }

    public List<LibrarySourceProvider> getLibrarySourceProviders() {
        return librarySourceProviders;
    }

    public NpmProcessor getNpmProcessor() {
        return this.npmProcessor;
    }

    public static class Builder {
        private Map<ModelIdentifier, Model> modelCache;
        private Map<VersionedIdentifier, CompiledLibrary> libraryCache;
        private Map<String, List<Code>> valueSetCache;
        private List<LibrarySourceProvider> librarySourceProviders;
        private CqlOptions cqlOptions;
        private RetrieveSettings retrieveSettings;
        private TerminologySettings terminologySettings;
        private NpmProcessor npmProcessor;

        public Builder modelCache(Map<ModelIdentifier, Model> modelCache) {
            this.modelCache = modelCache;
            return this;
        }

        public Builder libraryCache(Map<VersionedIdentifier, CompiledLibrary> libraryCache) {
            this.libraryCache = libraryCache;
            return this;
        }

        public Builder valueSetCache(Map<String, List<Code>> valueSetCache) {
            this.valueSetCache = valueSetCache;
            return this;
        }

        public Builder librarySourceProviders(List<LibrarySourceProvider> librarySourceProviders) {
            this.librarySourceProviders = librarySourceProviders;
            return this;
        }

        public Builder cqlOptions(CqlOptions cqlOptions) {
            this.cqlOptions = cqlOptions;
            return this;
        }

        public Builder retrieveSettings(RetrieveSettings retrieveSettings) {
            this.retrieveSettings = retrieveSettings;
            return this;
        }

        public Builder terminologySettings(TerminologySettings terminologySettings) {
            this.terminologySettings = terminologySettings;
            return this;
        }

        public Builder npmProcessor(NpmProcessor npmProcessor) {
            this.npmProcessor = npmProcessor;
            return this;
        }

        public EvaluationSettings build() {
            return new EvaluationSettings(
                    this.modelCache,
                    this.libraryCache,
                    this.valueSetCache,
                    this.librarySourceProviders,
                    this.cqlOptions,
                    this.retrieveSettings,
                    this.terminologySettings,
                    this.npmProcessor);
        }
    }
}
