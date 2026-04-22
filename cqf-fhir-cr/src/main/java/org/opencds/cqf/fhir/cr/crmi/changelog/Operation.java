package org.opencds.cqf.fhir.cr.crmi.changelog;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public class Operation {

    private String type;
    private String path;
    private Object newValue;
    private Object oldValue;

    Operation(String type, String path, Object newValue, Object originalValue) {
        this.type = type;
        this.path = path;
        if (originalValue instanceof IPrimitiveType<?> originalPrimitive) {
            this.oldValue = originalPrimitive.getValue();
        } else if (originalValue instanceof IBase) {
            this.oldValue = originalValue;
        } else if (originalValue != null) {
            this.oldValue = originalValue.toString();
        }
        if (newValue instanceof IPrimitiveType<?> newPrimitive) {
            this.newValue = newPrimitive.getValue();
        } else if (newValue instanceof IBase) {
            this.newValue = newValue;
        } else if (newValue != null) {
            this.newValue = newValue.toString();
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }
}
