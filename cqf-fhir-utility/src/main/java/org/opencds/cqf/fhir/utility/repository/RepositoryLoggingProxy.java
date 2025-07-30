package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  this is temporary until performance testing is complete
public class RepositoryLoggingProxy implements IRepository {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryLoggingProxy.class);

    private final IRepository repository;

    public static RepositoryLoggingProxy init(IRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }

        // Don't wrap a RepositoryLoggingProxy in a RepositoryLoggingProxy
        if (repository instanceof RepositoryLoggingProxy castedRepository) {
            logger.info("This is already a RepositoryLoggingProxy, so returning it directly");
            return castedRepository;
        }

        logger.info(
                "Initializing RepositoryLoggingProxy for repository: {}",
                repository.getClass().getName());
        return new RepositoryLoggingProxy(repository);
    }

    private RepositoryLoggingProxy(IRepository repository) {
        this.repository = repository;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> aClass, I i, Map<String, String> map) {
        logger.info("1234567890 - read()");
        return repository.read(aClass, i, map);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T t, Map<String, String> map) {
        logger.info("1234567890 - create()");
        return repository.create(t, map);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T t, Map<String, String> map) {
        logger.info("1234567890 - update()");
        return repository.update(t, map);
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> aClass, I i, Map<String, String> map) {
        logger.info("1234567890 - delete()");
        return repository.delete(aClass, i, map);
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> aClass,
            Class<T> aClass1,
            Multimap<String, List<IQueryParameterType>> multimap,
            Map<String, String> map) {
        logger.info("1234567890 - search()");
        return repository.search(aClass, aClass1, multimap, map);
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> aClass, String s, P p, Class<R> aClass1, Map<String, String> map) {
        logger.info("1234567890 - invoke()");
        return repository.invoke(aClass, s, p, aClass1, map);
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I i, String s, P p, Class<R> aClass, Map<String, String> map) {
        logger.info("1234567890 - invoke()");
        return repository.invoke(aClass, s, p, aClass, map);
    }

    @Override
    public FhirContext fhirContext() {
        return repository.fhirContext();
    }
}
