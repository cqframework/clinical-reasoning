package org.opencds.cqf.fhir.cr.crmi.changelog;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ValueAndOperation {

    private String value;
    private Operation operation;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        if (operation != null) {
            if (this.operation != null
                    && this.operation.getType().equals(operation.getType())
                    && this.operation.getPath().equals(operation.getPath())
                    && this.operation.getNewValue() != operation.getNewValue()) {
                throw new UnprocessableEntityException("Multiple changes to the same element");
            }
            this.operation = operation;
        }
    }
}
