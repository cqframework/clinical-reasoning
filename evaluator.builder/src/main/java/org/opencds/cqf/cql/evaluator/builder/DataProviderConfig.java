package org.opencds.cqf.cql.evaluator.builder;

import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

import ca.uhn.fhir.rest.api.SearchStyleEnum;

public class DataProviderConfig {

    private static DataProviderConfig DEFAULT_CONFIG = new DataProviderConfig();

    public static DataProviderConfig defaultConfig() {
        return DEFAULT_CONFIG;
    }

    private int maxCodesPerQuery = 64;
    private SearchStyleEnum searchStyle = SearchStyleEnum.GET;
    private boolean expandValueSets = true;
    private TerminologyProvider terminologyProvider;

    public int getMaxCodesPerQuery() {
        return this.maxCodesPerQuery;
    }

    public DataProviderConfig setMaxCodesPerQuery(int value) {
        this.maxCodesPerQuery = value;
        return this;
    }

    public SearchStyleEnum getSearchStyle() {
        return this.searchStyle;
    }

    public DataProviderConfig setSearchStyle(SearchStyleEnum value) {
        this.searchStyle = value;
        return this;
    }

    public boolean getExpandValueSets() {
        return this.expandValueSets;
    }

    public DataProviderConfig setExpandValueSets(boolean value) {
        this.expandValueSets = value;
        return this;
    }

    public TerminologyProvider getTerminologyProvider() {
        return this.terminologyProvider;
    }

    public DataProviderConfig setTerminologyProvider(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
        return this;
    }
}