package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * All continuous variable scoring aggregation methods.
 */
public enum ContinuousVariableObservationAggregateMethod {
    AVG("average"),
    COUNT("count"),
    MAX("maximum"),
    MEDIAN("median"),
    MIN("minimum"),
    SUM("sum"),
    N_A(null);

    @Nullable
    private final String text;

    ContinuousVariableObservationAggregateMethod(@Nullable String text) {
        this.text = text;
    }

    @Nullable
    public String getText() {
        return text;
    }

    @Nullable
    public static ContinuousVariableObservationAggregateMethod fromString(@Nullable String text) {
        for (ContinuousVariableObservationAggregateMethod value : values()) {
            if (Objects.equals(text, value.getText())) {
                return value;
            }
        }

        return null;
    }
}
