package org.opencds.cqf.fhir.cr.measure.common.def.measure;

import java.util.Collections;
import java.util.List;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;

/**
 * Immutable definition of a FHIR Measure Stratifier structure.
 * Contains only the stratifier's structural metadata (id, code, expression, type, components).
 * Does NOT contain evaluation state like stratum or results - use StratifierReportDef for that.
 */
public class StratifierDef {

    private final String id;
    private final ConceptDef code;
    private final String expression;
    private final MeasureStratifierType stratifierType;
    private final List<StratifierComponentDef> components;

    public StratifierDef(String id, ConceptDef code, String expression, MeasureStratifierType stratifierType) {
        this(id, code, expression, stratifierType, Collections.emptyList());
    }

    public StratifierDef(
            String id,
            ConceptDef code,
            String expression,
            MeasureStratifierType stratifierType,
            List<StratifierComponentDef> components) {
        this.id = id;
        this.code = code;
        this.expression = expression;
        this.stratifierType = stratifierType;
        this.components = List.copyOf(components); // Defensive copy for immutability
    }

    public boolean isComponentStratifier() {
        return !components.isEmpty();
    }

    public boolean isCriteriaStratifier() {
        return MeasureStratifierType.CRITERIA == this.stratifierType;
    }

    public String expression() {
        return this.expression;
    }

    public ConceptDef code() {
        return this.code;
    }

    public String id() {
        return this.id;
    }

    public List<StratifierComponentDef> components() {
        return this.components; // Already unmodifiable from List.copyOf()
    }

    public MeasureStratifierType getStratifierType() {
        return stratifierType;
    }
}
