package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.nestedquestionnaireitem;

import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.exceptions.QuestionnaireParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedQuestionnaireItemService {
  protected QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
  protected ElementIsFixedOrHasPattern elementIsFixedOrHasPattern;
  protected ElementHasCqfExtension elementHasCqfExtension;
  protected static final String ITEM_TYPE_ERROR = "Unable to determine type for element: %s";
  protected static final Logger logger = LoggerFactory.getLogger(NestedQuestionnaireItemService.class);

  public QuestionnaireItemComponent getNestedQuestionnaireItem(
      StructureDefinition profile,
      ElementDefinition element,
      String childLinkId
  ) throws QuestionnaireParsingException {
    final QuestionnaireItemType itemType = getItemType(element);
    QuestionnaireItemComponent item = initializeQuestionnaireItem(itemType, profile, element, childLinkId);
    if (itemType == QuestionnaireItemType.CHOICE) {
      item = questionnaireTypeIsChoice.addProperties(element, item);
    }
    if (element.hasFixed() || element.hasPattern()) {
      item = elementIsFixedOrHasPattern.addProperties(element, item);
    } else if (element.hasDefaultValue()) {
      item = item.setInitial(element.getDefaultValue());
    } else if (element.hasExtension(Constants.CQF_EXPRESSION)) {
      item = elementHasCqfExtension.addProperties(item);
    }
    item.setRequired(element.hasMin() && element.getMin() == 1);
    // set readonly based on?
    // set repeat based on?
    // set enableWhen based on?
    return item;
  }

  protected QuestionnaireItemComponent initializeQuestionnaireItem(
      QuestionnaireItemType itemType,
      StructureDefinition profile,
      ElementDefinition element,
      String childLinkId
  ) {
    final String definition = profile.getUrl() + "#" + element.getPath();
    final String childText = getElementText(element);
    return new QuestionnaireItemComponent()
        .setType(itemType)
        .setDefinition(definition)
        .setLinkId(childLinkId)
        .setText(childText);
  }

  protected QuestionnaireItemType getItemType(ElementDefinition element) throws QuestionnaireParsingException {
    final String elementType = element.getType().get(0).getCode();
    final QuestionnaireItemType itemType = getItemType(elementType, element.hasBinding());
    if (itemType == null) {
      final String message = String.format(ITEM_TYPE_ERROR, element.getId());
      throw new QuestionnaireParsingException(message);
    }
    return itemType;
  }

  protected static QuestionnaireItemType getItemType(String elementType, Boolean hasBinding) {
    if (Boolean.TRUE.equals(hasBinding)) {
      return QuestionnaireItemType.CHOICE;
    }
    switch (elementType) {
      case "CodeableConcept":
        return QuestionnaireItemType.CHOICE;
      case "Reference":
      case "uri":
        return QuestionnaireItemType.STRING;
      case "BackboneElement":
        return QuestionnaireItemType.GROUP;
      default:
        return QuestionnaireItemType.fromCode(elementType);
    }
  }

  public String getElementText(ElementDefinition element) {
    return element.hasLabel() ? element.getLabel() : getElementDefinition(element);
  }

  protected String getElementDefinition(ElementDefinition element) {
    return element.hasShort() ? element.getShort() : element.getPath();
  }
}
