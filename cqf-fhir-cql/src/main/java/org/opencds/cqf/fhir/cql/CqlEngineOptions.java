package org.opencds.cqf.fhir.cql;

import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
import org.opencds.cqf.cql.engine.execution.CqlEngine;

// TODO: Eventually, the cql-engine needs to expose these itself.
public class CqlEngineOptions {
    private Set<CqlEngine.Options> options = EnumSet.noneOf(CqlEngine.Options.class);
    private boolean isDebugLoggingEnabled = false;
    private boolean shouldExpandValueSets = false;
    private Integer pageSize;
    private Integer maxCodesPerQuery;
    private Integer queryBatchThreshold;
    private boolean enableHedisCompatibilityMode = false;

    public Set<CqlEngine.Options> getOptions() {
        return this.options;
    }

    public void setOptions(Set<CqlEngine.Options> options) {
        this.options = options;
    }

    public boolean isDebugLoggingEnabled() {
        return this.isDebugLoggingEnabled;
    }

    public void setDebugLoggingEnabled(boolean isDebugLoggingEnabled) {
        this.isDebugLoggingEnabled = isDebugLoggingEnabled;
    }

    public boolean shouldExpandValueSets() {
        return this.shouldExpandValueSets;
    }

    public void setShouldExpandValueSets(boolean shouldExpandValueSets) {
        this.shouldExpandValueSets = shouldExpandValueSets;
    }

    public Integer getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(Integer value) {
        this.pageSize = value;
    }

    public Integer getMaxCodesPerQuery() {
        return this.maxCodesPerQuery;
    }

    public void setMaxCodesPerQuery(Integer value) {
        this.maxCodesPerQuery = value;
    }

    public Integer getQueryBatchThreshold() {
        return this.queryBatchThreshold;
    }

    public void setQueryBatchThreshold(Integer value) {
        this.queryBatchThreshold = value;
    }

    public void setEnableHedisCompatibilityMode(boolean enableHedisCompatibilityMode) {
        this.enableHedisCompatibilityMode = enableHedisCompatibilityMode;
    }

    public boolean isEnableHedisCompatibilityMode() {
        return this.enableHedisCompatibilityMode;
    }

    public static CqlEngineOptions defaultOptions() {
        CqlEngineOptions result = new CqlEngineOptions();
        result.options.add(CqlEngine.Options.EnableExpressionCaching);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CqlEngineOptions.class.getSimpleName() + "[", "]")
                .add("options=" + options)
                .add("isDebugLoggingEnabled=" + isDebugLoggingEnabled)
                .add("shouldExpandValueSets=" + shouldExpandValueSets)
                .add("pageSize=" + pageSize)
                .add("maxCodesPerQuery=" + maxCodesPerQuery)
                .add("queryBatchThreshold=" + queryBatchThreshold)
                .add("enableHedisCompatibilityMode=" + enableHedisCompatibilityMode)
                .toString();
    }
}
