package org.opencds.cqf.fhir.utility.client;

public class TerminologyServerClientSettings {

    private int maxRetryCount = 3;
    private long retryIntervalMillis = 1000;
    private int timeoutSeconds = 30;
    private int socketTimeout = 60;
    private String crmiVersion = "1.0.0";
    private int expansionsPerPage = 1000;
    private int maxExpansionPages = 1000;

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
}
