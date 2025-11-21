package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.util.BundleBuilder;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.jetbrains.annotations.NotNull;

public class ComboProxyRepo implements IRepository {

    /**
     * Ordered list of repositories
     */
    protected final List<IRepository> orderedRepositories;

    private final FhirContext fhirContext;


    public ComboProxyRepo(FhirContext fhirContext, IRepository... repositories) {
        orderedRepositories = new ArrayList<>();
        this.fhirContext = fhirContext;
        for (IRepository r : repositories) {
            orderedRepositories.add(r);
        }
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> desiredClazz, I idClazz,
        Map<String, String> map) {
        T resource = null;
        for (IRepository repository : orderedRepositories) {
            resource = repository.read(desiredClazz, idClazz, map);

            if (resource != null) {
                break;
            }
        }
        return resource;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T clazz, Map<String, String> map) {
        throw new NotImplementedException("Create is not implemented");
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T clazz, Map<String, String> map) {
        throw new NotImplementedException("Update is not implemented");
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> clazz,
        I idType, Map<String, String> map) {
        throw new NotImplementedException("Delete is not implemented");
    }


    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> clazz,
        Class<T> desiredClazz, Multimap<String, List<IQueryParameterType>> map,
        Map<String, String> map2) {
        BundleBuilder builder = new BundleBuilder(fhirContext);
        for (IRepository repository : orderedRepositories) {
            B bundle = repository.search(clazz, desiredClazz, map, map2);
            builder.addCollectionEntry(bundle);
        }
        return (B) builder.getBundle();
    }

    @NotNull
    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }
}
