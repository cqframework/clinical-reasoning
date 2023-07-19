package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.questionnaireitem;

import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.StructureDefinition;

public class QuestionnaireItemService {
  public QuestionnaireItemComponent createQuestionnaireItem(
      DataRequirement actionInput,
      String linkId,
      StructureDefinition profile
  ) {
    final String profileUrl = getProfileUrl(actionInput);
    final String text = getProfileText(profileUrl, profile);
    return createQuestionnaireItemComponent(text, linkId, profileUrl);
  }

  protected String getProfileText(String profileUrl, StructureDefinition profile) {
    return profile.hasTitle() ?
        profile.getTitle() :
        profileUrl.substring(profileUrl.lastIndexOf("/") + 1);
  }

  protected QuestionnaireItemComponent createQuestionnaireItemComponent(String text, String linkId, String profileUrl) {
    return new QuestionnaireItemComponent()
        .setType(Questionnaire.QuestionnaireItemType.GROUP)
        .setDefinition(profileUrl)
        .setLinkId(linkId)
        .setText(text);
  }

  protected String getProfileUrl(DataRequirement actionInput) {
    return actionInput.getProfile().get(0).getValue();
  }
}
