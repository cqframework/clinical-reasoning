package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.collect.Multimap;
import com.rits.cloning.Cloner;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.repository.operations.IRepositoryOperationProvider;

/**
 * This class exists for testing only, and is meant strictly to mimic the behaviour of an actual
 * production repository, backed by a database.   More specifically, it ensures that each query
 * for a resource will return a new instance of that resource, as opposed to returning the same
 * object in memory with the same reference, as the superclass does.  This is mean to help us
 * detect bugs that would occur in a real-world scenario, where a resource is fetched with a new
 * instance each time.
 */
public class IgRepositoryForTests extends IgRepository implements IRepository {

    private static final Cloner cloner = new Cloner();

    public IgRepositoryForTests(FhirContext fhirContext, Path root) {
        super(fhirContext, root);
    }

    public IgRepositoryForTests(
            FhirContext fhirContext,
            Path root,
            IgConventions conventions,
            IRepositoryOperationProvider operationProvider) {
        super(fhirContext, root, conventions, operationProvider);
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {

        return duplicateObject(super.read(resourceType, id, headers));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {

        return duplicateObject(super.search(bundleType, resourceType, searchParameters, headers));
    }

    private <T extends IBaseResource> T duplicateObject(T originalObject) {
        return cloner.deepClone(originalObject);
    }
}
