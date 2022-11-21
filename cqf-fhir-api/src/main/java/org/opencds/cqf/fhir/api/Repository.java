package org.opencds.cqf.fhir.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;

public interface Repository {

        @Nonnull
        default <T extends IBaseResource, I extends IIdType> T read(@Nonnull Class<T> resourceType, @Nonnull I id) {
                return this.read(resourceType, id, Collections.emptyMap());
        }

        @Nonnull
        <T extends IBaseResource, I extends IIdType> T read(@Nonnull Class<T> resourceType, @Nonnull I id,
                        @Nonnull Map<String, String> headers);

        @Nonnull
        default <T extends IBaseResource> MethodOutcome create(@Nonnull T resource) {
                return this.create(resource, Collections.emptyMap());
        }

        @Nonnull
        <T extends IBaseResource> MethodOutcome create(@Nonnull T resource, @Nonnull Map<String, String> headers);

        @Nonnull
        default <T extends IBaseResource> MethodOutcome update(@Nonnull T resource) {
                return this.update(resource, Collections.emptyMap());
        }

        @Nonnull
        <T extends IBaseResource> MethodOutcome update(@Nonnull T resource, @Nonnull Map<String, String> headers);

        default <T extends IBaseResource, I extends IIdType> MethodOutcome delete(@Nonnull Class<T> resourceType,
                        @Nonnull I id) {
                return this.delete(resourceType, id, Collections.emptyMap());
        }

        <T extends IBaseResource, I extends IIdType> MethodOutcome delete(@Nonnull Class<T> resourceType, @Nonnull I id,
                        @Nonnull Map<String, String> headers);

        @Nonnull
        // TODO: Support AND search maps...
        default <B extends IBaseBundle, T extends IBaseResource> B search(@Nonnull Class<T> resourceType,
                        @Nonnull Map<String, List<IQueryParameterType>> searchParameters) {
                return this.search(resourceType, searchParameters, Collections.emptyMap());
        }

        @Nonnull
        // TODO: Support AND search maps...
        <B extends IBaseBundle, T extends IBaseResource> B search(@Nonnull Class<T> resourceType,
                        @Nonnull Map<String, List<IQueryParameterType>> searchParameters,
                        @Nonnull Map<String, String> headers);

        @Nonnull
        default <C extends IBaseConformance> C capabilities(Class<C> resourceType) {
                return this.capabilities(resourceType, Collections.emptyMap());
        }

        @Nonnull
        <C extends IBaseConformance> C capabilities(Class<C> resourceType, @Nonnull Map<String, String> headers);

        @Nonnull
        default <B extends IBaseBundle> B transaction(@Nonnull B transaction) {
                return this.transaction(transaction, Collections.emptyMap());
        }

        @Nonnull
        <B extends IBaseBundle> B transaction(@Nonnull B transaction, @Nonnull Map<String, String> headers);

        @Nonnull
        default <R extends IBaseResource, P extends IBaseParameters> R invoke(@Nonnull String name,
                        @Nonnull P parameters,
                        @Nonnull Class<R> returnType) {
                return this.invoke(name, parameters, returnType, Collections.emptyMap());
        }

        @Nonnull
        <R extends IBaseResource, P extends IBaseParameters> R invoke(@Nonnull String name, @Nonnull P parameters,
                        @Nonnull Class<R> returnType,
                        @Nonnull Map<String, String> headers);

        @Nonnull
        default <P extends IBaseParameters> MethodOutcome invoke(@Nonnull String name, @Nonnull P parameters) {
                return this.invoke(name, parameters, Collections.emptyMap());
        }

        @Nonnull
        <P extends IBaseParameters> MethodOutcome invoke(@Nonnull String name, @Nonnull P parameters,
                        @Nonnull Map<String, String> headers);

        @Nonnull
        default <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
                        @Nonnull Class<T> resourceType,
                        @Nonnull String name, @Nonnull P parameters, @Nonnull Class<R> returnType) {
                return this.invoke(resourceType, name, parameters, returnType, Collections.emptyMap());
        }

        @Nonnull
        <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
                        @Nonnull Class<T> resourceType,
                        @Nonnull String name, @Nonnull P parameters, @Nonnull Class<R> returnType,
                        @Nonnull Map<String, String> headers);

        @Nonnull
        default <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
                        @Nonnull Class<T> resourceType, @Nonnull String name, @Nonnull P parameters) {
                return this.invoke(name, parameters, Collections.emptyMap());
        }

        @Nonnull
        <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(@Nonnull Class<T> resourceType,
                        @Nonnull String name, @Nonnull P parameters,
                        @Nonnull Map<String, String> headers);

        @Nonnull
        default <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(@Nonnull I id,
                        @Nonnull String name,
                        @Nonnull P parameters,
                        @Nonnull Class<R> returnType) {
                return this.invoke(id, name, parameters, returnType, Collections.emptyMap());
        }

        @Nonnull
        <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(@Nonnull I id,
                        @Nonnull String name,
                        @Nonnull P parameters, @Nonnull Class<R> returnType, @Nonnull Map<String, String> headers);

        @Nonnull
        default <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(
                        @Nonnull I id, @Nonnull String name, @Nonnull P parameters) {
                return this.invoke(name, parameters, Collections.emptyMap());
        }

        @Nonnull
        <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(@Nonnull I id,
                        @Nonnull String name, @Nonnull P parameters,
                        @Nonnull Map<String, String> headers);
}
