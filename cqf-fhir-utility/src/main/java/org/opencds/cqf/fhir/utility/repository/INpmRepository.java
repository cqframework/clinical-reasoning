package org.opencds.cqf.fhir.utility.repository;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface INpmRepository {
    <T extends IBaseResource> List<T> resolveByUrl(Class<T> clazz, String url);
}
