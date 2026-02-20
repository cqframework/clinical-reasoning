package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IPublishProcessor;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy handler for persisting ImplementationGuide dependencies using the $publish operation.
 *
 * <p>Implements adaptive strategies based on bundle size:
 * <ul>
 *   <li>Transaction approach: For bundles < 500 resources (default)</li>
 *   <li>Batch approach: For bundles >= 500 resources (fallback)</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Configurable size threshold for strategy selection</li>
 *   <li>Automatic retry logic (default: 3 attempts)</li>
 *   <li>Rollback on failure to maintain server consistency</li>
 *   <li>Memory-efficient batching for large dependency sets</li>
 * </ul>
 */
public class PublishStrategy {
    private static final Logger logger = LoggerFactory.getLogger(PublishStrategy.class);

    private static final int DEFAULT_BATCH_SIZE_THRESHOLD = 500;
    private static final int DEFAULT_BATCH_SIZE = 500;
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final IRepository repository;
    private final IPublishProcessor publishProcessor;
    private final FhirContext fhirContext;

    private final int batchSizeThreshold;
    private final int batchSize;
    private final int maxRetries;

    /**
     * Creates a PublishStrategy with default configuration.
     *
     * @param repository Repository for data access
     * @param publishProcessor Processor for $publish operation
     */
    public PublishStrategy(IRepository repository, IPublishProcessor publishProcessor) {
        this(repository, publishProcessor, DEFAULT_BATCH_SIZE_THRESHOLD, DEFAULT_BATCH_SIZE, DEFAULT_MAX_RETRIES);
    }

