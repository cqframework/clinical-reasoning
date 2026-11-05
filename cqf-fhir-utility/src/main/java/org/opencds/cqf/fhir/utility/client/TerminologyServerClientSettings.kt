package org.opencds.cqf.fhir.utility.client

class TerminologyServerClientSettings {
    /**
     * Controls when the package's own resources are supplied to a remote `$expand` call via the
     * `tx-resource` parameter.
     * * `AUTO` — the first attempt is made WITHOUT `tx-resource`; retries (attempts 2..N) are made
     *   WITH `tx-resource`.
     * * `ENABLED` — every attempt (from the first) is made WITH `tx-resource`.
     * * `DISABLED` — `tx-resource` is never attached (legacy behavior).
     */
    enum class TxResourceMode {
        AUTO,
        ENABLED,
        DISABLED,
    }

    /**
     * Maximum number of attempts for terminology-server calls before a failure is surfaced. This
     * governs BOTH ValueSet `$expand` calls and the resource-resolution reads
     * (`getValueSetResource`, `getCodeSystemResource`, `getLatestValueSetResource`) used during
     * `$package` / `$data-requirements` dependency gathering. Only transient failures
     * (connection/read timeouts and retryable HTTP statuses) are retried; each retry is delayed by
     * [.retryIntervalMillis] times the attempt number (linear back-off).
     *
     * **Performance note:** a value greater than 1 improves resilience against a flaky terminology
     * server (a single hiccup no longer aborts the whole operation), but it also makes every
     * *transient* failure take up to `maxRetryCount`× longer — dominated by the per-attempt connect
     * timeout — which can noticeably slow operations that make many terminology-server calls (e.g.
     * `$data-requirements` against many un-cached ValueSets). In performance-sensitive production
     * environments that prefer fail-fast over resilience, set this to `1` to disable retries
     * entirely (a single attempt per call).
     */
    var maxRetryCount: Int = 3
        private set

    var retryIntervalMillis: Long = 1000
        private set

    var timeoutSeconds: Int = 30
        private set

    var socketTimeout: Int = 60
        private set

    var crmiVersion: String? = "1.0.0"
        private set

    var expansionsPerPage: Int = 1000
        private set

    var maxExpansionPages: Int = 1000
        private set

    var txResourceMode: TxResourceMode? = TxResourceMode.AUTO
        private set

    var maxTxResourceCodeSystemConcepts: Int = 1000
        private set

    internal constructor()

    /**
     * Copy constructor for ExpandSettings
     *
     * @param terminologyServerClientSettings the TerminologyServerClientSettings to copy
     */
    constructor(terminologyServerClientSettings: TerminologyServerClientSettings) {
        this.maxRetryCount = terminologyServerClientSettings.maxRetryCount
        this.retryIntervalMillis = terminologyServerClientSettings.retryIntervalMillis
        this.timeoutSeconds = terminologyServerClientSettings.timeoutSeconds
        this.socketTimeout = terminologyServerClientSettings.socketTimeout
        this.crmiVersion = terminologyServerClientSettings.crmiVersion
        this.expansionsPerPage = terminologyServerClientSettings.expansionsPerPage
        this.maxExpansionPages = terminologyServerClientSettings.maxExpansionPages
        this.txResourceMode = terminologyServerClientSettings.txResourceMode
        this.maxTxResourceCodeSystemConcepts =
            terminologyServerClientSettings.maxTxResourceCodeSystemConcepts
    }

    fun setMaxRetryCount(maxRetryCount: Int): TerminologyServerClientSettings {
        this.maxRetryCount = maxRetryCount
        return this
    }

    fun setRetryIntervalMillis(retryIntervalMillis: Long): TerminologyServerClientSettings {
        this.retryIntervalMillis = retryIntervalMillis
        return this
    }

    fun setTimeoutSeconds(timeoutSeconds: Int): TerminologyServerClientSettings {
        this.timeoutSeconds = timeoutSeconds
        return this
    }

    fun setSocketTimeout(socketTimeout: Int): TerminologyServerClientSettings {
        this.socketTimeout = socketTimeout
        return this
    }

    fun setCrmiVersion(crmiVersion: String?): TerminologyServerClientSettings {
        this.crmiVersion = crmiVersion
        return this
    }

    fun setExpansionsPerPage(expansionsPerPage: Int): TerminologyServerClientSettings {
        this.expansionsPerPage = expansionsPerPage
        return this
    }

    fun setMaxExpansionPages(maxExpansionPages: Int): TerminologyServerClientSettings {
        this.maxExpansionPages = maxExpansionPages
        return this
    }

    fun withTxResourceMode(txResourceMode: TxResourceMode?): TerminologyServerClientSettings {
        return setTxResourceMode(txResourceMode)
    }

    fun setTxResourceMode(txResourceMode: TxResourceMode?): TerminologyServerClientSettings {
        this.txResourceMode = txResourceMode
        return this
    }

    fun withMaxTxResourceCodeSystemConcepts(
        maxTxResourceCodeSystemConcepts: Int
    ): TerminologyServerClientSettings {
        return setMaxTxResourceCodeSystemConcepts(maxTxResourceCodeSystemConcepts)
    }

    fun setMaxTxResourceCodeSystemConcepts(
        maxTxResourceCodeSystemConcepts: Int
    ): TerminologyServerClientSettings {
        this.maxTxResourceCodeSystemConcepts = maxTxResourceCodeSystemConcepts
        return this
    }

    companion object {
        @JvmStatic
        val default: TerminologyServerClientSettings
            get() = TerminologyServerClientSettings()
    }
}
