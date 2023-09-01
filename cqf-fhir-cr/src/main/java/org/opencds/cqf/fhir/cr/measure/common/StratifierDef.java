package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StratifierDef {

  private final String id;
  private final ConceptDef code;
  private final String expression;

  private final List<StratifierComponentDef> components;

  private Map<String, CriteriaResult> results;

  public StratifierDef(String id, ConceptDef code, String expression) {
    this(id, code, expression, Collections.emptyList());
  }

  public StratifierDef(String id, ConceptDef code, String expression,
      List<StratifierComponentDef> components) {
    this.id = id;
    this.code = code;
    this.expression = expression;
    this.components = components;
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
    return this.components;
  }

  public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
    this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
  }

  public Map<String, CriteriaResult> getResults() {
    if (this.results == null) {
      this.results = new HashMap<>();
    }

    return this.results;
  }
}
