package org.opencds.cqf.fhir.utility;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class RestRepository implements Repository {

    public RestRepository(IGenericClient client) {
        this.client = client;
    }

    private IGenericClient client;

    protected IGenericClient getClient() {
        return this.client;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
            Map<String, String> headers) {
        var op = this.client.read().resource(resourceType).withId(id);
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        var op = this.client.create().resource(resource);
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        var op = this.client.update().resource(resource).withId(resource.getIdElement());
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType,
            I id,
            Map<String, String> headers) {
        var op = this.client.delete().resourceById(id);
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {

        var op = this.client.search().forResource(resourceType).where(searchParameters).returnBundle(bundleType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers) {
        var op = this.client.capabilities().ofType(resourceType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        var op = this.client.transaction().withBundle(transaction);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters,
            Class<R> returnType, Map<String, String> headers) {
        var op = this.client.operation().onServer().named(name).withParameters(parameters)
                .returnResourceType(returnType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters,
            Map<String, String> headers) {
        var op = this.client.operation().onServer().named(name).withParameters(parameters).returnMethodOutcome();

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType,
            Map<String, String> headers) {
        var op = this.client.operation().onType(resourceType).named(name).withParameters(parameters)
                .returnResourceType(returnType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(Class<T> resourceType,
            String name, P parameters, Map<String, String> headers) {
        var op = this.client.operation().onType(resourceType).named(name).withParameters(parameters)
                .returnMethodOutcome();

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id,
            String name, P parameters, Class<R> returnType,
            Map<String, String> headers) {
        var op = this.client.operation().onInstance(id).named(name).withParameters(parameters)
                .returnResourceType(returnType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id, String name,
            P parameters, Map<String, String> headers) {
        var op = this.client.operation().onInstance(id).named(name).withParameters(parameters).returnMethodOutcome();

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }
}
