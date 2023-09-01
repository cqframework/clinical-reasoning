package org.opencds.cqf.fhir.cr.questionnaire.dstu3;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Type;

public class ItemValueTransformer {
  private ItemValueTransformer() {}

  public static Type transformValue(Type value) {
    if (value instanceof CodeableConcept) {
      return ((CodeableConcept) value).getCoding().get(0);
    }

    return value;
  }
}
