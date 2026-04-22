package org.opencds.cqf.fhir.cr.crmi.changelog;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class Page<T extends PageBase> {

    private final T oldData;
    private final T newData;
    private String url;
    private String resourceType;

    public T getOldData() {
        return oldData;
    }

    public T getNewData() {
        return newData;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    Page(String url, T oldData, T newData) {
        this.url = url;
        this.oldData = oldData;
        this.newData = newData;
        if (oldData != null && oldData.getResourceType() != null) {
            this.resourceType = oldData.getResourceType();
        } else if (newData != null && newData.getResourceType() != null) {
            this.resourceType = newData.getResourceType();
        }
    }

    public void addOperation(String type, String path, Object currentValue, Object originalValue) {
        if (type != null) {
            switch (type) {
                case ChangeLog.REPLACE -> addReplaceOperation(type, path, currentValue, originalValue);
                case ChangeLog.DELETE -> addDeleteOperation(type, path, originalValue);
                case ChangeLog.INSERT -> addInsertOperation(type, path, currentValue);
                default ->
                    throw new UnprocessableEntityException(
                            "Unknown type provided when adding an operation to the ChangeLog");
            }
        } else {
            throw new UnprocessableEntityException("Type must be provided when adding an operation to the ChangeLog");
        }
    }

    void addInsertOperation(String type, String path, Object currentValue) {
        if (!type.equals(ChangeLog.INSERT)) {
            throw new UnprocessableEntityException(ChangeLog.WRONG_TYPE);
        }
        this.newData.addOperation(type, path, currentValue, null);
    }

    void addDeleteOperation(String type, String path, Object originalValue) {
        if (!type.equals(ChangeLog.DELETE)) {
            throw new UnprocessableEntityException(ChangeLog.WRONG_TYPE);
        }
        this.oldData.addOperation(type, path, null, originalValue);
    }

    void addReplaceOperation(String type, String path, Object currentValue, Object originalValue) {
        if (!type.equals(ChangeLog.REPLACE)) {
            throw new UnprocessableEntityException(ChangeLog.WRONG_TYPE);
        }
        this.oldData.addOperation(type, path, currentValue, null);
        this.newData.addOperation(type, path, null, originalValue);
    }
}
