package org.opencds.cqf.fhir.cr.crmi.changelog;

import org.apache.commons.lang3.StringUtils;

public class PageBase {

    private final ValueAndOperation title = new ValueAndOperation();
    private final ValueAndOperation id = new ValueAndOperation();
    private final ValueAndOperation version = new ValueAndOperation();
    private final ValueAndOperation name = new ValueAndOperation();

    public ValueAndOperation getTitle() {
        return title;
    }

    public ValueAndOperation getId() {
        return id;
    }

    public ValueAndOperation getVersion() {
        return version;
    }

    public ValueAndOperation getName() {
        return name;
    }

    public ValueAndOperation getUrl() {
        return url;
    }

    public String getResourceType() {
        return resourceType;
    }

    private final ValueAndOperation url = new ValueAndOperation();
    private final String resourceType;

    PageBase(String title, String id, String version, String name, String url, String resourceType) {
        if (!StringUtils.isEmpty(title)) {
            this.title.setValue(title);
        }
        if (!StringUtils.isEmpty(id)) {
            this.id.setValue(id);
        }
        if (!StringUtils.isEmpty(version)) {
            this.version.setValue(version);
        }
        if (!StringUtils.isEmpty(name)) {
            this.name.setValue(name);
        }
        if (!StringUtils.isEmpty(url)) {
            this.url.setValue(url);
        }
        this.resourceType = resourceType;
    }

    public void addOperation(String type, String path, Object currentValue, Object originalValue) {
        if (type != null) {
            var newOp = new Operation(type, path, currentValue, originalValue);
            if (path.equals("id")) {
                this.id.setOperation(newOp);
            } else if (path.contains("title")) {
                this.title.setOperation(newOp);
            } else if (path.equals("version")) {
                this.version.setOperation(newOp);
            } else if (path.equals("name")) {
                this.name.setOperation(newOp);
            } else if (path.equals("url")) {
                this.url.setOperation(newOp);
            }
        }
    }
}
