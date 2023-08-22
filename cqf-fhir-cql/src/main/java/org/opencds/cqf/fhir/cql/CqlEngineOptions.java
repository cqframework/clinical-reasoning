package org.opencds.cqf.fhir.cql;

import java.util.EnumSet;
import java.util.Set;

import org.opencds.cqf.cql.engine.execution.CqlEngine;

// TODO: Eventually, the cql-engine needs to expose these itself.
public class CqlEngineOptions {
  private Set<CqlEngine.Options> options = EnumSet.noneOf(CqlEngine.Options.class);
  private boolean isDebugLoggingEnabled = false;
  private boolean shouldExpandValueSets = false;
  private Integer pageSize;
  private Integer maxCodesPerQuery;
  private Integer queryBatchThreshold;

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

  public static CqlEngineOptions defaultOptions() {
    CqlEngineOptions result = new CqlEngineOptions();
    result.options.add(CqlEngine.Options.EnableExpressionCaching);
    return result;
  }

}
