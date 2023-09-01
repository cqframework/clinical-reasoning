package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupDef {

  private final String id;
  private final ConceptDef code;
  private final List<StratifierDef> stratifiers;
  private final List<PopulationDef> populations;

  private final Map<MeasurePopulationType, List<PopulationDef>> populationIndex;

  public GroupDef(String id, ConceptDef code, List<StratifierDef> stratifiers,
      List<PopulationDef> populations) {
    this.id = id;
    this.code = code;
    this.stratifiers = stratifiers;
    this.populations = populations;
    this.populationIndex = index(populations);
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
      throw new IllegalStateException(
          "There is more than one PopulationDef of type: " + type.toCode());
    }

    return defs.get(0);
  }

  public List<PopulationDef> get(MeasurePopulationType type) {
    return this.populationIndex.computeIfAbsent(type, x -> Collections.emptyList());
  }

  private Map<MeasurePopulationType, List<PopulationDef>> index(List<PopulationDef> populations) {
    return populations.stream().collect(Collectors.groupingBy(PopulationDef::type));
  }
}
