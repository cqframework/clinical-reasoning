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
    final String definition = getDefinition(profile);
    String text = getProfileText(profile);
    if (actionInput.hasExtension(Constants.CPG_INPUT_TEXT)) {
      text = actionInput.getExtensionString(Constants.CPG_INPUT_TEXT);
    }
    var item = createQuestionnaireItemComponent(text, linkId, definition);
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

  protected String getProfileText(StructureDefinition profile) {
    return profile.hasTitle() ? profile.getTitle()
        : profile.getUrl().substring(profile.getUrl().lastIndexOf("/") + 1);
  }

  protected QuestionnaireItemComponent createQuestionnaireItemComponent(String text, String linkId,
      String definition) {
    return new QuestionnaireItemComponent()
        .setType(Questionnaire.QuestionnaireItemType.GROUP)
        .setDefinition(definition)
        .setLinkId(linkId)
        .setText(text);
  }

  protected String getDefinition(StructureDefinition profile) {
    return String.format("%s#%s", profile.getUrl(), profile.getType());
  }
}
