package org.opencds.cqf.cql.evaluator.fhir.repository;

import ca.uhn.fhir.util.BundleUtil;
import org.apache.commons.lang3.NotImplementedException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.hl7.fhir.instance.model.api.*;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.Resources;
import org.opencds.cqf.fhir.api.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BundleFhirRepository implements Repository {
    protected FhirContext context;
    protected IBaseBundle bundle;
    protected IFhirPath fhirPath;

    public BundleFhirRepository(FhirContext context, IBaseBundle bundle) {
        this.context = context;
        this.bundle = bundle;
        this.fhirPath = FhirPathCache.cachedForContext(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id, Map<String, String> headers) {
        List<IBaseResource> resources = (List<IBaseResource>) BundleUtil.toListOfResourcesOfType(this.context,
                this.bundle,
                this.context.getResourceDefinition(id.getResourceType()).getImplementingClass());

        for (IBaseResource resource : resources) {
            if (resource.getIdElement().getIdPart().equals(id.getIdPart())) {
                return (T)resource;
            }
        }
        return null;
    }

    //wip: search
    @Override
    @SuppressWarnings("unchecked")
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        IBaseResource bundle = Resources.newResource(IBaseBundle.class, UUID.randomUUID().toString());
        //HashMap<String, List<List<IQueryParameterType>>>
        Iterable<IBaseResource> iterable = (Iterable<IBaseResource>) BundleUtil.toListOfResourcesOfType(this.context, this.bundle,
                this.context.getResourceDefinition(resourceType).getImplementingClass());



        return (B) bundle;
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType, I id, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(I id, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }
}
