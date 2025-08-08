package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  this is temporary until performance testing is complete
public class RepositoryLoggingProxy implements IRepository {
    private static final AtomicLong searchCounter = new AtomicLong(0);
    private static final AtomicLong readCounter = new AtomicLong(0);
    private static final AtomicLong createCounter = new AtomicLong(0);
    private static final AtomicLong updateCounter = new AtomicLong(0);
    private static final AtomicLong deleteCounter = new AtomicLong(0);
    private static final AtomicLong invoke1Counter = new AtomicLong(0);
    private static final AtomicLong invoke2Counter = new AtomicLong(0);

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
        final long readCount = readCounter.incrementAndGet();
        logger.info("1234567890 - read() count: {}", readCount);
        return repository.read(aClass, i, map);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T t, Map<String, String> map) {
        final long createCount = createCounter.incrementAndGet();
        logger.info("1234567890 - create() count: {}", createCount);
        return repository.create(t, map);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T t, Map<String, String> map) {
        final long updateCount = updateCounter.incrementAndGet();
        logger.info("1234567890 - update() count: {}", updateCount);
        return repository.update(t, map);
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> aClass, I i, Map<String, String> map) {
        final long deleteCount = deleteCounter.incrementAndGet();
        logger.info("1234567890 - delete() count: {}", deleteCount);
        return repository.delete(aClass, i, map);
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> aClass,
            Class<T> aClass1,
            Multimap<String, List<IQueryParameterType>> multimap,
            Map<String, String> map) {
        final long searchCount = searchCounter.incrementAndGet();
        logger.info(
                "1234567890 - search() count: {}, resource class: {}, search params: {}",
                searchCount,
                aClass1,
                multimap);
        return repository.search(aClass, aClass1, multimap, map);
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> aClass, String s, P p, Class<R> aClass1, Map<String, String> map) {
        final long invoke1Count = invoke1Counter.incrementAndGet();
        logger.info("1234567890 - invoke1() count: {}", invoke1Count);
        return repository.invoke(aClass, s, p, aClass1, map);
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I i, String s, P p, Class<R> aClass, Map<String, String> map) {
        final long invoke2Count = invoke2Counter.incrementAndGet();
        logger.info("1234567890 - invoke2() count: {}", invoke2Count);
        return repository.invoke(aClass, s, p, aClass, map);
    }

    @Override
    public <B extends IBaseBundle> B transaction(B bundle, Map<String, String> headers) {
        return repository.transaction(bundle, headers);
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url, Map<String, String> headers) {
        return repository.link(bundleType, url, headers);
    }

    @Override
    public FhirContext fhirContext() {
        return repository.fhirContext();
    }
}
