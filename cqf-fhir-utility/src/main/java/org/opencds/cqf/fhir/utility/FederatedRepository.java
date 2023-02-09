package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.hl7.fhir.instance.model.api.*;
import org.opencds.cqf.fhir.api.Repository;

import java.util.*;

public class FederatedRepository implements Repository {

    private Repository data;
    private Repository content;
    private Repository terminology;

    public FederatedRepository(Repository data, Repository content, Repository terminology) {
        this.data = data;
        this.content = content;
        this.terminology = terminology;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id, Map<String, String> headers) {
        if (isTerminologyResource(id.getResourceType())) {
            return terminology.read(resourceType, id, headers);
        } else if (isContentResource(id.getResourceType())) {
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
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType, I id, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType, Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters, Map<String, String> headers) {
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
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id, String name, P parameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(P parameters, Class<B> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
        return null;
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(I id, P parameters, Class<B> returnType, Map<String, String> headers) {
        return null;
    }

    private static Set<String> terminologyResourceSet = new HashSet<>(Arrays.asList("ValueSet", "CodeSystem", "ConceptMap"));

    private boolean isTerminologyResource(String type) {
        if (terminologyResourceSet.contains(type)) return true;
        return false;
    }

    private static Set<String> contentResourceSet = new HashSet<>(Arrays.asList("Library", "Measure", "PlanDefinition", "StructureDefinition"));

    private boolean isContentResource(String type) {
        if (contentResourceSet.contains(type)) return true;
        return false;
    }


}
