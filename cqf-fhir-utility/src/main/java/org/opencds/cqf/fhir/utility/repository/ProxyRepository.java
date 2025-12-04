package org.opencds.cqf.fhir.utility.repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.utility.KnowledgeArtifactUtil.RESOURCE_TYPES;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.google.common.collect.Multimap;
import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ProxyRepository implements IRepository {
    public static final Logger logger = LoggerFactory.getLogger(ProxyRepository.class);

    // One data server, one terminology server (content defaults to data)
    // One data server, one content server (terminology defaults to data)
    // One data server, one content server, one terminology server

    private final IRepository data;
    private final IRepository content;
    private final IRepository terminology;

    public ProxyRepository(
            IRepository local, Boolean useLocalData, IRepository data, IRepository content, IRepository terminology) {
        checkNotNull(local);

        if (data == null) {
            if (Boolean.TRUE.equals(useLocalData)) {
                this.data = local;
            } else {
                this.data = new InMemoryFhirRepository(local.fhirContext());
            }
        } else {
            if (Boolean.TRUE.equals(useLocalData)) {
                this.data = new FederatedRepository(local, data);
            } else {
                this.data = data;
            }
        }
        this.content = content == null ? local : content;
        this.terminology = terminology == null ? local : terminology;
    }

    public ProxyRepository(IRepository data, IRepository content, IRepository terminology) {
        checkNotNull(data);

        this.data = data;
        this.content = content == null ? this.data : content;
        this.terminology = terminology == null ? this.data : terminology;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {

        if (isTerminologyResource(resourceType.getSimpleName())) {
            return terminology.read(resourceType, id, headers);
        } else if (isContentResource(resourceType.getSimpleName())) {
            return content.read(resourceType, id, headers);
        } else {
            return data.read(resourceType, id, headers);
        }
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

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        if (isTerminologyResource(resourceType.getSimpleName())) {
            return terminology.search(bundleType, resourceType, searchParameters, headers);
        } else if (isContentResource(resourceType.getSimpleName())) {
            return content.search(bundleType, resourceType, searchParameters, headers);
        } else {
            return data.search(bundleType, resourceType, searchParameters, headers);
        }
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url, Map<String, String> headers) {
        return Stream.of(this.data, this.content, this.terminology)
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
        if (isTerminologyResource(resourceType.getSimpleName())) {
            return terminology.invoke(resourceType, name, parameters, returnType, headers);
        } else if (isContentResource(resourceType.getSimpleName())) {
            return content.invoke(resourceType, name, parameters, returnType, headers);
        } else {
            return data.invoke(resourceType, name, parameters, returnType, headers);
        }
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
        return data.fhirContext();
    }

    private static final Set<String> terminologyResourceSet =
            new HashSet<>(Arrays.asList("ValueSet", "CodeSystem", "ConceptMap"));

    private boolean isTerminologyResource(String type) {
        return (terminologyResourceSet.contains(type));
    }

    private boolean isContentResource(String type) {
        return (RESOURCE_TYPES.contains(type));
    }
}
