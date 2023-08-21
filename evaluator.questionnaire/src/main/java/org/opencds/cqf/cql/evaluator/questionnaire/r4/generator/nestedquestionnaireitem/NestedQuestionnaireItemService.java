package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedQuestionnaireItemService {
  protected QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
  protected ElementHasDefaultValue elementHasDefaultValue;
  protected ElementHasCqfExpression elementHasCqfExpression;
  protected static final String ITEM_TYPE_ERROR = "Unable to determine type for element: %s";
  protected static final Logger logger =
      LoggerFactory.getLogger(NestedQuestionnaireItemService.class);

  public static NestedQuestionnaireItemService of(
      Repository repository,
      String patientId,
      IBaseParameters parameters,
      IBaseBundle bundle,
      LibraryEngine libraryEngine) {
    final QuestionnaireTypeIsChoice questionnaireTypeIsChoice =
        QuestionnaireTypeIsChoice.of(repository);
    final ElementHasDefaultValue elementHasDefault = new ElementHasDefaultValue();
    final ElementHasCqfExpression elementHasCqfExpression = new ElementHasCqfExpression(
        patientId,
        parameters,
        bundle,
        libraryEngine);
    return new NestedQuestionnaireItemService(
        questionnaireTypeIsChoice,
        elementHasDefault,
        elementHasCqfExpression);
  }

  NestedQuestionnaireItemService(
      QuestionnaireTypeIsChoice questionnaireTypeIsChoice,
      ElementHasDefaultValue elementHasDefaultValue,
      ElementHasCqfExpression elementHasCqfExpression) {
    this.questionnaireTypeIsChoice = questionnaireTypeIsChoice;
    this.elementHasDefaultValue = elementHasDefaultValue;
    this.elementHasCqfExpression = elementHasCqfExpression;
  }

  public QuestionnaireItemComponent getNestedQuestionnaireItem(
      StructureDefinition profile,
      ElementDefinition element,
      String childLinkId) {
    final QuestionnaireItemType itemType = getItemType(element);
    QuestionnaireItemComponent item =
        initializeQuestionnaireItem(itemType, profile, element, childLinkId);
    if (itemType == QuestionnaireItemType.CHOICE) {
      item = questionnaireTypeIsChoice.addProperties(element, item);
    }
    if (element.hasFixedOrPattern()) {
      item = elementHasDefaultValue.addProperties(element.getFixedOrPattern(), item);
    } else if (element.hasDefaultValue()) {
      item = elementHasDefaultValue.addProperties(element.getDefaultValue(), item);
    } else if (element.hasExtension(Constants.CQF_EXPRESSION)) {
      item = elementHasCqfExpression.addProperties(element, item);
    }
    item.setRequired(element.hasMin() && element.getMin() == 1);
    // set repeat based on? if expression result type is a list?
    // set enableWhen based on? use
    // http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-enableWhenExpression
    return item;
  }

  protected QuestionnaireItemComponent initializeQuestionnaireItem(
      QuestionnaireItemType itemType,
      StructureDefinition profile,
      ElementDefinition element,
      String childLinkId) {
    final String definition = profile.getUrl() + "#" + element.getPath();
    final String childText = getElementText(element);
    return new QuestionnaireItemComponent()
        .setType(itemType)
        .setDefinition(definition)
        .setLinkId(childLinkId)
        .setText(childText);
  }

  public QuestionnaireItemType getItemType(ElementDefinition element) {
    final String elementType = element.getType().get(0).getCode();
    final QuestionnaireItemType itemType = parseItemType(elementType, element.hasBinding());
    if (itemType == null) {
      final String message = String.format(ITEM_TYPE_ERROR, element.getId());
      throw new IllegalArgumentException(message);
    }
    return itemType;
  }

  public QuestionnaireItemType parseItemType(String elementType, Boolean hasBinding) {
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
    return element.hasLabel() ? element.getLabel() : getElementDescription(element);
  }

  protected String getElementDescription(ElementDefinition element) {
    return element.hasShort() ? element.getShort() : element.getPath();
  }
}
