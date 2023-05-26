package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.TypeRefComponent;
import javax.annotation.Nonnull;
import java.util.List;

public class TestingHelper {
  final static String PROFILE_URL = "http://www.sample.com/profile/profileId";

  @Nonnull
  public static ElementDefinition withElementDefinition(String typeCode, String pathValue) {
    final ElementDefinition elementDefinition = new ElementDefinition().setPath(pathValue);
    final TypeRefComponent type = new TypeRefComponent();
    type.setCode(typeCode);
    elementDefinition.setType(List.of(type));
    return elementDefinition;
  }

  @Nonnull
  public static DataRequirement withActionInput() {
    final DataRequirement actionInput = new DataRequirement();
    final CanonicalType canonicalType = new CanonicalType();
    canonicalType.setValue(PROFILE_URL);
    actionInput.setProfile(List.of(canonicalType));
    return actionInput;
  }

}
