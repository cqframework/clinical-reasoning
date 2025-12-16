package org.opencds.cqf.fhir.cr.measure.common.def.report;

import jakarta.annotation.Nonnull;
import java.util.StringJoiner;

/**
 * Capture results of component stratifier stratum calculation.
 */
public record StratumValueReportDef(StratumValueWrapperReportDef value, StratifierComponentReportDef def) {

    @Override
    @Nonnull
    public String toString() {
        return new StringJoiner(", ", StratumValueReportDef.class.getSimpleName() + "[", "]")
                .add("value=" + value)
                .add("def=" + def)
                .toString();
    }
}
