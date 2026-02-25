package org.opencds.cqf.fhir.utility.client;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
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
                var id = buildResourceIdForExpand(valueSetUrl);
                String terminologyServerBase = fhirClient.getServerBase();
                String fullExpandUrl = terminologyServerBase + "/" + id + "/$expand";

                // Format parameters for logging
                String parametersLog = formatParametersForLogging(parameters);

                logger.info(
                        "Expansion attempt {} for ValueSet: {} | Terminology Server: {} | Full URL: {} | Parameters: {}",
                        expansionAttempt,
                        valueSetUrl,
                        terminologyServerBase,
                        fullExpandUrl,
                        parametersLog);

                expandedValueSet = fhirClient
                        .operation()
                        .onInstance(id)
                        .named("$expand")
                        .withParameters(parameters)
                        .returnResourceType(getValueSetClass())
                        .execute();

                var expandedValueSetAdapter = (IValueSetAdapter) createAdapterForResource(expandedValueSet);

                if (expandedValueSetAdapter.getExpansionTotal()
                        > expandedValueSetAdapter.getExpansionContains().size()) {
                    var paramsWithOffset = (IParametersAdapter) createAdapterForResource(
                            createAdapterForResource(parameters).copy());
                    var offset = terminologyServerClientSettings.getExpansionsPerPage();

                    for (int expansionPage = 2;
                            expansionPage <= terminologyServerClientSettings.getMaxExpansionPages()
                                    && offset < expandedValueSetAdapter.getExpansionTotal();
                            expansionPage++) {
                        logger.info("Expanding page: {} for ValueSet: {}", expansionPage, valueSetUrl);
                        paramsWithOffset.setParameter("offset", offset);
                        var nextExpansion = fhirClient
                                .operation()
                                .onInstance(id)
                                .named("$expand")
                                .withParameters((IBaseParameters) paramsWithOffset.get())
                                .returnResourceType(getValueSetClass())
                                .execute();

                        var nextExpansionValueSetAdapter = (IValueSetAdapter) createAdapterForResource(nextExpansion);

                        expandedValueSetAdapter.appendExpansionContains(
                                nextExpansionValueSetAdapter.getExpansionContains());

                        offset += terminologyServerClientSettings.getExpansionsPerPage();
                    }
                }

                scheduler.shutdown();
            }
        } catch (Exception ex) {
            var isTransient = isTransient(ex);
            var id = buildResourceIdForExpand(valueSetUrl);
            String terminologyServerBase = fhirClient.getServerBase();
            String fullExpandUrl = terminologyServerBase + "/" + id + "/$expand";
            String parametersLog = formatParametersForLogging(parameters);

            logger.warn(
                    "Expansion attempt {} failed{} for ValueSet: {} | Terminology Server: {} | Full URL: {} | Parameters: {} | Error: {}",
                    expansionAttempt,
                    isTransient ? " due to transient fault" : "",
                    valueSetUrl,
                    terminologyServerBase,
                    fullExpandUrl,
                    parametersLog,
                    ex.getMessage());

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

    /**
     * Builds the resource ID for the $expand operation.
     * VSAC requires a non-standard syntax where the version is appended to the ID with a dash
     * instead of using the pipe separator (e.g., "ValueSet/id-version" instead of "ValueSet/id|version").
     *
     * @param canonicalUrl The canonical URL which may include a version (e.g., "url|version")
     * @return The resource ID formatted for the $expand operation
     */
    private String buildResourceIdForExpand(String canonicalUrl) {
        String resourceType = Canonicals.getResourceType(canonicalUrl);
        String idPart = Canonicals.getIdPart(canonicalUrl);
        String version = Canonicals.getVersion(canonicalUrl);

        // Check if this is a VSAC URL by looking for cts.nlm.nih.gov
        boolean isVsac = canonicalUrl != null && canonicalUrl.contains("cts.nlm.nih.gov");

        if (version != null && isVsac) {
            // VSAC requires version appended with dash: "ValueSet/id-version"
            return resourceType + "/" + idPart + "-" + version;
        } else {
            // Standard format: "ValueSet/id"
            return resourceType + "/" + idPart;
        }
    }

    private static boolean isTransient(Exception ex) {
        var isTransient = false;
        if (ex instanceof BaseServerResponseException bsre) {
            isTransient = switch (bsre.getStatusCode()) {
                case HttpStatus.SC_REQUEST_TIMEOUT,
                        HttpStatus.SC_TOO_MANY_REQUESTS,
                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        HttpStatus.SC_BAD_GATEWAY,
                        HttpStatus.SC_SERVICE_UNAVAILABLE,
                        HttpStatus.SC_GATEWAY_TIMEOUT -> true;
                default -> false;};
        }
        return isTransient;
    }

    private Class<IBaseResource> getValueSetClass() {
        return Resources.getClassForTypeAndVersion(
                "ValueSet", fhirClient.getFhirContext().getVersion().getVersion());
    }

    /**
     * Formats Parameters for logging - extracts parameter names and values for debugging.
     * Returns a compact string representation like "count=1000, offset=0, system-version=..."
     */
    private String formatParametersForLogging(IBaseParameters parameters) {
        if (parameters == null) {
            return "none";
        }

        try {
            var parameterAdapter = (IParametersAdapter) createAdapterForResource(parameters);
            var paramComponents = parameterAdapter.getParameter();

            if (paramComponents.isEmpty()) {
                return "none";
            }

            var paramStrings = new java.util.ArrayList<String>();
            for (var param : paramComponents) {
                String name = param.getName();
                String value = formatParameterValue(param);
                if (name != null && value != null) {
                    paramStrings.add(name + "=" + value);
                }
            }

            return paramStrings.isEmpty() ? "none" : String.join(", ", paramStrings);
        } catch (Exception e) {
            return "error formatting parameters: " + e.getMessage();
        }
    }

    /**
     * Formats a parameter value for logging - handles different value types.
     */
    private String formatParameterValue(IParametersParameterComponentAdapter param) {
        try {
            // Try to get primitive value
            if (param.hasValue()) {
                var value = param.getValue();
                if (value instanceof IPrimitiveType<?> primitive) {
                    return String.valueOf(primitive.getValue());
                }
                return value.toString();
            }
            // If no value, might be a part parameter
            if (param.hasPart()) {
                return "[" + param.getPart().size() + " parts]";
            }
            return "null";
        } catch (Exception e) {
            return "error";
        }
    }

    public static class TerminologyServerExpansionException extends BaseServerResponseException {

        private static final int STATUS_CODE = 429;

        public TerminologyServerExpansionException(String message) {
            super(STATUS_CODE, message);
        }
    }
}
