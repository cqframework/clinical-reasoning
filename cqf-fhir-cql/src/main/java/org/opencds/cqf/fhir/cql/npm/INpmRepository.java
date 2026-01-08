package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nonnull;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * A "repository" backed by fhir's NPM storage.
 */
public interface INpmRepository {

    /**
     * Resolve a resource by a class and url
     * @param clazz - the class of the resource desired
     * @param url - url of the resource desired (can be null)
     * @return list of resources that match the condition
     */
    <T extends IBaseResource> List<T> resolveByUrl(@Nonnull Class<T> clazz, String url);
}
