package org.opencds.cqf.fhir.cql;

import ca.uhn.fhir.rest.api.SearchStyleEnum;

public class RetrieveProviderConfig {

  private static RetrieveProviderConfig DEFAULT_CONFIG = new RetrieveProviderConfig();

  public static RetrieveProviderConfig defaultConfig() {
    return DEFAULT_CONFIG;
  }

  private Integer maxCodesPerQuery = 64;
  private SearchStyleEnum searchStyle = SearchStyleEnum.GET;
  private boolean expandValueSets = true;

  public Integer getMaxCodesPerQuery() {
    return this.maxCodesPerQuery;
  }

  public RetrieveProviderConfig setMaxCodesPerQuery(Integer value) {
    this.maxCodesPerQuery = value;
    return this;
  }

  public SearchStyleEnum getSearchStyle() {
    return this.searchStyle;
  }

  public RetrieveProviderConfig setSearchStyle(SearchStyleEnum value) {
    this.searchStyle = value;
    return this;
  }

  public boolean getExpandValueSets() {
    return this.expandValueSets;
  }

  public RetrieveProviderConfig setExpandValueSets(boolean value) {
    this.expandValueSets = value;
    return this;
  }
}
