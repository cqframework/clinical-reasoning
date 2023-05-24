package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.questionnaireitem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.bundle.BundleParser;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.exceptions.QuestionnaireParsingException;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.nestedquestionnaireitem.NestedQuestionnaireItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionnaireItemGenerator {
  protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireItemGenerator.class);
  protected BundleParser bundleParser;
  protected QuestionnaireItemComponent questionnaireItem;
  protected QuestionnaireItemService questionnaireItemService;
  protected NestedQuestionnaireItemService nestedQuestionnaireItemService;
  protected List<String> paths = new ArrayList<>();
  protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
  protected static final String ITEM_CREATION_ERROR = "An error occurred during item creation: %s";
  protected static final String CHILD_LINK_ID_FORMAT = "%s.%s";

  public Questionnaire.QuestionnaireItemComponent generateItem(
      DataRequirement actionInput,
      int itemCount
  ) {
    if (!actionInput.hasProfile()) {
      throw new IllegalArgumentException(NO_PROFILE_ERROR);
    }
    final String linkId = String.valueOf(itemCount + 1);
    try {
      final StructureDefinition profile = bundleParser.getProfileDefinition(actionInput);
      this.questionnaireItem = questionnaireItemService.getQuestionnaireItem(actionInput, linkId, profile);
      processElements(profile);
      // Should we do this?
      // var requiredElements = profile.getSnapshot().getElement().stream()
      // .filter(e -> !paths.contains(e.getPath()) && e.getPath().split("\\.").length == 2 &&
      // e.getMin() > 0).collect(Collectors.toList());
      // processElements(requiredElements, profile, item, paths);
    } catch (Exception ex) {
      final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
      logger.error(message);
      return createErrorItem(linkId, message);
    }
    return questionnaireItem;
  }

  protected void processElements(
      StructureDefinition profile
  ) {
    int childCount = questionnaireItem.getItem().size();
    for (var element : getElementsWithNonNullElementType(profile)) {
      childCount++;
      processElement(profile, element, childCount);
    }
  }

  protected void processElement(
      StructureDefinition profile,
      ElementDefinition element,
      int childCount
  ) {
    final String childLinkId = String.format(CHILD_LINK_ID_FORMAT, questionnaireItem.getLinkId(), childCount);
    try {
      final QuestionnaireItemComponent nestedQuestionnaireItem = nestedQuestionnaireItemService.getNestedQuestionnaireItem(
          profile,
          element,
          childLinkId
      );
      questionnaireItem.addItem(nestedQuestionnaireItem);
      paths.add(element.getPath());
    } catch (QuestionnaireParsingException ex) {
      logger.warn(ex.getMessage());
      questionnaireItem.addItem(createErrorItem(childLinkId, ex.getMessage()));
    } catch (Exception ex) {
      final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
      logger.error(message);
      questionnaireItem.addItem(createErrorItem(childLinkId, message));
    }
  }

  protected List<ElementDefinition> getElementsWithNonNullElementType(StructureDefinition profile) {
    final List<ElementDefinition> elements = profile.getDifferential().getElement();
    return elements.stream()
        .filter(element -> getElementType(element) != null)
        .collect(Collectors.toList());
  }

  protected Questionnaire.QuestionnaireItemComponent createErrorItem(String linkId, String errorMessage) {
    return new QuestionnaireItemComponent().setLinkId(linkId).setType(QuestionnaireItemType.DISPLAY).setText(errorMessage);
  }

  public String getElementType(ElementDefinition element) {
    return element.hasType() ? element.getType().get(0).getCode() : null;
    // StructureDefinition profile) {
    // if (elementType == null) {
    // var snapshot = profile.getSnapshot().getElement().stream().filter(e ->
    // e.getId().equals(element.getId())).collect(Collectors.toList());
    // elementType = snapshot == null || snapshot.size() == 0 ? null : snapshot.get(0).hasType()
    // ? snapshot.get(0).getType().get(0).getCode() : null;
    // }
    // return elementType;
  }
}
