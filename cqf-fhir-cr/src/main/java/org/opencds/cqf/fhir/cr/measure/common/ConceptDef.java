package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.StringJoiner;

public record ConceptDef(List<CodeDef> codes, String text) {

    public boolean isEmpty() {
        return this.codes.isEmpty();
    }

    public CodeDef first() {
        if (isEmpty()) {
            throw new IllegalStateException("No codes in this concept, so can't return first()");
        }

        return this.codes.get(0);
    }

    @Override
    @Nonnull
    public String toString() {
        return new StringJoiner(", ", ConceptDef.class.getSimpleName() + "[", "]")
                .add("codes=" + codes)
                .add("text='" + text + "'")
                .toString();
    }
}
