package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nonnull;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * A "repository" backed by fhir's NPM storage.
 */
public interface INpmRepository {

    /**
     * Resolve a resource by a class and url.
     * -
     * The url provided is optional (can be null), can contain a version (http://example.com|1.2.3),
     * or not (http://example.com).
     * -
     * If a version is included, it must be an exact match. If it's omitted, all like-urls will be matched.
     * Ie, http://example.com matches http://example.com|1.2.3 and http://example.com|2.3.4.
     * -
     * If no url is provided, all resources of the provided type are returned.
     *
     * @param clazz - the class of the resource desired (required; must not be null)
     * @param url - url of the resource desired (can be null)
     * @return list of resources that match the condition
     */
    <T extends IBaseResource> List<T> resolveByUrl(@Nonnull Class<T> clazz, String url);
}
