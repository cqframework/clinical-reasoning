package org.opencds.cqf.cql.evaluator.questionnaire.r4.questionnaireitem;

import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.StructureDefinition;

public class QuestionnaireItemService {
  public QuestionnaireItemComponent getQuestionnaireItem(
      DataRequirement actionInput,
      String linkId,
      StructureDefinition profile
  ) {
    final String profileUrl = getProfileUrl(actionInput);
    final String text = getProfileText(profileUrl, profile);
    // ROSIE TODO: WE ARE NOT ADDING AN EXTENSION HERE LIKE IN R4
    return createQuestionnaireItemComponent(text, linkId);
  }

  protected String getProfileText(String profileUrl, StructureDefinition profile) {
    // TODO: define an extension for the text?
    return profile.hasTitle() ?
        profile.getTitle() :
        profileUrl.substring(profileUrl.lastIndexOf("/") + 1);
  }

  protected QuestionnaireItemComponent createQuestionnaireItemComponent(String text, String linkId) {
    return new QuestionnaireItemComponent()
        .setType(Questionnaire.QuestionnaireItemType.GROUP)
        .setLinkId(linkId)
        .setText(text);
  }

  protected String getProfileUrl(DataRequirement actionInput) {
    return actionInput.getProfile().get(0).getValue();
  }
}
