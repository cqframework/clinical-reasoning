package org.opencds.cqf.fhir.utility.client;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandRunner implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ExpandRunner.class);

    private int expansionAttempt = 0;
    private IBaseResource expandedValueSet;
    private final IGenericClient fhirClient;
    private final String valueSetUrl;
    private final IBaseParameters parameters;

    private final TerminologyServerClientSettings terminologyServerClientSettings;
    private final ScheduledExecutorService scheduler;

    public ExpandRunner(
            IGenericClient client,
            TerminologyServerClientSettings terminologyServerClientSettings,
            String valueSetUrl,
            IBaseParameters parameters) {
        this(client, terminologyServerClientSettings, valueSetUrl, parameters, null);
    }

    public ExpandRunner(
            IGenericClient fhirClient,
            TerminologyServerClientSettings terminologyServerClientSettings,
            String valueSetUrl,
            IBaseParameters parameters,
            ScheduledExecutorService scheduler) {
        this.fhirClient = requireNonNull(fhirClient);
        this.terminologyServerClientSettings = requireNonNull(terminologyServerClientSettings);
        this.valueSetUrl = requireNonNull(valueSetUrl);
        this.parameters = parameters;
        this.scheduler = scheduler != null ? scheduler : Executors.newScheduledThreadPool(1);
    }

    public IBaseResource expandValueSet() {
        var result = scheduler.schedule(this, 0, TimeUnit.SECONDS);
        try {
            result.get();
            if (scheduler.awaitTermination(terminologyServerClientSettings.getTimeoutSeconds(), TimeUnit.SECONDS)) {
                if (result.isDone() && expandedValueSet != null) {
                    return expandedValueSet;
                } else {
                    throw new UnprocessableEntityException(
                            "Terminology Server expansion failed for ValueSet (%s) - Server could not process expansion requests."
                                    .formatted(valueSetUrl));
                }
            } else {
                throw new UnprocessableEntityException(
                        "Terminology Server expansion took longer than the allotted timeout: %s"
                                .formatted(terminologyServerClientSettings.getTimeoutSeconds()));
            }
        } catch (Exception e) {
            scheduler.shutdown();
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new TerminologyServerExpansionException(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            expansionAttempt++;
            if (expansionAttempt <= terminologyServerClientSettings.getMaxRetryCount()) {
                logger.info("Expansion attempt: {} for ValueSet: {}", expansionAttempt, valueSetUrl);
                var id = Canonicals.getResourceType(valueSetUrl) + "/" + Canonicals.getIdPart(valueSetUrl);
                expandedValueSet = fhirClient
                        .operation()
                        .onInstance(id)
                        .named("$expand")
                        .withNoParameters(parameters.getClass())
                        .returnResourceType(getValueSetClass())
                        .execute();
                scheduler.shutdown();
            }
        } catch (Exception ex) {
            logger.info("Expansion attempt {} failed: {}", expansionAttempt, ex.getMessage());
            if (expansionAttempt < terminologyServerClientSettings.getMaxRetryCount()) {
                scheduler.schedule(
                        this,
                        terminologyServerClientSettings.getRetryIntervalMillis() * expansionAttempt,
                        TimeUnit.MILLISECONDS);
            } else {
                scheduler.shutdown();
            }
        }
    }

    private Class<IBaseResource> getValueSetClass() {
        return Resources.getClassForTypeAndVersion(
                "ValueSet", fhirClient.getFhirContext().getVersion().getVersion());
    }

    public static class TerminologyServerExpansionException extends BaseServerResponseException {

        private static final int STATUS_CODE = 429;

        public TerminologyServerExpansionException(String message) {
            super(STATUS_CODE, message);
        }
    }
}
