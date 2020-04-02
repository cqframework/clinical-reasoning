package org.opencds.cqf.cql.evaluator.manager;

import org.cqframework.cql.cql2elm.ModelInfoLoader;
import org.cqframework.cql.cql2elm.ModelInfoProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.cql2elm.model.SystemModel;
import org.hl7.elm.r1.VersionedIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Bryn on 12/29/2016.
 */
public class CacheAwareModelManager extends ModelManager {

    private final Map<VersionedIdentifier, Model> globalCache;

    private final Map<String, Model> localCache;

    public CacheAwareModelManager(Map<VersionedIdentifier, Model> globalCache) {
        this.globalCache = globalCache;
        this.localCache = new HashMap<>();
    }

	private Model buildModel(VersionedIdentifier identifier) {
        Model model = null;
        try {
            ModelInfoProvider provider = ModelInfoLoader.getModelInfoProvider(identifier);
            if (identifier.getId().equals("System")) {
                model = new SystemModel(provider.load());
            }
            else {
                model = new Model(provider.load(), resolveModel("System"));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Could not load model information for model %s, version %s.",
                    identifier.getId(), identifier.getVersion()));
        }

        return model;
    }

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
