package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.questionnaireitem;

import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.cql.evaluator.fhir.Constants;

public class QuestionnaireItemService {
  public QuestionnaireItemComponent getQuestionnaireItem(
      DataRequirement actionInput,
      String linkId,
      StructureDefinition profile
  ) {
    final String profileUrl = getProfileUrl(actionInput);
    final String text = getProfileText(profileUrl, profile);
    final Extension extension = createExtension(profile);
    final QuestionnaireItemComponent questionnaireItemComponent = createQuestionnaireItemComponent(text, linkId);
    questionnaireItemComponent.addExtension(extension);
    return questionnaireItemComponent;
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

  protected Extension createExtension(StructureDefinition profile) {
    final Type codeType = new CodeType().setValue(profile.getType());
    return new Extension()
        .setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
        .setValue(codeType);
  }

  protected String getProfileUrl(DataRequirement actionInput) {
    return actionInput.getProfile().get(0).getValue();
  }
}
