package org.opencds.cqf.cql.evaluator.fhir.util;

import java.util.List;

public class ValidationProfile {
    private String name;
    private List<String> ignoreKeys;

    public ValidationProfile() { }

    public ValidationProfile(String name, List<String> ignoreKeys) {
        this.name = name;
        this.ignoreKeys = ignoreKeys;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getIgnoreKeys() {
        return ignoreKeys;
    }

    public void setIgnoreKeys(List<String> ignoreKeys) {
        this.ignoreKeys = ignoreKeys;
    }

    public void addIgnoreKey(String key) {
        this.ignoreKeys.add(key);
    }
}
