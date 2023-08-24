package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem;

import static org.opencds.cqf.cql.evaluator.questionnaire.r4.ItemValueTransformer.transformValue;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedQuestionnaireItemService {
  protected final ModelResolver modelResolver;
  protected final QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
  protected final ElementHasDefaultValue elementHasDefaultValue;
  protected final ElementHasCqfExpression elementHasCqfExpression;
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
        elementHasCqfExpression,
        new FhirModelResolverFactory()
            .create(repository.fhirContext().getVersion().getVersion().getFhirVersionString()));
  }

  NestedQuestionnaireItemService(
      QuestionnaireTypeIsChoice questionnaireTypeIsChoice,
      ElementHasDefaultValue elementHasDefaultValue,
      ElementHasCqfExpression elementHasCqfExpression,
      ModelResolver modelResolver) {
    this.questionnaireTypeIsChoice = questionnaireTypeIsChoice;
    this.elementHasDefaultValue = elementHasDefaultValue;
    this.elementHasCqfExpression = elementHasCqfExpression;
    this.modelResolver = modelResolver;
  }

  public QuestionnaireItemComponent getNestedQuestionnaireItem(
      String profileUrl,
      ElementDefinition element,
      String childLinkId,
      Resource caseFeature) {
    final QuestionnaireItemType itemType = getItemType(element);
    final QuestionnaireItemComponent item =
        initializeQuestionnaireItem(itemType, profileUrl, element, childLinkId);
    if (itemType == QuestionnaireItemType.CHOICE) {
      questionnaireTypeIsChoice.addProperties(element, item);
    }
    if (element.hasFixedOrPattern()) {
      elementHasDefaultValue.addProperties(element.getFixedOrPattern(), item);
    } else if (element.hasDefaultValue()) {
      elementHasDefaultValue.addProperties(element.getDefaultValue(), item);
    } else if (element.hasExtension(Constants.CQF_EXPRESSION)) {
      elementHasCqfExpression.addProperties(element, item);
    } else if (caseFeature != null) {
      var path = element.getPath().split("\\.")[1].replace("[x]", "");
      var pathValue = modelResolver.resolvePath(caseFeature, path);
      if (pathValue instanceof Type) {
        item.addInitial().setValue(transformValue((Type) pathValue));
      }
    }
    item.setRequired(element.hasMin() && element.getMin() == 1);
    // set repeat based on? if expression result type is a list?
    // set enableWhen based on? use
    // http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-enableWhenExpression
    return item;
  }

  protected QuestionnaireItemComponent initializeQuestionnaireItem(
      QuestionnaireItemType itemType,
      String profileUrl,
      ElementDefinition element,
      String childLinkId) {
    final String definition = profileUrl + "#" + element.getPath();
    return new QuestionnaireItemComponent()
        .setType(itemType)
        .setDefinition(definition)
        .setLinkId(childLinkId)
        .setText(getElementText(element));
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
      case "uri":
        return QuestionnaireItemType.URL;
      case "BackboneElement":
        return QuestionnaireItemType.GROUP;
      case "Quantity":
        return QuestionnaireItemType.QUANTITY;
      case "Reference":
        return QuestionnaireItemType.REFERENCE;
      case "code":
        return QuestionnaireItemType.STRING;
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
