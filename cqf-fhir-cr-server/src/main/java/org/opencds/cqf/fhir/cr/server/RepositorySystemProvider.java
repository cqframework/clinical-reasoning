package org.opencds.cqf.fhir.cr.server;

import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseBundle;

/**
 * System-level provider (no resource type) that handles {@code POST /} bundle transactions and
 * batches by delegating to {@link ca.uhn.fhir.repository.IRepository#transaction}.
 */
@SuppressWarnings("UnstableApiUsage")
public class RepositorySystemProvider {

    private final IRepositoryFactory repositoryFactory;

    public RepositorySystemProvider(IRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    @Transaction
    public IBaseBundle transaction(@TransactionParam IBaseBundle bundle, RequestDetails requestDetails) {
        return repositoryFactory
                .create(requestDetails)
                .transaction(bundle, RepositoryResourceProvider.headersOf(requestDetails));
    }
}
