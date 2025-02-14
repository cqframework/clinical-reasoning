/*-
 * #%L
 * HAPI FHIR - Clinical Reasoning
 * %%
 * Copyright (C) 2014 - 2024 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.cr;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * Simulate FhirDal operations until that's fully baked. This interface is
 * trying to be small and have a reasonable path forwards towards
 * model independence in the future. The method overloads with "RequestDetails"
 * will eventually go away once we're able to make that a
 * cross-cutting concern. There are some ramifications to not using the
 * "RequestDetails" such as not firing hooks on the server, so the
 * overloads with that parameter should generally be preferred for the
 * short-term.
 */
public interface IDaoRegistryUser {

    DaoRegistry getDaoRegistry();

    default FhirContext getFhirContext() {
        return getDaoRegistry().getSystemDao().getContext();
    }

    /**
     * Get the class of the given Resource. FHIR version aware. For example, if the
     * server is running in DSTU3 mode
     * this will return the DSTU3 Library class when invoked with "Library".
     *
     * @param <T>             the type of resource to return
     * @param resourceName the name of the Resource to get the class for
     * @return the class of the resource
     */
    @SuppressWarnings("unchecked")
    default <T extends IBaseResource> Class<T> getClass(String resourceName) {
        return (Class<T>) getFhirContext().getResourceDefinition(resourceName).getImplementingClass();
    }

    /**
     * Reads a Resource with the given Id from the local server. Throws an error if
     * the Resource is not present
     * <p>
     * NOTE: Use {@code search} if a null result is preferred over an error.
     *
     * @param <T>   the Resource type to read
     * @param id the id to read
     * @return the FHIR Resource
     * @throws ResourceNotFoundException if the Id is not known
     */
    default <T extends IBaseResource> T read(IIdType id) {
        checkNotNull(id);

        return read(id, new SystemRequestDetails());
    }

    /**
     * Reads a Resource with the given Id from the local server. Throws an error if
     * the Resource is not present
     * <p>
     * NOTE: Use {@code search} if a null result is preferred over an error.
     *
     * @param <T>            the Resource type to read
     * @param id          the id to read
     * @param requestDetails multi-tenancy information
     * @return the FHIR Resource
     * @throws ResourceNotFoundException if the Id is not known
     */
    @SuppressWarnings("unchecked")
    default <T extends IBaseResource> T read(IIdType id, RequestDetails requestDetails) {
        checkNotNull(id);

        return (T) getDaoRegistry().getResourceDao(id.getResourceType()).read(id, requestDetails);
    }

    /**
     * Creates the given Resource on the local server
     *
     * @param <T>         The Resource type
     * @param resource the resource to create
     * @return the outcome of the creation
     */
    default <T extends IBaseResource> DaoMethodOutcome create(T resource) {
        checkNotNull(resource);

        return create(resource, new SystemRequestDetails());
    }

    /**
     * Creates the given Resource on the local server
     *
     * @param <T>            The Resource type
     * @param resource    the resource to create
     * @param requestDetails multi-tenancy information
     * @return the outcome of the creation
     */
    @SuppressWarnings("unchecked")
    default <T extends IBaseResource> DaoMethodOutcome create(T resource, RequestDetails requestDetails) {
        checkNotNull(resource);

        return ((IFhirResourceDao<T>) getDaoRegistry().getResourceDao(resource.fhirType()))
                .create(resource, requestDetails);
    }

    /**
     * Updates the given Resource on the local server
     *
     * @param <T>         The Resource type
     * @param resource the resource to update
     * @return the outcome of the creation
     */
    default <T extends IBaseResource> DaoMethodOutcome update(T resource) {
        checkNotNull(resource);

        return update(resource, new SystemRequestDetails());
    }

    /**
     * Updates the given Resource on the local server
     *
     * @param <T>            The Resource type
     * @param resource    the resource to update
     * @param requestDetails multi-tenancy information
     * @return the outcome of the creation
     */
    @SuppressWarnings("unchecked")
    default <T extends IBaseResource> DaoMethodOutcome update(T resource, RequestDetails requestDetails) {
        checkNotNull(resource);

        return ((IFhirResourceDao<T>) getDaoRegistry().getResourceDao(resource.fhirType()))
                .update(resource, requestDetails);
    }

    /**
     * Deletes the Resource with the given Id from the local server
     *
     * @param idType the Id of the Resource to delete.
     * @return the outcome of the deletion
     */
    default DaoMethodOutcome delete(IIdType idType) {
        checkNotNull(idType);

        return delete(idType, new SystemRequestDetails());
    }

    /**
     * Deletes the Resource with the given Id from the local server
     *
     * @param idType      the Id of the Resource to delete.
     * @param requestDetails multi-tenancy information
     * @return the outcome of the deletion
     */
    default DaoMethodOutcome delete(IIdType idType, RequestDetails requestDetails) {
        checkNotNull(idType);

        return getDaoRegistry().getResourceDao(idType.getResourceType()).delete(idType, requestDetails);
    }

    /**
     * NOTE: This is untested as of the time I'm writing this so it may need to be
     * reworked.
     * Executes a given transaction Bundle on the local server
     *
     * @param <T>            the type of Bundle
     * @param transaction the transaction to process
     * @return the transaction outcome
     */
    default <T extends IBaseBundle> T transaction(T transaction) {
        checkNotNull(transaction);

        return transaction(transaction, new SystemRequestDetails());
    }

    /**
     * NOTE: This is untested as of the time I'm writing this so it may need to be
     * reworked.
     * Executes a given transaction Bundle on the local server
     *
     * @param <T>               the type of Bundle
     * @param transaction    the transaction to process
     * @param requestDetails multi-tenancy information
     * @return the transaction outcome
     */
    @SuppressWarnings("unchecked")
    default <T extends IBaseBundle> T transaction(T transaction, RequestDetails requestDetails) {
        checkNotNull(transaction);

        return (T) getDaoRegistry().getSystemDao().transaction(requestDetails, transaction);
    }
}
