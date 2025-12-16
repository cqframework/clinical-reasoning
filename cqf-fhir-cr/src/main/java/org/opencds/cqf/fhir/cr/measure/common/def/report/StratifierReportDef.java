package org.opencds.cqf.fhir.cr.measure.common.def.report;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.HashSetForFhirResourcesAndCqlTypes;
import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.def.measure.StratifierDef;

public class StratifierReportDef {

    private final StratifierDef stratifierDef; // Reference to immutable structure
    private final List<StratifierComponentReportDef> components; // Converted from StratifierDef for evaluation
    private final List<StratumReportDef> stratum = new ArrayList<>(); // Evaluation-only state

    @Nullable
    private Map<String, CriteriaResult> results; // Evaluation-only state

    /**
     * Factory method to create StratifierReportDef from immutable StratifierDef.
     * The StratifierReportDef will have empty mutable state (stratum list, results map).
     */
    public static StratifierReportDef fromStratifierDef(StratifierDef stratifierDef) {
        List<StratifierComponentReportDef> componentReportDefs = stratifierDef.components().stream()
                .map(StratifierComponentReportDef::fromStratifierComponentDef)
                .toList();
        return new StratifierReportDef(stratifierDef, componentReportDefs);
    }

    /**
     * Constructor for creating StratifierReportDef with a StratifierDef reference.
     * This is the primary constructor for production use.
     */
    public StratifierReportDef(StratifierDef stratifierDef, List<StratifierComponentReportDef> components) {
        this.stratifierDef = stratifierDef;
        this.components = components;
    }

    /**
     * Test-only constructor for creating StratifierReportDef with explicit structural data (4 params).
     * Creates a minimal StratifierDef internally. Use fromStratifierDef() for production code.
     */
    public StratifierReportDef(String id, ConceptDef code, String expression, MeasureStratifierType stratifierType) {
        this(id, code, expression, stratifierType, Collections.emptyList());
    }

    /**
     * Test-only constructor for creating StratifierReportDef with explicit structural data (5 params).
     * Creates a minimal StratifierDef internally. Use fromStratifierDef() for production code.
     */
    public StratifierReportDef(
            String id,
            ConceptDef code,
            String expression,
            MeasureStratifierType stratifierType,
            List<StratifierComponentReportDef> components) {
        this.stratifierDef = new StratifierDef(id, code, expression, stratifierType, Collections.emptyList());
        this.components = components;
    }

    /**
     * Accessor for the immutable structural definition.
     */
    public StratifierDef stratifierDef() {
        return this.stratifierDef;
    }

    // Delegate structural queries to stratifierDef
    public boolean isComponentStratifier() {
        return !components.isEmpty();
    }

    public boolean isCriteriaStratifier() {
        return MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType();
    }

    public String expression() {
        return stratifierDef.expression();
    }

    public ConceptDef code() {
        return stratifierDef.code();
    }

    public String id() {
        return stratifierDef.id();
    }

    public List<StratumReportDef> getStratum() {
        return stratum;
    }

    public void addAllStratum(List<StratumReportDef> stratumDefs) {
        stratum.addAll(stratumDefs);
    }

    public List<StratifierComponentReportDef> components() {
        return this.components;
    }

    public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
        this.getResults()
                .put(subject, new CriteriaResult(value, new HashSetForFhirResourcesAndCqlTypes<>(evaluatedResources)));
    }

    public Map<String, CriteriaResult> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }

    // Ensure we handle FHIR resource identity properly
    public Set<Object> getAllCriteriaResultValues() {
        return new HashSetForFhirResourcesAndCqlTypes<>(this.getResults().values().stream()
                .map(CriteriaResult::rawValue)
                .map(this::toSet)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet()));
    }

    public MeasureStratifierType getStratifierType() {
        return stratifierDef.getStratifierType();
    }

    private Set<Object> toSet(Object value) {
        if (value == null) {
            return Set.of();
        }

        if (value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toUnmodifiableSet());
        } else {
            return Set.of(value);
        }
    }
}
