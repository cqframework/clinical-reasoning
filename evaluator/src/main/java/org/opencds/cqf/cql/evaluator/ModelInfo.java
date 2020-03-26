package org.opencds.cqf.cql.evaluator;

public class ModelInfo {
    public ModelInfo() {}
    public ModelInfo(String name, String url, String version) {
        this.name = name;
        this.url = url;
        this.version = version;
    }

    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String version;

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private String url;

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}