    /**
     * Creates a PublishStrategy with custom configuration.
     *
     * @param repository Repository for data access
     * @param publishProcessor Processor for $publish operation
     * @param batchSizeThreshold Threshold for switching to batch strategy
     * @param batchSize Size of each batch when using batch strategy
     * @param maxRetries Maximum number of retry attempts
     */
    public PublishStrategy(
            IRepository repository,
            IPublishProcessor publishProcessor,
            int batchSizeThreshold,
            int batchSize,
            int maxRetries) {
        this.repository = repository;
        this.publishProcessor = publishProcessor;
        this.fhirContext = repository.fhirContext();
        this.batchSizeThreshold = batchSizeThreshold;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    /**
     * Persists ImplementationGuide and its dependencies using appropriate strategy.
     *
     * @param implementationGuide The ImplementationGuide resource (must be first)
     * @param dependencies List of dependent resources to persist
     * @return Result containing success/failure status and any error messages
     * @throws PublishException if persistence fails after all retries
     */
    public PublishResult publish(IBaseResource implementationGuide, List<IBaseResource> dependencies)
            throws PublishException {
        int totalResources = 1 + (dependencies != null ? dependencies.size() : 0);

        logger.info(
                "Publishing ImplementationGuide with {} dependencies (total: {} resources)",
                dependencies != null ? dependencies.size() : 0,
                totalResources);

        // Select strategy based on size
        if (totalResources < batchSizeThreshold) {
            return publishWithTransactionStrategy(implementationGuide, dependencies);
        } else {
            logger.info(
                    "Resource count ({}) exceeds threshold ({}), using batch strategy",
                    totalResources,
                    batchSizeThreshold);
            return publishWithBatchStrategy(implementationGuide, dependencies);
        }
    }

    /**
     * Publishes using single transaction bundle (all-or-nothing).
     */
    private PublishResult publishWithTransactionStrategy(
            IBaseResource implementationGuide, List<IBaseResource> dependencies) throws PublishException {
        logger.debug("Using transaction strategy for persistence");

        IBaseBundle bundle = createTransactionBundle(implementationGuide, dependencies);
        List<String> persistedIds = new ArrayList<>();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("Transaction attempt {} of {}", attempt, maxRetries);
                IBaseBundle response = publishProcessor.publishBundle(bundle);

                // Extract persisted resource IDs from response for potential rollback
                persistedIds = extractPersistedIds(response);

                logger.info("Successfully persisted {} resources via transaction", persistedIds.size());
                return PublishResult.success(persistedIds.size());

            } catch (BaseServerResponseException e) {
                logger.warn("Transaction attempt {} failed: {}", attempt, e.getMessage());

                if (attempt == maxRetries) {
                    logger.error("Transaction failed after {} attempts, initiating rollback", maxRetries);
                    rollback(persistedIds);
                    throw new PublishException(
                            "Failed to publish ImplementationGuide dependencies after " + maxRetries + " attempts: "
                                    + e.getMessage(),
                            e);
                }

                // Exponential backoff before retry
                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PublishException("Interrupted during retry backoff", ie);
                }
            }
        }

        throw new PublishException("Unexpected error: exceeded retry limit without success or exception");
    }

    /**
     * Publishes using multiple batched transactions (progressive persistence).
     */
    private PublishResult publishWithBatchStrategy(IBaseResource implementationGuide, List<IBaseResource> dependencies)
            throws PublishException {
        logger.debug("Using batch strategy for persistence");

        List<String> persistedIds = new ArrayList<>();
        int totalPersisted = 0;

        try {
            // First batch: IG + immediate dependencies (must be first per CRMIPublishableBundle)
            IBaseBundle primaryBundle = createTransactionBundle(implementationGuide, null);
            IBaseBundle primaryResponse = publishWithRetry(primaryBundle);
            List<String> primaryIds = extractPersistedIds(primaryResponse);
            persistedIds.addAll(primaryIds);
            totalPersisted += primaryIds.size();

            logger.info("Persisted primary bundle with {} resources", primaryIds.size());

            // Subsequent batches: remaining dependencies
            if (dependencies != null && !dependencies.isEmpty()) {
                List<List<IBaseResource>> batches = createBatches(dependencies, batchSize);
                logger.info("Processing {} batches of remaining dependencies", batches.size());

                for (int i = 0; i < batches.size(); i++) {
                    logger.debug("Processing batch {} of {}", i + 1, batches.size());

                    IBaseBundle batchBundle = createBatchBundle(batches.get(i));
                    IBaseBundle batchResponse = publishWithRetry(batchBundle);

                    List<String> batchIds = extractPersistedIds(batchResponse);
                    persistedIds.addAll(batchIds);
                    totalPersisted += batchIds.size();

                    logger.debug("Batch {} persisted {} resources", i + 1, batchIds.size());
                }
            }

            logger.info("Successfully persisted {} resources via batch strategy", totalPersisted);
            return PublishResult.success(totalPersisted);

        } catch (Exception e) {
            logger.error("Batch strategy failed, initiating rollback of {} resources", persistedIds.size());
            rollback(persistedIds);
            throw new PublishException("Failed to publish dependencies using batch strategy: " + e.getMessage(), e);
        }
    }

    /**
     * Publishes a bundle with retry logic.
     */
    private IBaseBundle publishWithRetry(IBaseBundle bundle) throws PublishException {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return publishProcessor.publishBundle(bundle);
            } catch (BaseServerResponseException e) {
                logger.warn("Publish attempt {} failed: {}", attempt, e.getMessage());

                if (attempt == maxRetries) {
                    throw new PublishException("Failed after " + maxRetries + " attempts", e);
                }

                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PublishException("Interrupted during retry", ie);
                }
            }
        }
        throw new PublishException("Unexpected retry failure");
    }

    /**
     * Creates a transaction bundle with IG as first entry.
     */
    private IBaseBundle createTransactionBundle(IBaseResource implementationGuide, List<IBaseResource> resources) {
        IBaseBundle bundle = BundleHelper.newBundle(fhirContext.getVersion().getVersion(), null, "transaction");

        // Add IG as first entry (required by CRMIPublishableBundle profile)
        addResourceToBundle(bundle, implementationGuide);

        // Add dependencies
        if (resources != null) {
            for (IBaseResource resource : resources) {
                addResourceToBundle(bundle, resource);
            }
        }

        return bundle;
    }

    /**
     * Creates a transaction bundle for batched resources.
     * Note: Even though this is called "batch" strategy, we still use transaction bundles
     * to comply with CRMIPublishableBundle profile requirements.
     */
    private IBaseBundle createBatchBundle(List<IBaseResource> resources) {
        IBaseBundle bundle = BundleHelper.newBundle(fhirContext.getVersion().getVersion(), null, "transaction");

        for (IBaseResource resource : resources) {
            addResourceToBundle(bundle, resource);
        }

        return bundle;
    }

    /**
     * Adds a resource to a bundle with request information.
     */
    private void addResourceToBundle(IBaseBundle bundle, IBaseResource resource) {
        var fhirVersion = fhirContext.getVersion().getVersion();
        String url = resource.fhirType() + "/"
                + (resource.getIdElement().hasIdPart()
                        ? resource.getIdElement().getIdPart()
                        : resource.getIdElement().getValueAsString());

        switch (fhirVersion) {
            case DSTU3:
                var dstu3Bundle = (org.hl7.fhir.dstu3.model.Bundle) bundle;
                var dstu3Entry = dstu3Bundle.addEntry();
                dstu3Entry.setResource((org.hl7.fhir.dstu3.model.Resource) resource);
                dstu3Entry
                        .getRequest()
                        .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.PUT)
                        .setUrl(url);
                break;
            case R4:
                var r4Bundle = (org.hl7.fhir.r4.model.Bundle) bundle;
                var r4Entry = r4Bundle.addEntry();
                r4Entry.setResource((org.hl7.fhir.r4.model.Resource) resource);
                r4Entry.getRequest()
                        .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT)
                        .setUrl(url);
                break;
            case R5:
                var r5Bundle = (org.hl7.fhir.r5.model.Bundle) bundle;
                var r5Entry = r5Bundle.addEntry();
                r5Entry.setResource((org.hl7.fhir.r5.model.Resource) resource);
                r5Entry.getRequest()
                        .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.PUT)
                        .setUrl(url);
                break;
            default:
                throw new IllegalStateException("Unsupported FHIR version: " + fhirVersion);
        }
    }

    /**
     * Splits resources into batches of specified size.
     */
    private List<List<IBaseResource>> createBatches(List<IBaseResource> resources, int batchSize) {
        List<List<IBaseResource>> batches = new ArrayList<>();
        for (int i = 0; i < resources.size(); i += batchSize) {
            batches.add(resources.subList(i, Math.min(i + batchSize, resources.size())));
        }
        return batches;
    }

    /**
     * Extracts persisted resource IDs from transaction/batch response.
     */
    private List<String> extractPersistedIds(IBaseBundle response) {
        List<String> ids = new ArrayList<>();
        List<IBaseResource> resources = BundleHelper.getEntryResources(response);

        for (IBaseResource resource : resources) {
            if (resource.getIdElement().hasIdPart()) {
                ids.add(resource.fhirType() + "/" + resource.getIdElement().getIdPart());
            }
        }

        return ids;
    }

    /**
     * Rolls back persisted resources to maintain server consistency.
     */
    private void rollback(List<String> persistedIds) {
        if (persistedIds.isEmpty()) {
            logger.debug("No resources to rollback");
            return;
        }

        logger.warn("Rolling back {} persisted resources", persistedIds.size());

        int successCount = 0;
        int failureCount = 0;

        for (String id : persistedIds) {
            try {
                deleteResource(id);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to rollback resource {}: {}", id, e.getMessage());
                failureCount++;
            }
        }

        logger.warn("Rollback completed: {} succeeded, {} failed", successCount, failureCount);

        if (failureCount > 0) {
            logger.error(
                    "CRITICAL: Rollback incomplete - {} resources may be in inconsistent state. "
                            + "Manual intervention may be required.",
                    failureCount);
        }
    }

    /**
     * Deletes a resource by ID string (format: "ResourceType/id").
     */
    private void deleteResource(String id) {
        String[] parts = id.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid resource ID format: " + id);
        }

        String resourceType = parts[0];
        String resourceId = parts[1];

        var fhirVersion = fhirContext.getVersion().getVersion();

        switch (fhirVersion) {
            case DSTU3:
                Class<? extends org.hl7.fhir.dstu3.model.Resource> dstu3Class = getResourceClass(resourceType);
                repository.delete(dstu3Class, new org.hl7.fhir.dstu3.model.IdType(resourceId));
                break;
            case R4:
                Class<? extends org.hl7.fhir.r4.model.Resource> r4Class = getResourceClass(resourceType);
                repository.delete(r4Class, new org.hl7.fhir.r4.model.IdType(resourceId));
                break;
            case R5:
                Class<? extends org.hl7.fhir.r5.model.Resource> r5Class = getResourceClass(resourceType);
                repository.delete(r5Class, new org.hl7.fhir.r5.model.IdType(resourceId));
                break;
            default:
                throw new IllegalStateException("Unsupported FHIR version: " + fhirVersion);
        }
    }

    /**
     * Gets the resource class for a given resource type name.
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> getResourceClass(String resourceType) {
        try {
            var fhirVersion = fhirContext.getVersion().getVersion();
            String packageName =
                    switch (fhirVersion) {
                        case DSTU3 -> "org.hl7.fhir.dstu3.model";
                        case R4 -> "org.hl7.fhir.r4.model";
                        case R5 -> "org.hl7.fhir.r5.model";
                        default -> throw new IllegalStateException("Unsupported FHIR version: " + fhirVersion);
                    };

            return (Class<T>) Class.forName(packageName + "." + resourceType);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown resource type: " + resourceType, e);
        }
    }

    /**
     * Result of a publish operation.
     */
    public static class PublishResult {
        private final boolean success;
        private final int resourceCount;
        private final String errorMessage;

        private PublishResult(boolean success, int resourceCount, String errorMessage) {
            this.success = success;
            this.resourceCount = resourceCount;
            this.errorMessage = errorMessage;
        }

        public static PublishResult success(int resourceCount) {
            return new PublishResult(true, resourceCount, null);
        }

        public static PublishResult failure(String errorMessage) {
            return new PublishResult(false, 0, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getResourceCount() {
            return resourceCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Exception thrown when publish operation fails.
     */
    public static class PublishException extends Exception {
        public PublishException(String message) {
            super(message);
        }

        public PublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
