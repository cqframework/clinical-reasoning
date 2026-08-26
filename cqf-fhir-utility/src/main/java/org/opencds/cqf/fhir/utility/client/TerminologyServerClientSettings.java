package org.opencds.cqf.fhir.utility.client;

public class TerminologyServerClientSettings {

    /**
     * Controls when the package's own resources are supplied to a remote {@code $expand} call via the
     * {@code tx-resource} parameter.
     * <ul>
     *   <li>{@code AUTO} — the first attempt is made WITHOUT {@code tx-resource}; retries (attempts 2..N)
     *       are made WITH {@code tx-resource}.</li>
     *   <li>{@code ENABLED} — every attempt (from the first) is made WITH {@code tx-resource}.</li>
     *   <li>{@code DISABLED} — {@code tx-resource} is never attached (legacy behavior).</li>
     * </ul>
     */
    public enum TxResourceMode {
        AUTO,
        ENABLED,
        DISABLED
    }

    private int maxRetryCount = 3;
    private long retryIntervalMillis = 1000;
    private int timeoutSeconds = 30;
    private int socketTimeout = 60;
    private String crmiVersion = "1.0.0";
    private int expansionsPerPage = 1000;
    private int maxExpansionPages = 1000;
    private TxResourceMode txResourceMode = TxResourceMode.AUTO;
    private int maxTxResourceCodeSystemConcepts = 1000;

    public static TerminologyServerClientSettings getDefault() {
        return new TerminologyServerClientSettings();
    }

    TerminologyServerClientSettings() {
        // intentionally empty
    }

    /**
     * Copy constructor for ExpandSettings
     * @param terminologyServerClientSettings the TerminologyServerClientSettings to copy
     */
    public TerminologyServerClientSettings(TerminologyServerClientSettings terminologyServerClientSettings) {
        this.maxRetryCount = terminologyServerClientSettings.maxRetryCount;
        this.retryIntervalMillis = terminologyServerClientSettings.retryIntervalMillis;
        this.timeoutSeconds = terminologyServerClientSettings.timeoutSeconds;
        this.socketTimeout = terminologyServerClientSettings.socketTimeout;
        this.crmiVersion = terminologyServerClientSettings.crmiVersion;
        this.expansionsPerPage = terminologyServerClientSettings.expansionsPerPage;
        this.maxExpansionPages = terminologyServerClientSettings.maxExpansionPages;
        this.txResourceMode = terminologyServerClientSettings.txResourceMode;
        this.maxTxResourceCodeSystemConcepts = terminologyServerClientSettings.maxTxResourceCodeSystemConcepts;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public TerminologyServerClientSettings setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public long getRetryIntervalMillis() {
        return retryIntervalMillis;
    }

    public TerminologyServerClientSettings setRetryIntervalMillis(long retryIntervalMillis) {
        this.retryIntervalMillis = retryIntervalMillis;
        return this;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public TerminologyServerClientSettings setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public TerminologyServerClientSettings setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public String getCrmiVersion() {
        return crmiVersion;
    }

    public TerminologyServerClientSettings setCrmiVersion(String crmiVersion) {
        this.crmiVersion = crmiVersion;
        return this;
    }

    public int getExpansionsPerPage() {
        return expansionsPerPage;
    }

    public TerminologyServerClientSettings setExpansionsPerPage(int expansionsPerPage) {
        this.expansionsPerPage = expansionsPerPage;
        return this;
    }

    public int getMaxExpansionPages() {
        return maxExpansionPages;
    }

    public TerminologyServerClientSettings setMaxExpansionPages(int maxExpansionPages) {
        this.maxExpansionPages = maxExpansionPages;
        return this;
    }

    public TxResourceMode getTxResourceMode() {
        return txResourceMode;
    }

    public TerminologyServerClientSettings withTxResourceMode(TxResourceMode txResourceMode) {
        return setTxResourceMode(txResourceMode);
    }

    public TerminologyServerClientSettings setTxResourceMode(TxResourceMode txResourceMode) {
        this.txResourceMode = txResourceMode;
        return this;
    }

    public int getMaxTxResourceCodeSystemConcepts() {
        return maxTxResourceCodeSystemConcepts;
    }

    public TerminologyServerClientSettings withMaxTxResourceCodeSystemConcepts(int maxTxResourceCodeSystemConcepts) {
        return setMaxTxResourceCodeSystemConcepts(maxTxResourceCodeSystemConcepts);
    }

    public TerminologyServerClientSettings setMaxTxResourceCodeSystemConcepts(int maxTxResourceCodeSystemConcepts) {
        this.maxTxResourceCodeSystemConcepts = maxTxResourceCodeSystemConcepts;
        return this;
    }
}
