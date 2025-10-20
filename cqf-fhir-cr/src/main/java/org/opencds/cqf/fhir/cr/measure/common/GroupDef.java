package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S107")
public class GroupDef implements IDef {

    private final String id;
    private final ConceptDef code;
    private final List<StratifierDef> stratifiers;
    private final List<PopulationDef> populations;
    private final MeasureScoring measureScoring;
    private final boolean isGroupImpNotation;
    private final CodeDef populationBasis;
    private final CodeDef improvementNotation;
    private final Map<MeasurePopulationType, List<PopulationDef>> populationIndex;
    private final ContinuousVariableObservationAggregateMethod aggregateMethod;

    public GroupDef(
            String id,
            ConceptDef code,
            List<StratifierDef> stratifiers,
            List<PopulationDef> populations,
            MeasureScoring measureScoring,
            boolean isGroupImprovementNotation,
            CodeDef improvementNotation,
            CodeDef populationBasis) {
        this(
                id,
                code,
                stratifiers,
                populations,
                measureScoring,
                isGroupImprovementNotation,
                improvementNotation,
                populationBasis,
                null);
    }

    public GroupDef(
            String id,
            ConceptDef code,
            List<StratifierDef> stratifiers,
            List<PopulationDef> populations,
            MeasureScoring measureScoring,
            boolean isGroupImprovementNotation,
            CodeDef improvementNotation,
            CodeDef populationBasis,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod) {
        //
        this.id = id;
        this.code = code;
        this.stratifiers = stratifiers;
        this.populations = populations;
        this.populationIndex = index(populations);
        this.measureScoring = measureScoring;
        this.isGroupImpNotation = isGroupImprovementNotation;
        this.improvementNotation = improvementNotation;
        this.populationBasis = populationBasis;
        this.aggregateMethod = aggregateMethod;
    }

    public String id() {
        return this.id;
    }

    public ConceptDef code() {
        return this.code;
    }

    public List<StratifierDef> stratifiers() {
        return this.stratifiers;
    }

    public List<PopulationDef> populations() {
        return this.populations;
    }

    public PopulationDef getSingle(MeasurePopulationType type) {
        if (!populationIndex.containsKey(type)) {
            return null;
        }

        List<PopulationDef> defs = this.populationIndex.get(type);
        if (defs.size() > 1) {
            throw new IllegalStateException("There is more than one PopulationDef of type: " + type.toCode());
        }

        return defs.get(0);
    }

    public List<PopulationDef> get(MeasurePopulationType type) {
        return this.populationIndex.computeIfAbsent(type, x -> Collections.emptyList());
    }

    private Map<MeasurePopulationType, List<PopulationDef>> index(List<PopulationDef> populations) {
        return populations.stream().collect(Collectors.groupingBy(PopulationDef::type));
    }

    public MeasureScoring measureScoring() {
        return this.measureScoring;
    }

    public boolean isIncreaseImprovementNotation() {
        if (getImprovementNotation() != null) {
            return getImprovementNotation().code().equals("increase");
        } else {
            // default response if null
            return true;
        }
    }

    public boolean isGroupImprovementNotation() {
        return this.isGroupImpNotation;
    }

    public boolean isBooleanBasis() {
        return getPopulationBasis().code().equals("boolean");
    }

    public CodeDef getPopulationBasis() {
        return this.populationBasis;
    }

    public CodeDef getImprovementNotation() {
        return this.improvementNotation;
    }

    public ContinuousVariableObservationAggregateMethod getAggregateMethod() {
        return this.aggregateMethod;
    }
}
