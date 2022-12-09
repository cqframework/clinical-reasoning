package org.opencds.cqf.cql.evaluator.measure.r4;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.hl7.fhir.instance.model.api.*;
import org.opencds.cqf.fhir.api.Repository;

import java.util.List;
import java.util.Map;

public class R4TestRepository implements Repository {



    @Override
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
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
}
