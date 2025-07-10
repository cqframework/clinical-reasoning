package org.opencds.cqf.fhir.utility.client;

public class TerminologyServerClientSettings {

    private int maxRetryCount = 3;
    private long retryIntervalMillis = 1000;
    private int timeoutSeconds = 30;
    private int socketTimeout = 60;
    /*
     * Default constructor for TerminologySettings
     */
    public TerminologyServerClientSettings() {
        // intentionally empty
    }

    /**
     * Copy constructor for ExpandSettings
     * @param terminologyServerClientSettings
     */
    public TerminologyServerClientSettings(TerminologyServerClientSettings terminologyServerClientSettings) {
        this.maxRetryCount = terminologyServerClientSettings.maxRetryCount;
        this.retryIntervalMillis = terminologyServerClientSettings.retryIntervalMillis;
        this.timeoutSeconds = terminologyServerClientSettings.timeoutSeconds;
        this.socketTimeout = terminologyServerClientSettings.socketTimeout;
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

    public int getSocketTimeout() { return socketTimeout; }

    public TerminologyServerClientSettings setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }
}
