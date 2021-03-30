package org.opencds.cqf.cql.evaluator.cql2elm.model;

import static java.util.Objects.requireNonNull;

import org.cqframework.cql.cql2elm.ModelInfoLoader;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.cql2elm.model.SystemModel;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * This class extends the CQL translator {@link org.cqframework.cql.cql2elm.ModelManager} class to be aware of a global cache of {@link org.cqframework.cql.cql2elm.model.Model}s
 * The global cache is by @{org.hl7.elm.r1.VersionedIdentifier}, while the local cache is by name. This is because the translator expects the ModelManager to only permit loading
 * of a single version version of a given Model in a single translation context, while the global cache is for all versions of Models
 */
public class CacheAwareModelManager extends ModelManager {

    private final Map<VersionedIdentifier, Model> globalCache;

    private final Map<String, Model> localCache;

    private final ModelInfoLoader modelInfoLoader;

    /**
     * @param globalCache cache for Models by VersionedIdentifier. Expected to be thread-safe.
     */
    public CacheAwareModelManager(Map<VersionedIdentifier, Model> globalCache) {
        requireNonNull(globalCache, "globalCache can not be null.");

        this.globalCache = globalCache;
        this.localCache = new HashMap<>();
        this.modelInfoLoader = new ModelInfoLoader();
    }

	private Model buildModel(VersionedIdentifier identifier) {
        Model model = null;
        try {

            ModelInfo modelInfo = this.modelInfoLoader.getModelInfo(identifier);
            if (identifier.getId().equals("System")) {
                model = new SystemModel(modelInfo);
            }
            else {
                model = new Model(modelInfo, this);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Could not load model information for model %s, version %s.",
                    identifier.getId(), identifier.getVersion()));
        }

        return model;
    }

    /**
     * @param modelIdentifier the identifer of the model to resolve
     * @return the model
     * @throws IllegalArgumentException if an attempt to resolve multiple versions of the same model is made
     */

    @Override
    public Model resolveModel(VersionedIdentifier modelIdentifier) {
        Model model = null;
        if (this.localCache.containsKey(modelIdentifier.getId())) {
            model = this.localCache.get(modelIdentifier.getId());
            if (modelIdentifier.getVersion() != null && !modelIdentifier.getVersion().equals(model.getModelInfo().getVersion())) {
                throw new IllegalArgumentException(String.format("Could not load model information for model %s, version %s because version %s is already loaded.",
                        modelIdentifier.getId(), modelIdentifier.getVersion(), model.getModelInfo().getVersion()));
            }

        }

        if (model == null && this.globalCache.containsKey(modelIdentifier)) {
            model = this.globalCache.get(modelIdentifier);
            this.localCache.put(modelIdentifier.getId(), model);
        }

        if (model == null) {
            model = buildModel(modelIdentifier);
            this.globalCache.put(modelIdentifier, model);
            this.localCache.put(modelIdentifier.getId(), model);
        }

        return model;
    }
}
