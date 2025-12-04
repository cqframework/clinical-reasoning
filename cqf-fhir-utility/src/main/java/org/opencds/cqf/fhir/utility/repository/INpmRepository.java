package org.opencds.cqf.fhir.utility.repository;

import java.util.List;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface INpmRepository {

    /**
     * Resolve a resource by a class and url
     * @param clazz - the class of the resource desired
     * @param url - url of the resource desired (can be null)
     * @return list of resources that match the condition
     */
    <T extends IBaseResource> List<T> resolveByUrl(@Nonnull Class<T> clazz, String url);
}
