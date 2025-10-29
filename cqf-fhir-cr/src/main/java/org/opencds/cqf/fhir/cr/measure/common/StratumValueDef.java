package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import java.util.StringJoiner;

/**
 * Capture results of component stratifier stratum calculation.
 */
public record StratumValueDef(StratumValueWrapper value, StratifierComponentDef def) {

    @Override
    @Nonnull
    public String toString() {
        return new StringJoiner(", ", StratumValueDef.class.getSimpleName() + "[", "]")
                .add("value=" + value)
                .add("def=" + def)
                .toString();
    }
}
