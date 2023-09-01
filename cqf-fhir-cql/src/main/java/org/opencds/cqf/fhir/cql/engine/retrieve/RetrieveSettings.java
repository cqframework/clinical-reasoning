package org.opencds.cqf.fhir.cql.engine.retrieve;

public class RetrieveSettings {
    private boolean filterBySearchParam = false;
    private boolean searchByTemplate = false;

    public boolean getFilterBySearchParam() {
        return filterBySearchParam;
    }

    public RetrieveSettings setFilterBySearchParam(boolean filterBySearchParam) {
        this.filterBySearchParam = filterBySearchParam;
        return this;
    }

    public boolean getSearchByTemplate() {
        return searchByTemplate;
    }

    public RetrieveSettings setSearchByTemplate(boolean searchByTemplate) {
        this.searchByTemplate = searchByTemplate;
        return this;
    }
}
