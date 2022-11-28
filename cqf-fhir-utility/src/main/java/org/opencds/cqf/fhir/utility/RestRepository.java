package org.opencds.cqf.fhir.utility;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

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
    @Nonnull
    public <T extends IBaseResource, I extends IIdType> T read(@Nonnull Class<T> resourceType, @Nonnull I id,
            @Nonnull Map<String, String> headers) {
        var op = this.client.read().resource(resourceType).withId(id);
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <T extends IBaseResource> MethodOutcome create(@Nonnull T resource, @Nonnull Map<String, String> headers) {
        var op = this.client.create().resource(resource);
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <T extends IBaseResource> MethodOutcome update(@Nonnull T resource, @Nonnull Map<String, String> headers) {
        var op = this.client.update().resource(resource).withId(resource.getIdElement());
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(@Nonnull Class<T> resourceType,
            @Nonnull I id,
            @Nonnull Map<String, String> headers) {
        var op = this.client.delete().resourceById(id);
        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <B extends IBaseBundle, T extends IBaseResource> B search(@Nonnull Class<T> resourceType,
            @Nonnull Map<String, List<IQueryParameterType>> searchParameters,
            @Nonnull Map<String, String> headers) {

        var op = this.client.search().forResource(resourceType).where(searchParameters);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return (B) op.execute();
    }

    @Override
    @Nonnull
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, @Nonnull Map<String, String> headers) {
        var op = this.client.capabilities().ofType(resourceType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <B extends IBaseBundle> B transaction(@Nonnull B transaction, @Nonnull Map<String, String> headers) {
        var op = this.client.transaction().withBundle(transaction);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(@Nonnull String name, @Nonnull P parameters,
            @Nonnull Class<R> returnType, @Nonnull Map<String, String> headers) {
        var op = this.client.operation().onServer().named(name).withParameters(parameters)
                .returnResourceType(returnType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <P extends IBaseParameters> MethodOutcome invoke(@Nonnull String name, @Nonnull P parameters,
            @Nonnull Map<String, String> headers) {
        var op = this.client.operation().onServer().named(name).withParameters(parameters).returnMethodOutcome();

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            @Nonnull Class<T> resourceType, @Nonnull String name, @Nonnull P parameters, @Nonnull Class<R> returnType,
            @Nonnull Map<String, String> headers) {
        var op = this.client.operation().onType(resourceType).named(name).withParameters(parameters)
                .returnResourceType(returnType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(@Nonnull Class<T> resourceType,
            @Nonnull String name, @Nonnull P parameters, @Nonnull Map<String, String> headers) {
        var op = this.client.operation().onType(resourceType).named(name).withParameters(parameters)
                .returnMethodOutcome();

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(@Nonnull I id,
            @Nonnull String name, @Nonnull P parameters, @Nonnull Class<R> returnType,
            @Nonnull Map<String, String> headers) {
        var op = this.client.operation().onInstance(id).named(name).withParameters(parameters)
                .returnResourceType(returnType);

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }

    @Override
    @Nonnull
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(@Nonnull I id, @Nonnull String name,
            @Nonnull P parameters, @Nonnull Map<String, String> headers) {
        var op = this.client.operation().onInstance(id).named(name).withParameters(parameters).returnMethodOutcome();

        for (var entry : headers.entrySet()) {
            op = op.withAdditionalHeader(entry.getKey(), entry.getValue());
        }

        return op.execute();
    }
}
