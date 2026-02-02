package org.opencds.cqf.fhir.cr.measure.common;

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

public class StratifierDef {

    private final String id;
    private final ConceptDef code;
    private final String expression;
    private final MeasureStratifierType stratifierType;

    private final List<StratifierComponentDef> components;
    private final List<StratumDef> stratum = new ArrayList<>();

    @Nullable
    private Map<String, CriteriaResult> results;

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
        this.components = components;
    }

    public boolean isComponentStratifier() {
        return !components.isEmpty();
    }

    public boolean isCriteriaStratifier() {
        return MeasureStratifierType.CRITERIA == this.stratifierType;
    }

    public boolean isNonSubjectValueStratifier() {
        return MeasureStratifierType.NON_SUBJECT_VALUE == this.stratifierType;
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

    public List<StratumDef> getStratum() {
        return stratum;
    }

    public void addAllStratum(List<StratumDef> stratumDefs) {
        stratum.addAll(stratumDefs);
    }

    public List<StratifierComponentDef> components() {
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
        return stratifierType;
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
