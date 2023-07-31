package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Type;

public class ItemValueTransformer {
  private ItemValueTransformer() {}

  public static Type transformValue(Type value) {
    if (value instanceof CodeableConcept) {
      return ((CodeableConcept) value).getCoding().get(0);
    }

    return value;
  }
}
