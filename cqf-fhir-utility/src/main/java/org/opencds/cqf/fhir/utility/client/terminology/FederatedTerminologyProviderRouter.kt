package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.Optional
import java.util.function.Supplier
import kotlin.math.max
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r4.model.TerminologyCapabilities
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings
import org.opencds.cqf.fhir.utility.search.Searches
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class is a router that links ValueSets + Endpoints to actual Terminology Servers. Routing
 * rules are derived from the CRMI IG,
 * https://hl7.org/fhir/uv/crmi/StructureDefinition-crmi-artifact-endpoint-configurable-operation.html
 */
open class FederatedTerminologyProviderRouter
@JvmOverloads
constructor(
    fhirContext: FhirContext,
    terminologyServerClientSettings: TerminologyServerClientSettings? = null,
) : BaseTerminologyProvider(fhirContext), ITerminologyProviderRouter {
    private val defaultClient =
        GenericTerminologyServerClient(fhirContext, terminologyServerClientSettings)
    private val clients: MutableList<ITerminologyServerClient> =
        mutableListOf<ITerminologyServerClient>(
            defaultClient,
            VsacTerminologyServerClient(fhirContext, terminologyServerClientSettings),
        )

    private fun getClient(address: String?): ITerminologyServerClient {
        return clients.firstOrNull { client -> client.isCanonicalMatch(address) } ?: defaultClient
    }

    /**
     * Returns a list of endpoints sorted such that the endpoint that matches the ValueSet's
     * canonical URL is first
     *
     * @param endpoints List of terminology server endpoints that can be used to lookup ValueSet
     *   resources
     * @param url ValueSet's Canonical URL
     * @return Sorted endpoints
     */
    private fun prioritizeEndpoints(
        endpoints: MutableList<IEndpointAdapter?>?,
        url: String?,
    ): MutableList<IEndpointAdapter?> {
        if (endpoints.isNullOrEmpty()) {
            return mutableListOf()
        }

        val byUrlMatch =
            Comparator.comparing<IEndpointAdapter?, Boolean> { endpoint ->
                val address = endpoint!!.address
                url == null || address == null || !url.startsWith(address)
            }

        val byAddress =
            Comparator.comparing<IEndpointAdapter?, String?>(
                IEndpointAdapter::address,
                Comparator.nullsLast<String>(Comparator.naturalOrder<String>()),
            )

        return endpoints.sortedWith(byUrlMatch.thenComparing(byAddress)).toMutableList()
    }

    override fun expand(
        valueSet: IValueSetAdapter?,
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
    ): IBaseResource? {
        return this.getClient(valueSet!!.url).expand(valueSet, endpoint, parameters)
    }

    override fun expand(
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource? {
        return this.getClient(endpoint!!.address).expand(endpoint, parameters, fhirVersion)
    }

    override fun expand(
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
        url: String?,
        valueSetVersion: String?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource? {
        return this.getClient(url).expand(endpoint, parameters, url, valueSetVersion, fhirVersion)
    }

    override fun expand(
        fhirClient: IGenericClient?,
        url: String?,
        parameters: IBaseParameters?,
    ): IBaseResource? {
        return this.getClient(url).expand(fhirClient, url, parameters)
    }

    override fun expand(
        valueSet: IValueSetAdapter?,
        endpoints: MutableList<IEndpointAdapter?>?,
        parameters: IParametersAdapter?,
    ): IBaseResource? {
        return prioritizeEndpoints(endpoints, valueSet!!.url).firstNotNullOfOrNull { endpoint ->
            expand(valueSet, endpoint, parameters)
        }
    }

    override fun expand(
        endpoints: MutableList<IEndpointAdapter?>?,
        parameters: IParametersAdapter?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource? {
        return endpoints!!.firstNotNullOfOrNull { endpoint ->
            expand(endpoint!!, parameters, fhirVersion)
        }
    }

    override fun expand(
        endpoints: MutableList<IEndpointAdapter?>?,
        parameters: IParametersAdapter?,
        url: String?,
        valueSetVersion: String?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource? {
        return prioritizeEndpoints(endpoints, url).firstNotNullOfOrNull { endpoint ->
            expand(endpoint, parameters, url, valueSetVersion, fhirVersion)
        }
    }

    override fun getCodeSystemResource(
        endpoint: IEndpointAdapter?,
        url: String?,
    ): Optional<IDomainResource> {
        val client = this.getClient(endpoint!!.address)
        return executeWithRetry(client, url) {
            IKnowledgeArtifactAdapter.findLatestVersion(
                client
                    .initializeClientWithAuth(endpoint)
                    .search<IBaseBundle>()
                    .forResource(client.codeSystemClass)
                    .where(Searches.toFlattenedMap(Searches.byCanonical(url)))
                    .execute()
            )
        }!!
    }

    override fun getCodeSystemResource(
        endpoints: MutableList<IEndpointAdapter?>?,
        url: String?,
    ): Optional<IDomainResource> {
        return prioritizeEndpoints(endpoints, url)
            .stream()
            .map { endpoint -> getCodeSystemResource(endpoint!!, url) }
            .flatMap { obj -> obj.stream() }
            .findFirst()
    }

    override fun getLatestValueSetResource(
        endpoint: IEndpointAdapter?,
        url: String?,
    ): Optional<IDomainResource> {
        val client = this.getClient(endpoint!!.address)
        return executeWithRetry(client, url) {
            IKnowledgeArtifactAdapter.findLatestVersion(
                client
                    .initializeClientWithAuth(endpoint)
                    .search<IBaseBundle>()
                    .forResource(client.valueSetClass)
                    .where(Searches.toFlattenedMap(Searches.byCanonical(url)))
                    .execute()
            )
        }!!
    }

    override fun getLatestValueSetResource(
        endpoints: MutableList<IEndpointAdapter?>?,
        url: String?,
    ): Optional<IDomainResource> {
        return prioritizeEndpoints(endpoints, url)
            .stream()
            .map { endpoint -> getLatestValueSetResource(endpoint!!, url) }
            .flatMap { obj -> obj.stream() }
            .findFirst()
    }

    override fun getR4TerminologyCapabilities(
        endpoint: IEndpointAdapter?
    ): TerminologyCapabilities? {
        return getClient(endpoint!!.address).getR4TerminologyCapabilities(endpoint)
    }

    override fun getTerminologyServerClientSettings(
        endpoint: IEndpointAdapter?
    ): TerminologyServerClientSettings? {
        return getClient(endpoint!!.address).terminologyServerClientSettings
    }

    override fun getValueSetResource(
        endpoint: IEndpointAdapter?,
        url: String?,
    ): Optional<IDomainResource> {
        val client = this.getClient(url)
        return executeWithRetry(client, url) {
            IKnowledgeArtifactAdapter.findLatestVersion(
                client
                    .initializeClientWithAuth(endpoint)
                    .search<IBaseBundle>()
                    .forResource(client.valueSetClass)
                    .where(Searches.toFlattenedMap(Searches.byCanonical(url)))
                    .execute()
            )
        }!!
    }

    /**
     * Executes a terminology-server read, retrying transient failures (connection/read timeouts and
     * retryable HTTP statuses) up to `maxRetryCount` times with a linear back-off, mirroring the
     * resilience of the expansion path (`ExpandRunner`). A single transient tx-server hiccup during
     * dependency resolution should not abort an entire `$package` run. Non-transient errors and the
     * final failure after exhausting retries propagate unchanged.
     *
     * **Performance note:** against a flaky terminology server this trades speed for resilience —
     * each transient failure takes up to `maxRetryCount`× longer (dominated by the per-attempt
     * connect timeout). Operations that make many resolution calls (e.g. `$data-requirements` over
     * many un-cached ValueSets) can be noticeably slower as a result. Performance-sensitive
     * environments that prefer fail-fast can disable retries by setting
     * [maxRetryCount][TerminologyServerClientSettings.setMaxRetryCount] to `1`.
     */
    fun <T> executeWithRetry(
        client: ITerminologyServerClient,
        url: String?,
        operation: Supplier<T?>,
    ): T? {
        val settings = client.terminologyServerClientSettings
        val maxAttempts = if (settings != null) max(1, settings.maxRetryCount) else 1
        val retryIntervalMillis = if (settings != null) settings.retryIntervalMillis else 0L

        var lastError: RuntimeException? = null
        for (attempt in 1..maxAttempts) {
            try {
                return operation.get()
            } catch (e: RuntimeException) {
                lastError = e
                if (attempt >= maxAttempts || !isTransient(e)) {
                    throw e
                }
                logger.warn(
                    "Transient error resolving {} from terminology server (attempt {} of {}): {}. Retrying...",
                    url,
                    attempt,
                    maxAttempts,
                    e.message,
                )
                try {
                    Thread.sleep(retryIntervalMillis * attempt)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
            }
        }
        throw lastError!! // unreachable: the loop either returns or throws
    }

    override fun getValueSetResource(
        endpoints: MutableList<IEndpointAdapter?>?,
        url: String?,
    ): Optional<IDomainResource> {
        return prioritizeEndpoints(endpoints, url)
            .stream()
            .map { endpoint -> getValueSetResource(endpoint, url) }
            .flatMap { obj -> obj.stream() }
            .findFirst()
    }

    // CRMI ArtifactEndpointConfiguration-based routing methods
    /**
     * Prioritizes artifact endpoint configurations based on CRMI routing rules:
     * 1. Configurations where artifactRoute matches the start of canonicalUrl are ranked by match
     *    length
     * 2. Configurations without artifactRoute are included but ranked lower (score = 0)
     * 3. For equal scores, original order is preserved
     *
     * @param configurations list of artifact endpoint configurations
     * @param canonicalUrl the canonical URL to match against
     * @return prioritized list of configurations (excludes non-matching configurations)
     */
    private fun prioritizeConfigurations(
        configurations: MutableList<ArtifactEndpointConfiguration?>?,
        canonicalUrl: String?,
    ): MutableList<ArtifactEndpointConfiguration?> {
        if (configurations.isNullOrEmpty()) {
            return mutableListOf()
        }

        // Create scored configurations, preserving original index for stable sorting
        data class ScoredConfig(
            val config: ArtifactEndpointConfiguration?,
            val score: Int,
            val originalIndex: Int,
        )

        val scored = mutableListOf<ScoredConfig>()
        for (i in configurations.indices) {
            val config = configurations[i]
            val score = config!!.getMatchScore(canonicalUrl)
            if (score >= 0) { // -1 means no match, exclude those
                scored.add(ScoredConfig(config, score, i))
            }
        }

        // Sort by score descending, then by original index for stability
        return scored
            .sortedWith(
                Comparator.comparingInt<ScoredConfig> { s -> s.score }
                    .reversed()
                    .thenComparingInt { s -> s.originalIndex }
            )
            .map(ScoredConfig::config)
            .toMutableList()
    }

    override fun expandWithConfigurations(
        valueSet: IValueSetAdapter?,
        configurations: MutableList<ArtifactEndpointConfiguration?>?,
        parameters: IParametersAdapter?,
    ): IBaseResource? {
        if (configurations.isNullOrEmpty()) {
            return null
        }

        val valueSetUrl = valueSet!!.url
        val prioritized = prioritizeConfigurations(configurations, valueSetUrl)

        for (config in prioritized) {
            try {
                val endpoint = config!!.getEffectiveEndpoint(fhirContext)
                if (endpoint != null) {
                    val result = expand(valueSet, endpoint, parameters)
                    if (result != null) {
                        return result
                    }
                }
            } catch (e: Exception) {
                // Log and try next configuration
                // Swallow exception to try next endpoint in priority order
            }
        }
        return null
    }

    override fun getValueSetResourceWithConfigurations(
        configurations: MutableList<ArtifactEndpointConfiguration?>?,
        url: String?,
    ): Optional<IDomainResource> {
        if (configurations.isNullOrEmpty()) {
            return Optional.empty<IDomainResource>()
        }

        val prioritized = prioritizeConfigurations(configurations, url)

        for (config in prioritized) {
            try {
                val endpoint = config!!.getEffectiveEndpoint(fhirContext)
                if (endpoint != null) {
                    val result = getValueSetResource(endpoint, url)
                    if (result.isPresent) {
                        return result
                    }
                }
            } catch (e: Exception) {
                // Try next configuration
            }
        }
        return Optional.empty<IDomainResource>()
    }

    override fun getCodeSystemResourceWithConfigurations(
        configurations: MutableList<ArtifactEndpointConfiguration?>?,
        url: String?,
    ): Optional<IDomainResource> {
        if (configurations.isNullOrEmpty()) {
            return Optional.empty<IDomainResource>()
        }

        val prioritized = prioritizeConfigurations(configurations, url)

        for (config in prioritized) {
            try {
                val endpoint = config!!.getEffectiveEndpoint(fhirContext)
                if (endpoint != null) {
                    val result = getCodeSystemResource(endpoint, url)
                    if (result.isPresent) {
                        return result
                    }
                }
            } catch (e: Exception) {
                // Try next configuration
            }
        }
        return Optional.empty<IDomainResource>()
    }

    companion object {
        private val logger: Logger =
            LoggerFactory.getLogger(FederatedTerminologyProviderRouter::class.java)

        @JvmStatic
        fun isTransient(error: Throwable?): Boolean {
            var t = error
            while (t != null) {
                // Connection-level failures (connect/read timeouts, connection refused) — the
                // reported symptom.
                if (
                    t is FhirClientConnectionException ||
                        t is SocketTimeoutException ||
                        t is ConnectException
                ) {
                    return true
                }
                if (
                    t is BaseServerResponseException &&
                        when (t.statusCode) {
                            408,
                            429,
                            500,
                            502,
                            503,
                            504 -> true
                            else -> false
                        }
                ) {
                    return true
                }
                if (t.cause === t) {
                    break
                }
                t = t.cause
            }
            return false
        }
    }
}
