package org.opencds.cqf.fhir.utility.repository;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public class ProxyRepository implements IRepository {

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
        return null;
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
    public FhirContext fhirContext() {
        return data.fhirContext();
    }

    private static Set<String> terminologyResourceSet =
            new HashSet<>(Arrays.asList("ValueSet", "CodeSystem", "ConceptMap"));

    private boolean isTerminologyResource(String type) {
        return (terminologyResourceSet.contains(type));
    }

    private static Set<String> contentResourceSet = new HashSet<>(Arrays.asList(
            "Library", "Measure", "PlanDefinition", "StructureDefinition", "ActivityDefinition", "Questionnaire"));

    private boolean isContentResource(String type) {
        return (contentResourceSet.contains(type));
    }
}
