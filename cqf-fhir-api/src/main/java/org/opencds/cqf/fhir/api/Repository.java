package org.opencds.cqf.fhir.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.google.common.annotations.Beta;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;

/**
 * <p>
 * This interface is a Java rendition of the FHIR REST API. All FHIR operations
 * are defined at the HTTP level, which is convenient from the specification
 * point-of-view since FHIR is built on top of web standards. This does mean
 * that a few HTTP specific considerations, such as transmitting side-band
 * information through the HTTP headers, bleeds into this API.
 * </p>
 *
 * <p>
 * One particularly odd case are FHIR Bundle links. The specification describes
 * these as opaque to the end-user, so a given FHIR repository implementation
 * must be able to resolve those directly. See
 * {@link Repository#link(Class, String)}
 * </p>
 *
 * <p>
 * This interface also chooses to ignore return headers for most cases,
 * preferring to return the Java objects directly. In cases where this is not
 * possible, or the additional headers are crucial information, the HAPI's
 * {@link ca.uhn.fhir.rest.api.MethodOutcome} is used.
 * </p>
 *
 * <p>
 * Implementations of this interface should prefer to throw the exceptions
 * located in the {@link ca.uhn.fhir.rest.server.exceptions} package.
 * </p>
 *
 * <p>
 * If a given operations is not supported, implementations should throw an
 * UnsupportedOperationException. The capabilities operation, if
 * supported,
 * should return the set of supported interactions. If capabilities is not
 * supported, the components in this repository will try to invoke operations
 * with "sensible" defaults. For example, by using the standard FHIR search
 * parameters. Discussion is on-going to determine what a "sensible" minimal
 * level of support for interactions should be.
 * </p>
 *
 * <p>
 * This API is under-going active development with contributors from multiple
 * different organizations, so it should be considered beta-level.
 * </p>
 *
 * @see <a href="https://www.hl7.org/fhir/http.html">FHIR REST API</a>
 */
@Beta
public interface Repository {

        // CRUD starts here
        default <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id) {
                return this.read(resourceType, id, Collections.emptyMap());
        }

        <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
                        Map<String, String> headers);

        default <T extends IBaseResource> MethodOutcome create(T resource) {
                return this.create(resource, Collections.emptyMap());
        }

        <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers);

        default <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters) {
                return this.patch(id, patchParameters, Collections.emptyMap());
        }

        <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters,
                        Map<String, String> headers);

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

        // Querying starts here
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

        // Paging starts here
        default <B extends IBaseBundle> B link(
                        Class<B> bundleType,
                        String url) {
                return this.link(bundleType, url, Collections.emptyMap());
        }

        <B extends IBaseBundle> B link(
                        Class<B> bundleType,
                        String url,
                        Map<String, String> headers);

        // Metadata starts here
        default <C extends IBaseConformance> C capabilities(Class<C> resourceType) {
                return this.capabilities(resourceType, Collections.emptyMap());
        }

        <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers);

        // Transactions starts here
        default <B extends IBaseBundle> B transaction(B transaction) {
                return this.transaction(transaction, Collections.emptyMap());
        }

        <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers);

        // Operations starts here
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

        // History starts here
        default <B extends IBaseBundle, P extends IBaseParameters> B history(
                        P parameters,
                        Class<B> returnType) {
                return this.history(parameters, returnType, Collections.emptyMap());
        }

        <B extends IBaseBundle, P extends IBaseParameters> B history(P parameters,
                        Class<B> returnType,
                        Map<String, String> headers);

        default <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
                        Class<T> resourceType, P parameters, Class<B> returnType) {
                return this.history(resourceType, parameters, returnType, Collections.emptyMap());
        }

        <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
                        Class<T> resourceType, P parameters, Class<B> returnType,
                        Map<String, String> headers);

        default <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(
                        I id,
                        P parameters,
                        Class<B> returnType) {
                return this.history(id, parameters, returnType, Collections.emptyMap());
        }

        <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(I id,
                        P parameters, Class<B> returnType, Map<String, String> headers);
}