package org.opencds.cqf.fhir.cr.ecr.r4;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import java.util.ArrayList;
import java.util.List;

import org.opencds.cqf.fhir.cr.ecr.FhirResourceExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class R4ERSDTransformService {
    private static final Logger logger = LoggerFactory.getLogger(R4ERSDTransformService.class);
    private final IRepository repository;

    public R4ERSDTransformService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Implements the $ersd-v2-import operation which loads an active (released)
     * eCR Version 2.1.1 (http://hl7.org/fhir/us/ecr/ImplementationGuide/hl7.fhir.us.ecr|2.1.1) conformant
     * eRSD Bundle
     * and transforms it into and Value Set Manager authoring state
     *
     * @param maybeBundle         the v2 bundle to import
     * @return the OperationOutcome
     */
    public OperationOutcome eRSDV2ImportOperation(
        IBaseResource maybeBundle,
        String appAuthoritativeUrl
    ) throws UnprocessableEntityException, FhirResourceExistsException {

        if (appAuthoritativeUrl == null) {
            throw new UnprocessableEntityException("appAuthoritativeUrl is missing, e.g. http://example.com/fhir");
        } else if (maybeBundle == null) {
            throw new UnprocessableEntityException("Resource is missing");
        } else if (!(maybeBundle instanceof IBaseBundle)) {
            throw new UnprocessableEntityException("Resource is not a bundle");
        }

        Bundle v2Bundle = (Bundle) maybeBundle;
        List<Bundle.BundleEntryComponent> importTxBundleEntries =
            R4ImportBundleProducer.transformImportBundle(v2Bundle, this.repository, appAuthoritativeUrl);

        List<List<Bundle.BundleEntryComponent>> subLists = splitList(importTxBundleEntries, 74);
        long startTime = System.nanoTime();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < subLists.size(); i++) {
            logger.info("Processing sublist {}", i);
            Bundle txBundle = this.createTransactionBundle(subLists.get(i));

            final int sublistIndex = i;
            CompletableFuture<Void> future = CompletableFuture
                .runAsync(() -> this.repository.transaction(txBundle))
                .exceptionally(ex -> {
                    logger.error("Transaction failed for sublist {}", sublistIndex, ex);
                    return null;
                });

            futures.add(future);
        }

        // Combine all futures and block until they finish
        CompletableFuture<Void> allFutures =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        allFutures.join(); // <-- waits for all to complete

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000L;
        logger.info("All tasks completed in {} milliseconds.", duration);

        // Now return AFTER all work is finished
        OperationOutcome response = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
        issue.setCode(OperationOutcome.IssueType.PROCESSING);
        issue.setDiagnostics("Import completed in " + duration + " ms");
        response.addIssue(issue);

        return response;
    }


    public static <T> List<List<T>> splitList(List<T> list, int chunkSize) {
        return IntStream.range(0, (list.size() + chunkSize - 1) / chunkSize)
            .mapToObj(i -> list.subList(i * chunkSize, Math.min((i + 1) * chunkSize, list.size())))
            .collect(Collectors.toList());
    }

    private Bundle createTransactionBundle(List<BundleEntryComponent> bundleEntry) {
        Bundle importBundle = new Bundle();
        importBundle.setType(Bundle.BundleType.TRANSACTION);
        importBundle.setEntry(bundleEntry);
        return importBundle;
    }
}