package org.opencds.cqf.cql.evaluator.cql2elm.model;

import java.util.Map;

import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.cql.model.ModelIdentifier;

/**
 * This class extends the CQL translator {@link org.cqframework.cql.cql2elm.ModelManager} class to
 * be aware of a global cache of {@link org.cqframework.cql.cql2elm.model.Model}s The global cache
 * is by @{org.hl7.cql.model.ModelIdentifier}, while the local cache is by name. This is because the
 * translator expects the ModelManager to only permit loading of a single version version of a given
 * Model in a single translation context, while the global cache is for all versions of Models
 *
 * As of 2.3.0, the translator ModelManager has incorporated support for the use of the global cache
 * by ModelIdentifier, so this class is now a backwards-compatibility wrapper for that functionality
 * and should be deprecated.
 */
public class CacheAwareModelManager extends ModelManager {

  /**
   * @param globalCache cache for Models by ModelIdentifier. Expected to be thread-safe.
   */
  public CacheAwareModelManager(Map<ModelIdentifier, Model> globalCache) {
    super(globalCache);
  }
}
