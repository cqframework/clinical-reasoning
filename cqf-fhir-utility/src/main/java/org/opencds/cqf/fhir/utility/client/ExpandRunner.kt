package org.opencds.cqf.fhir.utility.client

import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.apache.http.HttpStatus
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.opencds.cqf.fhir.utility.Canonicals.getIdPart
import org.opencds.cqf.fhir.utility.Canonicals.getResourceType
import org.opencds.cqf.fhir.utility.Canonicals.getVersion
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.Resources.getClassForTypeAndVersion
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.Companion.createAdapterForResource
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings.TxResourceMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ExpandRunner(
    private val fhirClient: IGenericClient,
    private val terminologyServerClientSettings: TerminologyServerClientSettings,
    private val valueSetUrl: String,
    private val parameters: IBaseParameters?,
    scheduler: ScheduledExecutorService?,
) : Runnable {
    private var expansionAttempt = 0
    private var expandedValueSet: IBaseResource? = null

    private val scheduler: ScheduledExecutorService =
        scheduler ?: Executors.newScheduledThreadPool(1)

    constructor(
        client: IGenericClient,
        terminologyServerClientSettings: TerminologyServerClientSettings,
        valueSetUrl: String,
        parameters: IBaseParameters?,
    ) : this(client, terminologyServerClientSettings, valueSetUrl, parameters, null)

    fun expandValueSet(): IBaseResource {
        val result = scheduler.schedule(this, 0, TimeUnit.SECONDS)
        try {
            result.get()
            if (
                scheduler.awaitTermination(
                    terminologyServerClientSettings.timeoutSeconds.toLong(),
                    TimeUnit.SECONDS,
                )
            ) {
                if (result.isDone && expandedValueSet != null) {
                    return expandedValueSet!!
                } else {
                    throw UnprocessableEntityException(
                        "Terminology Server expansion failed for ValueSet ($valueSetUrl) - Server could not process expansion requests."
                    )
                }
            } else {
                throw UnprocessableEntityException(
                    "Terminology Server expansion took longer than the allotted timeout: ${terminologyServerClientSettings.timeoutSeconds}"
                )
            }
        } catch (e: Exception) {
            scheduler.shutdown()
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            throw TerminologyServerExpansionException(e.message)
        }
    }

    override fun run() {
        try {
            expansionAttempt++
            if (expansionAttempt <= terminologyServerClientSettings.maxRetryCount) {
                val id = buildResourceIdForExpand(valueSetUrl)
                val terminologyServerBase = fhirClient.getServerBase()
                val fullExpandUrl = "$terminologyServerBase/$id/\$expand"

                // Compute the request parameters for THIS attempt. Depending on the configured
                // TxResourceMode, tx-resource parameters may be stripped for this attempt (see
                // parametersForAttempt).
                val requestParameters = parametersForAttempt(expansionAttempt)

                // Format parameters for logging
                val parametersLog = formatParametersForLogging(requestParameters)

                val txResourceCount: Long = countTxResourceParameters(requestParameters)
                logger.info(
                    "Expansion attempt {} for ValueSet: {} | tx-resource params sent: {} (mode={}) | Terminology Server: {} | Full URL: {} | Parameters: {}",
                    expansionAttempt,
                    valueSetUrl,
                    txResourceCount,
                    terminologyServerClientSettings.txResourceMode,
                    terminologyServerBase,
                    fullExpandUrl,
                    parametersLog,
                )

                expandedValueSet =
                    fhirClient
                        .operation()
                        .onInstance(id)
                        .named("\$expand")
                        .withParameters<IBaseParameters?>(requestParameters)
                        .returnResourceType(this.valueSetClass)
                        .execute()

                val expandedValueSetAdapter =
                    IAdapterFactory.createAdapterForResource(expandedValueSet!!) as IValueSetAdapter

                if (
                    expandedValueSetAdapter.expansionTotal >
                        expandedValueSetAdapter.expansionContains!!.size
                ) {
                    val paramsWithOffset =
                        createAdapterForResource(
                            createAdapterForResource(requestParameters!!).copy()
                        )
                            as IParametersAdapter
                    var offset = terminologyServerClientSettings.expansionsPerPage

                    var expansionPage = 2
                    while (
                        expansionPage <= terminologyServerClientSettings.maxExpansionPages &&
                            offset < expandedValueSetAdapter.expansionTotal
                    ) {
                        logger.info(
                            "Expanding page: {} for ValueSet: {}",
                            expansionPage,
                            valueSetUrl,
                        )
                        paramsWithOffset.setParameter("offset", offset)
                        val nextExpansion =
                            fhirClient
                                .operation()
                                .onInstance(id)
                                .named("\$expand")
                                .withParameters(paramsWithOffset.get() as IBaseParameters)
                                .returnResourceType(this.valueSetClass)
                                .execute()

                        val nextExpansionValueSetAdapter =
                            createAdapterForResource(nextExpansion) as IValueSetAdapter

                        expandedValueSetAdapter.appendExpansionContains(
                            nextExpansionValueSetAdapter.expansionContains
                        )

                        offset += terminologyServerClientSettings.expansionsPerPage
                        expansionPage++
                    }
                }

                scheduler.shutdown()
            }
        } catch (ex: Exception) {
            val isTransient: Boolean = isTransient(ex)
            val id = buildResourceIdForExpand(valueSetUrl)
            val terminologyServerBase = fhirClient.getServerBase()
            val fullExpandUrl = "$terminologyServerBase/$id/\$expand"
            val parametersLog = formatParametersForLogging(parameters)

            logger.warn(
                "Expansion attempt {} failed{} for ValueSet: {} | Terminology Server: {} | Full URL: {} | Parameters: {} | Error: {}",
                expansionAttempt,
                if (isTransient) " due to transient fault" else "",
                valueSetUrl,
                terminologyServerBase,
                fullExpandUrl,
                parametersLog,
                ex.message,
            )

            // Decide whether another attempt is worthwhile. Retrying only helps if either the fault
            // was
            // transient (a later identical call may succeed) or the NEXT attempt would send a
            // materially
            // different request. The latter happens only in AUTO mode after attempt 1, where
            // attempt 2
            // escalates by attaching tx-resource parameters (the escalation that a "definition
            // could not be
            // found" 422 is meant to resolve). Retrying a non-transient failure with an identical
            // request
            // (e.g. HTTP 422 for a CodeSystem the server will never know about) only adds latency
            // and log
            // noise, so stop early in that case.
            val nextAttemptEscalates =
                (terminologyServerClientSettings.txResourceMode == TxResourceMode.AUTO) &&
                    expansionAttempt == 1
            if (
                (isTransient || nextAttemptEscalates) &&
                    expansionAttempt < terminologyServerClientSettings.maxRetryCount
            ) {
                scheduler.schedule(
                    this,
                    terminologyServerClientSettings.retryIntervalMillis * expansionAttempt,
                    TimeUnit.MILLISECONDS,
                )
            } else {
                scheduler.shutdown()
            }
        }
    }

    /**
     * Builds the resource ID for the $expand operation. VSAC requires a non-standard syntax where
     * the version is appended to the ID with a dash instead of using the pipe separator (e.g.,
     * "ValueSet/id-version" instead of "ValueSet/id|version").
     *
     * @param canonicalUrl The canonical URL which may include a version (e.g., "url|version")
     * @return The resource ID formatted for the $expand operation
     */
    private fun buildResourceIdForExpand(canonicalUrl: String): String {
        val resourceType = getResourceType(canonicalUrl)
        val idPart = getIdPart(canonicalUrl)
        val version = getVersion(canonicalUrl)

        // Check if this is a VSAC URL by looking for cts.nlm.nih.gov
        val isVsac = canonicalUrl != null && canonicalUrl.contains("cts.nlm.nih.gov")

        if (version != null && isVsac) {
            // VSAC requires version appended with dash: "ValueSet/id-version"
            return resourceType + "/" + idPart + "-" + version
        } else {
            // Standard format: "ValueSet/id"
            return resourceType + "/" + idPart
        }
    }

    /**
     * Computes the `$expand` request parameters to send for the given attempt, honoring the
     * configured [TxResourceMode]. The `tx-resource` entries always ride inside [.parameters]; this
     * method decides, per attempt, whether to send them:
     * * `DISABLED` — nothing was attached upstream; send [.parameters] as-is.
     * * `ENABLED` — always keep `tx-resource`; send [.parameters] as-is.
     * * `AUTO` — attempt 1 is sent WITHOUT `tx-resource` (a filtered copy); attempts 2..N keep
     *   `tx-resource`.
     *
     * @param attempt the 1-based attempt number
     * @return the parameters to send for this attempt (either [.parameters] or a filtered copy)
     */
    fun parametersForAttempt(attempt: Int): IBaseParameters? {
        if (parameters == null) {
            return null
        }
        val mode = terminologyServerClientSettings.txResourceMode
        if (mode == TxResourceMode.AUTO && attempt <= 1) {
            return withoutTxResourceParameters(parameters)
        }
        return parameters
    }

    private val valueSetClass: Class<IBaseResource>
        get() =
            getClassForTypeAndVersion<IBaseResource>(
                "ValueSet",
                fhirClient.fhirContext.version.version,
            )

    /**
     * Formats Parameters for logging - extracts parameter names and values for debugging. Returns a
     * compact string representation like "count=1000, offset=0, system-version=..."
     */
    private fun formatParametersForLogging(parameters: IBaseParameters?): String {
        if (parameters == null) {
            return "none"
        }

        try {
            val parameterAdapter = createAdapterForResource(parameters) as IParametersAdapter
            val paramComponents = parameterAdapter.parameter

            if (paramComponents!!.isEmpty()) {
                return "none"
            }

            val paramStrings = mutableListOf<String?>()
            for (param in paramComponents) {
                val name = param!!.name
                val value = formatParameterValue(param)
                if (name != null && value != null) {
                    paramStrings.add("$name=$value")
                }
            }

            return if (paramStrings.isEmpty()) "none" else paramStrings.joinToString(", ")
        } catch (e: Exception) {
            return "error formatting parameters: " + e.message
        }
    }

    /** Formats a parameter value for logging - handles different value types. */
    private fun formatParameterValue(param: IParametersParameterComponentAdapter): String? {
        try {
            // Try to get primitive value
            if (param.hasValue()) {
                val value = param.value
                if (value is IPrimitiveType<*>) {
                    return value.value.toString()
                }
                return value.toString()
            }
            // Resource-valued parameters (e.g. tx-resource) — log a compact reference, not the full
            // body.
            if (param.hasResource()) {
                val resource = param.resource
                val ref = if (resource!!.idElement != null) resource.idElement.value else null
                return resource.fhirType() + (if (ref != null) "/$ref" else "")
            }
            // If no value, might be a part parameter
            if (param.hasPart()) {
                return "[" + param.part!!.size + " parts]"
            }
            return "null"
        } catch (e: Exception) {
            return "error"
        }
    }

    class TerminologyServerExpansionException(message: String?) :
        BaseServerResponseException(STATUS_CODE, message) {
        companion object {
            private const val STATUS_CODE = 429
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExpandRunner::class.java)

        /**
         * Returns a copy of the supplied parameters with all `tx-resource` parameters removed. Uses
         * the same adapter copy pattern used for paging so the original parameters are left
         * untouched.
         */
        private fun withoutTxResourceParameters(parameters: IBaseParameters): IBaseParameters {
            val copy =
                createAdapterForResource(createAdapterForResource(parameters).copy())
                    as IParametersAdapter
            copy.setParameter(
                copy.parameter!!
                    .filter { p -> Constants.TX_RESOURCE != p!!.name }
                    .map { obj -> obj!!.get() }
                    .toMutableList()
            )
            return copy.get() as IBaseParameters
        }

        private fun countTxResourceParameters(parameters: IBaseParameters?): Long {
            if (parameters == null) {
                return 0
            }
            return (createAdapterForResource(parameters) as IParametersAdapter)
                .parameter!!
                .stream()
                .filter { p: IParametersParameterComponentAdapter? ->
                    Constants.TX_RESOURCE == p!!.name
                }
                .count()
        }

        private fun isTransient(ex: Exception?): Boolean {
            var isTransient = false
            if (ex is BaseServerResponseException) {
                isTransient =
                    when (ex.statusCode) {
                        HttpStatus.SC_REQUEST_TIMEOUT,
                        HttpStatus.SC_TOO_MANY_REQUESTS,
                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        HttpStatus.SC_BAD_GATEWAY,
                        HttpStatus.SC_SERVICE_UNAVAILABLE,
                        HttpStatus.SC_GATEWAY_TIMEOUT -> true
                        else -> false
                    }
            }
            return isTransient
        }
    }
}
