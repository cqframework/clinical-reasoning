package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.generator.questionnaireitem;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.opencds.cqf.cql.evaluator.fhir.Constants;

public class QuestionnaireItemService {
  public QuestionnaireItemComponent createQuestionnaireItem(
      DataRequirement actionInput,
      String linkId,
      StructureDefinition profile) {
    final String profileUrl = getProfileUrl(actionInput);
    String text = getProfileText(profileUrl, profile);
    if (actionInput.hasExtension(Constants.CPG_INPUT_TEXT)) {
      text = actionInput.getExtensionString(Constants.CPG_INPUT_TEXT);
    }
    var item = createQuestionnaireItemComponent(text, linkId, profileUrl);
    item.setExtension(resolveExtensions(actionInput.getExtension(), profile.getExtension()));
    return item;
  }

  protected List<Extension> resolveExtensions(List<Extension> inputExtensions,
      List<Extension> profileExtensions) {
    var extensions = new ArrayList<Extension>();
    inputExtensions.forEach(ext -> {
      if (ext.getUrl().equals(Constants.CPG_INPUT_DESCRIPTION)
          && extensions.stream().noneMatch(e -> e.getUrl().equals(ext.getUrl()))) {
        extensions.add(ext);
      }
    });
    // profileExtensions.forEach(ext -> {
    // });
    // extensions.forEach(ext -> {
    // });

    return extensions.isEmpty() ? null : extensions;
  }

  protected String getProfileText(String profileUrl,
      StructureDefinition profile) {
    return profile.hasTitle() ? profile.getTitle()
        : profileUrl.substring(profileUrl.lastIndexOf("/") + 1);
  }

  protected QuestionnaireItemComponent createQuestionnaireItemComponent(String text, String linkId,
      String profileUrl) {
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
