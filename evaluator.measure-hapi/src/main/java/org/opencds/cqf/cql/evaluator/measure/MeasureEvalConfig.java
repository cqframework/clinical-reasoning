package org.opencds.cqf.cql.evaluator.measure;

import java.util.EnumSet;

import org.opencds.cqf.cql.engine.execution.CqlEngine;

public class MeasureEvalConfig {
    public static MeasureEvalConfig defaultConfig() {
        return new MeasureEvalConfig();
    }

    private Boolean parallelEnabled = false;
    private Integer parallelThreshold = 200;
    private Integer parallelThreads = null;

    private Boolean debugLoggingEnabled = false;

    private EnumSet<CqlEngine.Options> cqlEngineOptions;

    public EnumSet<CqlEngine.Options> getCqlEngineOptions() {
        if (this.cqlEngineOptions == null) {
            this.cqlEngineOptions = EnumSet.of(CqlEngine.Options.EnableExpressionCaching);
        }

        return this.cqlEngineOptions; 
    }

    public void setCqlEngineOptions(EnumSet<CqlEngine.Options> cqlEngineOptions) {
        this.cqlEngineOptions = cqlEngineOptions;
    }

    public Boolean getDebugLoggingEnabled() {
        return this.debugLoggingEnabled;
    }

    public void setDebugLoggingEnabled(Boolean value) {
        this.debugLoggingEnabled = value;
    }

    public Boolean getParallelEnabled() {
        return this.parallelEnabled;
    }

    public void setParallelEnabled(Boolean value) {
        this.parallelEnabled = value;
    }

    public MeasureEvalConfig withParallelEnabled(Boolean value) {
        this.parallelEnabled = value;
        return this;
    }

    public Integer getParallelThreshold() {
        return this.parallelThreshold;
    }

    public MeasureEvalConfig withParallelThreshold(int threshold) {
        this.parallelThreshold = threshold;
        return this;
    }

    public void setParallelThreshold(Integer value) {
        this.parallelThreshold = value;
    }

    public Integer getParallelThreads() {
        if (this.parallelThreads == null) {
            return Runtime.getRuntime().availableProcessors();
        }

        return this.parallelThreads;
    }

    public void setParallelThreads(Integer value) {
        this.parallelThreads = value;
    }
}
