package org.opencds.cqf.fhir.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;

public interface Repository {

        default <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id) {
                return this.read(resourceType, id, Collections.emptyMap());
        }

        <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
                        Map<String, String> headers);

        default <T extends IBaseResource> MethodOutcome create(T resource) {
                return this.create(resource, Collections.emptyMap());
        }

        <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers);

        default <T extends IBaseResource> MethodOutcome update(T resource) {
                return this.update(resource, Collections.emptyMap());
        }

        <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers);

        default <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType,
                        I id) {
                return this.delete(resourceType, id, Collections.emptyMap());
        }

        <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType, I id,
                        Map<String, String> headers);

        default <B extends IBaseBundle, T extends IBaseResource> B search(
                        Class<B> bundleType,
                        Class<T> resourceType,
                        Map<String, List<IQueryParameterType>> searchParameters) {
                return this.search(bundleType, resourceType, searchParameters, Collections.emptyMap());
        }

        <B extends IBaseBundle, T extends IBaseResource> B search(
                        Class<B> bundleType,
                        Class<T> resourceType,
                        Map<String, List<IQueryParameterType>> searchParameters,
                        Map<String, String> headers);

        default <C extends IBaseConformance> C capabilities(Class<C> resourceType) {
                return this.capabilities(resourceType, Collections.emptyMap());
        }

        <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers);

        default <B extends IBaseBundle> B transaction(B transaction) {
                return this.transaction(transaction, Collections.emptyMap());
        }

        <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers);

        default <R extends IBaseResource, P extends IBaseParameters> R invoke(String name,
                        P parameters,
                        Class<R> returnType) {
                return this.invoke(name, parameters, returnType, Collections.emptyMap());
        }

        <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters,
                        Class<R> returnType,
                        Map<String, String> headers);

        default <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters) {
                return this.invoke(name, parameters, Collections.emptyMap());
        }

        <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters,
                        Map<String, String> headers);

        default <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
                        Class<T> resourceType,
                        String name, P parameters, Class<R> returnType) {
                return this.invoke(resourceType, name, parameters, returnType, Collections.emptyMap());
        }

        <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
                        Class<T> resourceType,
                        String name, P parameters, Class<R> returnType,
                        Map<String, String> headers);

        default <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
                        Class<T> resourceType, String name, P parameters) {
                return this.invoke(name, parameters, Collections.emptyMap());
        }

        <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(Class<T> resourceType,
                        String name, P parameters,
                        Map<String, String> headers);

        default <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id,
                        String name,
                        P parameters,
                        Class<R> returnType) {
                return this.invoke(id, name, parameters, returnType, Collections.emptyMap());
        }

        <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id,
                        String name,
                        P parameters, Class<R> returnType, Map<String, String> headers);

        default <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(
                        I id, String name, P parameters) {
                return this.invoke(name, parameters, Collections.emptyMap());
        }

        <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id,
                        String name, P parameters,
                        Map<String, String> headers);
}