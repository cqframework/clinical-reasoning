package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleBuilder;
import com.google.common.collect.Multimap;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.iterable.BundleIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// wip
@SuppressWarnings("UnstableApiUsage")
public class FederatedRepository implements IRepository {
    public static final Logger logger = LoggerFactory.getLogger(FederatedRepository.class);

    private final IRepository local;
    private final List<IRepository> repositoryList;

    public FederatedRepository(IRepository local, IRepository... repositories) {
        this.local = local;
        repositoryList = Arrays.asList(repositories);
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {

        // Check local first, then go through the list if nothing is found
        T result = null;
        try {
            result = local.read(resourceType, id, headers);

        } catch (Exception e) {
            // TODO: handle exception
        }

        if (result == null) {
            for (var repository : repositoryList) {
                try {
                    result = repository.read(resourceType, id, headers);
                    if (result != null) {
                        break;
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }

        if (result == null) {
            throw new ResourceNotFoundException("No resource found with id: %s".formatted(id.getValue()));
        }

        return result;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        return null;
    }

    @Override
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(
            I id, P patchParameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private <B extends IBaseBundle, T extends IBaseResource> List<T> conductSearch(
            IRepository repository,
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        List<T> results = new ArrayList<>();
        var bundle = repository.search(bundleType, resourceType, searchParameters, headers);
        var iterator = new BundleIterator<>(repository, bundle);
        iterator.forEachRemaining(b -> results.add((T) b.getResource()));
        return results;
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        // Search all repositories and return combined results.
        var builder = new BundleBuilder(fhirContext());

        List<CompletableFuture<List<T>>> futureList = new ArrayList<>();
        futureList.add(CompletableFuture.supplyAsync(
                () -> conductSearch(local, bundleType, resourceType, searchParameters, headers)));
        this.repositoryList.forEach(r -> futureList.add(CompletableFuture.supplyAsync(
                () -> conductSearch(r, bundleType, resourceType, searchParameters, headers))));

        futureList.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .forEach(builder::addCollectionEntry);
        builder.setType("searchset");

        return builder.getBundleTyped();
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url, Map<String, String> headers) {
        return Stream.concat(Stream.of(local), repositoryList.stream())
                .map(repo -> tryLink(repo, bundleType, url, headers))
                .flatMap(Optional::stream)
                .filter(this::hasResourceEntries)
                .findFirst()
                .orElse(null);
    }

    private <B extends IBaseBundle> Optional<B> tryLink(
            IRepository repo, Class<B> type, String url, Map<String, String> headers) {
        try {
            return Optional.ofNullable(repo.link(type, url, headers));
        } catch (Exception e) {
            // swallow and try next
            logger.debug(
                    "Encountered error attempting to fetch link from repository of type {}: {}",
                    repo.getClass().getName(),
                    e.getMessage());
            return Optional.empty();
        }
    }

    private Boolean hasResourceEntries(IBaseBundle bundle) {
        var bundleFactory =
                FhirContext.forCached(bundle.getStructureFhirVersionEnum()).newBundleFactory();
        bundleFactory.initializeWithBundleResource(bundle);
        return !bundleFactory.toListOfResources().isEmpty();
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        return null;
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(
            String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
            Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(
            I id, String name, P parameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(
            P parameters, Class<B> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
            Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(
            I id, P parameters, Class<B> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    @Nonnull
    public FhirContext fhirContext() {
        return local.fhirContext();
    }
}
