package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.questionnaireitem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem.NestedQuestionnaireItemService;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.SearchHelper.searchRepositoryByCanonical;

public class QuestionnaireItemGenerator {
  protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
  protected static final String ITEM_CREATION_ERROR = "An error occurred during item creation: %s";
  protected static final String CHILD_LINK_ID_FORMAT = "%s.%s";
  protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireItemGenerator.class);
  protected Repository repository;
  protected QuestionnaireItemService questionnaireItemService;
  protected NestedQuestionnaireItemService nestedQuestionnaireItemService;
  // ROSIE TODO: get rid of paths and remove references to it
  protected List<String> paths = new ArrayList<>();
  protected QuestionnaireItemComponent questionnaireItem;

  public static QuestionnaireItemGenerator of(
      Repository repository,
      String patientId,
      IBaseParameters parameters,
      IBaseBundle bundle,
      LibraryEngine libraryEngine
  ) {
    QuestionnaireItemService questionnaireItemService = new QuestionnaireItemService();
    NestedQuestionnaireItemService nestedQuestionnaireItemService = NestedQuestionnaireItemService.of(
        repository,
        patientId,
        parameters,
        bundle,
        libraryEngine
    );
    return new QuestionnaireItemGenerator(repository, questionnaireItemService, nestedQuestionnaireItemService);
  }

  QuestionnaireItemGenerator(
      Repository theRepository,
      QuestionnaireItemService theQuestionnaireItemService,
      NestedQuestionnaireItemService theNestedQuestionnaireItemService
  ) {
      repository = theRepository;
      questionnaireItemService = theQuestionnaireItemService;
      nestedQuestionnaireItemService = theNestedQuestionnaireItemService;
  }

  public Questionnaire.QuestionnaireItemComponent generateItem(
      DataRequirement actionInput,
      int itemCount
  ) {
    if (!actionInput.hasProfile()) {
      throw new IllegalArgumentException(NO_PROFILE_ERROR);
    }
    final String linkId = String.valueOf(itemCount + 1);
    try {
      final StructureDefinition profile = getProfileDefinition(actionInput);
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
    } catch (Exception ex) {
      final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
      logger.warn(message);
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

  protected String getElementType(ElementDefinition element) {
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

  protected StructureDefinition getProfileDefinition(DataRequirement actionInput) {
    final CanonicalType type = actionInput.getProfile().get(0);
    final Resource profile = searchRepositoryByCanonical(repository, type);
    return (StructureDefinition) profile;
  }
}
