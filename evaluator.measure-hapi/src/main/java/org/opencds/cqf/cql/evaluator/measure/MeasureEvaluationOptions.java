package org.opencds.cqf.cql.evaluator.measure;

public class MeasureEvaluationOptions {
    public static MeasureEvaluationOptions defaultOptions() {
        return new MeasureEvaluationOptions();
    }

    private boolean isThreadedEnabled = false;
    private Integer threadedBatchSize = 200;
    private Integer numThreads = null;

    public boolean isThreadedEnabled() {
        return this.isThreadedEnabled;
    }

    public void setThreadedEnabled(Boolean isThreadedEnabled) {
        this.isThreadedEnabled = isThreadedEnabled;
    }

    public Integer getThreadedBatchSize() {
        return this.threadedBatchSize;
    }

    public void setThreadedBatchSize(Integer threadedBatchSize) {
        this.threadedBatchSize = threadedBatchSize;
    }

    public Integer getNumThreads() {
        if (this.numThreads == null) {
            return Runtime.getRuntime().availableProcessors();
        }

        return this.numThreads;
    }

    public void setNumThreads(Integer value) {
        this.numThreads = value;
    }
}
