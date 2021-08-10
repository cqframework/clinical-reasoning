package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

public class GroupDef {

    private HashMap<MeasurePopulationType, PopulationDef> populationSets = new HashMap<>();

    private List<StratifierDef> stratifiers;

    public PopulationDef get(MeasurePopulationType populationType) {
        return this.populationSets.get(populationType);
    }

    public PopulationDef createPopulation(MeasurePopulationType measurePopulationType, String criteriaExpression) {
        return this.populationSets.put(measurePopulationType, new PopulationDef(measurePopulationType, criteriaExpression));
    }

    public Set<MeasurePopulationType> keys() {
        return this.populationSets.keySet();
    }

    public Collection<PopulationDef> values() {
        return this.populationSets.values();
    }

    public Set<Entry<MeasurePopulationType, PopulationDef>> entrySet() {
        return this.populationSets.entrySet();
    }

    public List<StratifierDef> getStratifiers() {
        if (this.stratifiers == null) {
            this.stratifiers = new ArrayList<>();
        }

        return this.stratifiers;
    }
}
