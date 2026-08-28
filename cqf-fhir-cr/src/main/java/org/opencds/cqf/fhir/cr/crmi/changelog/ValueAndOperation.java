package org.opencds.cqf.fhir.cr.crmi.changelog;

import org.hl7.fhir.r4.model.RelatedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueAndOperation {

    private static final Logger logger = LoggerFactory.getLogger(ValueAndOperation.class);

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
        if (operation == null) {
            return;
        }
        // This used to throw "Multiple changes to the same element". It cannot tell a genuine conflict
        // from two elements of one collection. Aborting $create-changelog over that is wrong.
        // Keep the first operation and let the rest of the changelog build.
        if (this.operation != null
                && this.operation.getType().equals(operation.getType())
                && this.operation.getPath().equals(operation.getPath())) {
            logger.warn(
                    "Ignoring a repeat operation on element {}: type={} path={} kept={} ignored={}",
                    this.value,
                    operation.getType(),
                    operation.getPath(),
                    describe(this.operation.getNewValue()),
                    describe(operation.getNewValue()));
            return;
        }
        this.operation = operation;
    }

    private static String describe(Object value) {
        if (value instanceof RelatedArtifact relatedArtifact) {
            return "%s %s"
                    .formatted(
                            relatedArtifact.hasType()
                                    ? relatedArtifact.getType().toCode()
                                    : "(no type)",
                            relatedArtifact.getResource());
        }
        return String.valueOf(value);
    }
}
